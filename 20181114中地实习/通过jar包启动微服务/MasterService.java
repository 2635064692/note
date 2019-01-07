package com.mapgis.agent.service;

import com.mapgis.agent.entity.MasterEntity;
import com.mapgis.agent.entity.SlaveEntity;
import com.mapgis.agent.entity.YamlConfig;
import com.mapgis.agent.mapper.MasterMapper;
import com.mapgis.agent.mapper.SlaveMapper;
import com.mapgis.agent.utils.ReviseYamlUtils;
import com.mapgis.agent.utils.Utils;
import com.mapgis.agent.utils.YamlHelper;
import com.mapgis.core.common.util.BaseClassUtil;
import com.mapgis.core.entity.ResultData;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;

@Service
public class MasterService {
    private final static Logger logger = LoggerFactory.getLogger(MasterService.class);

    @Resource
    private MasterMapper masterMapper;

    @Resource
    private SlaveMapper slaveMapper;

    @Resource
    private SlaveService slaveService;

    @Resource
    private PortService portService;

    @Resource
    private Hashtable<String, ExecuteWatchdog> watchdogs;

    public ResultData<MasterEntity> newServer(String name, String path, int port, String iisName, String iisIP, int iisPort) {
        ResultData<MasterEntity> data = new ResultData<>();

        try {
            if (!(path.endsWith("/") || path.endsWith("\\")))
                path += "\\";

            MasterEntity bean = masterMapper.selectByNameOrPathOrPort(name, path, port);

            if (bean != null) {
                return data.failed("抱歉，此站点已经被占用: " + bean.toString(), bean);
            }

            data = launchServer(name, path, port, iisName, iisIP, iisPort);

            return data.success("新建站点成功");
        } catch (Exception ex) {
            logger.warn("新建站点失败", ex);

            return data.failed(ex.getMessage());
        }
    }

    private ResultData<MasterEntity> launchServer(String name, String path, int port, String iisName, String iisIP, int iisPort) throws Exception {
        ResultData<MasterEntity> data = new ResultData<>();

        if (!portService.isUnusedPort(port, 1))
            throw new Exception("抱歉，指定的网关端口已经被占用");

        int zuulPort = port;

        if (!(path.endsWith("/") || path.endsWith("\\")))
            path += "\\";

        Collection<File> files = FileUtils.listFiles(new File(path), new String[]{"jar"}, false);

        if (files.size() < 4)
            return data.failed("搜索到的微服务插件数目少于4个");

        String[] nobles = new String[]{"mapgis-eureka-server", "mapgis-zipkin-server", "mapgis-gateway"};

        String[] modules = new String[files.size()];
        int k = 0;

        for (File file : files) {
            String fileName = file.getName();

            fileName = fileName.substring(0, fileName.lastIndexOf('.'));

            boolean isHit = false;

            for (int i = 0; i < nobles.length; i++) {
                if (fileName.startsWith(nobles[i])) {
                    modules[k] = modules[i];
                    modules[i] = fileName;

                    isHit = true;

                    break;
                }
            }

            if (!isHit)
                modules[k] = fileName;

            k++;
        }

        int eurekaServerPort = 0;
        int zipkinServerPort = 0;

        YamlConfig config = ReviseYamlUtils.loadYamlConfig(iisIP, iisPort);

        for (String module : modules) {
            int serverPort;
            String part = "api";
            String ymlPath = path + "config\\" + module.substring(0, module.indexOf("0") - 1) + ".yml";

            Map yamlMap = ReviseYamlUtils.setYaml(ymlPath, config);

            if (yamlMap == null)
                continue;

            if (!module.startsWith("mapgis-gateway")) {//非网关，自动寻找可用端口
                port = Utils.getAvailablePort(++port);

                if (module.startsWith("mapgis-eureka-server")) {
                    eurekaServerPort = port;
                    ((Map) yamlMap.get("server")).put("port", eurekaServerPort);
                    part = "eureka";
                } else if (module.startsWith("mapgis-zipkin-server")) {
                    zipkinServerPort = port;
                    ((Map) yamlMap.get("server")).put("port", zipkinServerPort);
                    part = "zipkin";
                }

                serverPort = port;
            } else {//网关，固定使用用户指定的端口
                serverPort = zuulPort;
                List routes = ((List) ((Map) (Map) ((Map) ((Map) yamlMap.get("spring")).get("cloud")).get("gateway")).get("routes"));

                for (int i = 0; i < routes.size(); i++) {
                    if (!((Map) routes.get(i)).get("uri").toString().contains("lb://")) {
                        ((Map) routes.get(i)).put("uri", "http://" + iisIP + ":" + iisPort);
                    }
                }

                part = "gateway";
            }

            ((Map) yamlMap.get("server")).put("port", serverPort);

            if (!(module.contains("mapgis-eureka-server"))) {
                if (!(module.contains("mapgis-zipkin-server") || module.contains("mapgis-gateway"))) {
                    ((Map) ((Map) yamlMap.get("spring")).get("zipkin")).put("base-url", "http://localhost:" + zipkinServerPort);
                }

                ((Map) ((Map) ((Map) yamlMap.get("eureka")).get("client")).get("serviceUrl")).put("defaultZone", "http://localhost:" + eurekaServerPort + "/eureka/");
            }

            YamlHelper.dumpYaml(yamlMap, ymlPath);

            String guid = UUID.randomUUID().toString();

            //注意：第一个空格之后的所有参数都为参数
            CommandLine cmdLine = new CommandLine("java");

            cmdLine.addArgument("-Xms128m");
            cmdLine.addArgument("-Xmx256m");
            cmdLine.addArgument("-jar");

            cmdLine.addArgument(path + module + ".jar");

            //cmdLine.addArgument(">" + path + "logs\\" + module + ".log");

            DefaultExecutor exec = new DefaultExecutor();

            ExecuteWatchdog watchdog = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);
            DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

            exec.setWatchdog(watchdog);
            exec.setWorkingDirectory(new File(path));

            exec.execute(cmdLine, resultHandler);

            watchdogs.put(guid, watchdog);

//            resultHandler.waitFor(10_000);

            System.out.println(module + " \n http://localhost:" + port);

            slaveMapper.deleteBySiteAndName(name, module);

            SlaveEntity model = new SlaveEntity(name, module, path + module + ".jar", serverPort, part, 0, guid,
                    cmdLine.toString(), "运行中", BaseClassUtil.getSystemTime(), "", " ", "", "");

            boolean isSuccess = slaveMapper.insert(model) > 0;

            if (isSuccess) {
                portService.addOne(serverPort);

                logger.info("sqlite记录插入civ_ms_service成功");
            } else
                logger.error("sqlite记录插入civ_ms_service失败");
        }

        System.out.println("打完收工,站点名称：" + name + "，站点路径：" + path + "，网关端口：" + zuulPort);

        masterMapper.deleteByName(name);

        MasterEntity bean = new MasterEntity(name, path, zuulPort, BaseClassUtil.getSystemTime(), "started", "");
        bean.setIisIP(iisIP);
        bean.setIisName(iisName);
        bean.setIisPort(iisPort);
        bean.setState("运行中");
        bean.setBridgePort(config.getBridgePort());

        boolean isSuccess = masterMapper.insert(bean) > 0;

        if (isSuccess)
            logger.info("sqlite记录插入civ_ms_site成功");
        else
            logger.error("sqlite记录插入civ_ms_site失败");

        return data.success("启动服务集群成功", bean);
    }

    public ResultData<MasterEntity> destroyServer(String name) {
        ResultData<MasterEntity> data = new ResultData<>();

        MasterEntity bean = masterMapper.selectByName(name);

        if (bean == null) {
            return data.failed("没有找到目标站点");
        }

        String siteName = bean.getName();

        stopServer(siteName);

        List<SlaveEntity> slaveEntities = slaveMapper.selectAllBySiteName(siteName);

        for (SlaveEntity entity : slaveEntities) {
            portService.deleteOne(entity.getPort());
        }

        slaveMapper.deleteBySiteName(siteName);
        masterMapper.deleteByName(siteName);

        return data.success("销毁站点成功", bean);
    }

    public ResultData<SlaveEntity> stopServer(String name) {
        ResultData<SlaveEntity> data = new ResultData<>();

        List<SlaveEntity> beans = slaveMapper.selectAllBySiteName(name);

        if (beans == null || beans.size() <= 0)
            return data.failed("没有找到下属的微服务集群");

        for (SlaveEntity bean : beans) {
            slaveService.stopSingleService(bean.getGuid());
        }

        System.out.println("强制退出所有JAVA进程，退出程序：" + name);

        data.success("停止服务集群成功，杀死所有下属微服务");

        data.setDataList(beans);

        return data;
    }

    public ResultData<SlaveEntity> startServer(String name) throws Exception {
        ResultData<SlaveEntity> data = new ResultData<>();

        List<SlaveEntity> beans = slaveMapper.selectAllBySiteName(name);

        if (beans == null || beans.size() <= 0)
            return data.failed("没有找到下属的微服务集群");

        for (SlaveEntity bean : beans) {
            slaveService.restartSingleService(bean.getGuid());

            logger.info("启动单个微服务成功:" + name);
        }

        data.success("启动服务集群成功，重启了所有下属微服务");

        data.setDataList(beans);

        return data;
    }

    public ResultData<SlaveEntity> serverStatus(String name) {
        ResultData<SlaveEntity> data = new ResultData<>();

        List<SlaveEntity> beans = slaveMapper.selectAllBySiteName(name);

        if (beans == null || beans.size() <= 0)
            return data.failed("没有找到下属的微服务集群");

        data.success("获取服务集群健康状态成功");

        data.setDataList(beans);

        return data;
    }

    public ResultData<MasterEntity> getExistSite() {
        // @ResponseBody 如果返回的是对象 会自动转为json字符串，如果返回的是String 则返回该字符串
        ResultData<MasterEntity> data = new ResultData<>();

        List<MasterEntity> beans = masterMapper.selectAll();

        if (beans != null)
            data.setDataList(beans);

        return data.success("获取已有站点列表成功");
    }
}
