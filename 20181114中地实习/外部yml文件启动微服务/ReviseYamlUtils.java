package com.mapgis.agent.utils;

import com.mapgis.AgentApplication;
import com.mapgis.agent.entity.YamlConfig;
import com.mapgis.core.common.util.NetUtil;
import com.microsoft.sqlserver.jdbc.SQLServerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReviseYamlUtils {

    private static void loadSqlServer(String url, YamlConfig config) throws Exception {
        String input = NetUtil.executeHttpGet(url);

        // 表达式对象
        Pattern p = Pattern.compile("server=(.*?);database=\'(.*?)\';User id=(.*?);password=(.*?);Integrated Security=false");

        // 创建 Matcher 对象
        Matcher m = p.matcher(input);

        // 是否找到匹配
        boolean found = m.find();

        if (found) {
            config.setSqlServerHost(m.group(1));
            config.setSqlServerDB(m.group(2));
            config.setSqlServerUser(m.group(3));
            config.setSqlServerPassword(m.group(4));
        }
    }

    private static void loadMongoDB(YamlConfig config) {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource(new SQLServerDriver(), config.getSqlServerURL(), config.getSqlServerUser(), config.getSqlServerPassword());

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        String sql = "SELECT NODEVALUE FROM SYSDATADICTIONARY WHERE PARENTID =(SELECT NODEID FROM SYSDATADICTIONARY WHERE NODENAME='通用配置') AND NODENAME='桥接端口'";

        String bridgePort = jdbcTemplate.queryForObject(sql, String.class);

        //System.out.println(bridgePort);
        config.setBridgePort(Integer.parseInt(bridgePort));

        sql = "SELECT NODEVALUE FROM SYSDATADICTIONARY WHERE PARENTID =(SELECT NODEID FROM SYSDATADICTIONARY WHERE NODENAME='通用配置') AND NODENAME='文件服务器地址'";

        String mongoURL = jdbcTemplate.queryForObject(sql, String.class);

        config.setMongoDBHost(mongoURL.split("//")[1].split("/")[0].split(":")[0]);

        String port = mongoURL.split("//")[1].split("/")[0].split(":")[1];

        config.setMongoDBPort(Integer.valueOf(port));

        config.setMongoDBDatabase(mongoURL.split("//")[1].split("/")[1].split("\\?")[0]);
    }

    public static YamlConfig loadYamlConfig(String iisIP, int iisPort) throws Exception {
        String url = "http://" + iisIP + ":" + iisPort + "/CityInterface/rest/services/Bridge.svc/GetBizDBInfo";

        YamlConfig config = new YamlConfig();

        loadSqlServer(url, config);
        loadMongoDB(config);

        return config;
    }

    private static Logger logger = LoggerFactory.getLogger(AgentApplication.class);

    public static Map setYaml(String path, YamlConfig config) {
        Map properties = YamlHelper.loadYaml(path);

        if (properties == null) {
            logger.error("加载自定义配置文件失败， " + path);

            return null;
        } else {
            logger.info("加载自定义配置文件成功， " + path);
        }

        Map datasource = (Map) ((Map) properties.get("spring")).get("datasource");

        if (datasource != null) {
            datasource.put("url", config.getSqlServerURL());
            datasource.put("username", config.getSqlServerUser());
            datasource.put("password", config.getSqlServerPassword());
        }

        if ((((Map) properties.get("spring")).get("data")) != null) {
            Map mongodb = (Map) ((Map) ((Map) properties.get("spring")).get("data")).get("mongodb");

            mongodb.put("database", config.getMongoDBDatabase());
            mongodb.put("host", config.getMongoDBHost());
            mongodb.put("port", config.getMongoDBPort());
        }

        return properties;
    }
}
