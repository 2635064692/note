package com.mapgis.agent.service;

import com.mapgis.agent.entity.MasterEntity;
import com.mapgis.agent.entity.SlaveEntity;
import com.mapgis.agent.mapper.MasterMapper;
import com.mapgis.agent.mapper.SlaveMapper;
import com.mapgis.agent.utils.CpuUtils;
import com.mapgis.agent.utils.MemoryUtils;
import com.mapgis.core.common.util.NetUtil;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Component
public class CpuAndMemoryHelper {

    @Autowired
    SlaveMapper dao;

    @Autowired
    PortService portService;

    @Autowired
    MasterMapper masterMapper;

    private String siteName = null;
    private Logger logger = LoggerFactory.getLogger(CpuAndMemoryHelper.class);

    @Scheduled(fixedDelay = 20000)
    public List<SlaveEntity> getCpuAndMemory(){
        List<SlaveEntity> models = dao.selectAll();
        int count = 0;
        for (SlaveEntity model : models) {
            if (MemoryUtils.getMemory(MemoryUtils.getPid(model.getPort())) != null) {
                model.setPid(MemoryUtils.getPid(model.getPort()));
                model.setState("运行中");
                model.setMemory(MemoryUtils.getMemory(model.getPid()));
                model.setCpu(CpuUtils.getProcessUsage(model.getPid()));
                System.out.println(model.getCpu() + "   " + model.getMemory() + "   " + model.getName());
                portService.addOne(model.getPort());
            }  else {
                model.setCpu("00");
                model.setMemory("00");
                model.setPid(-1);
                System.out.println(model.getPort() + "---未运行");
                portService.deleteOne(model.getPort());
            }

            model.setState(dao.selectByName(model.getName()).getState());
            dao.updateState(model);

            siteName = model.getSite();
            if (model.getState().equals("运行中"))
                count++;
            //logger.error("20秒一次的正在运行");
        }

        MasterEntity masterEntity = masterMapper.selectByName(siteName);
        if (masterEntity!=null){
            if (count>0&&count<5)
                masterEntity.setState("部分运行");
            else if(count==0)
                masterEntity.setState("已停止");
            else
                masterEntity.setState("运行中");
            masterMapper.updateState(masterEntity);
        }

        String url = "http://" + masterEntity.getIisIP() + ":" + masterEntity.getIisPort()+"/CityInterface/rest/services/Bridge.svc/HealthCheck?port="+masterEntity.getBridgePort();


        try {
            System.out.println(NetUtil.executeHttpGet(url));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return models;
    }

    /**
     * initialDelay 初次执行任务之前等待10000ms
     * fixedDelay 每次执行任务之后间隔5000ms再次执行该任务。
     */
    @Scheduled( fixedDelay = 5000)
    public void checkProcess() throws Exception {
        List<SlaveEntity> models = dao.selectAll();
        for (SlaveEntity model : models) {
            if (model.getState().equals("运行中")) {
                if (MemoryUtils.getMemory(MemoryUtils.getPid(model.getPort())) == null) {
                    CommandLine cmdLine = new CommandLine("java");

                    cmdLine.addArgument("-Xms128m");
                    cmdLine.addArgument("-Xmx256m");
                    cmdLine.addArgument("-jar");
                    cmdLine.addArgument(model.getPath());

                    DefaultExecutor exec = new DefaultExecutor();

                    ExecuteWatchdog watchdog = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);
                    DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

                    exec.setWatchdog(watchdog);
                    exec.setWorkingDirectory(new File(model.getPath().substring(0, model.getPath().lastIndexOf("\\"))));
                    exec.execute(cmdLine, resultHandler);

                    resultHandler.waitFor(10_000);
                }
            }
            // logger.error("5秒一次的正在运行");
            dao.updateState(model);
        }
    }
}

