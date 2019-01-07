package com.mapgis.core;

import com.mapgis.core.common.util.BaseClassUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.util.Objects;
import java.util.Properties;

public class CoreInitConfig {
    private static final Logger logger = LoggerFactory.getLogger(CoreInitConfig.class);

    public static PropertySourcesPlaceholderConfigurer properties(String artifactId) {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();

        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();

        yaml.setResources(new ClassPathResource("application.yml"));//class引入

        Properties properties = Objects.requireNonNull(yaml.getObject());

        if (BaseClassUtil.isNullOrEmptyString(artifactId)) {
            artifactId = properties.getProperty("spring.application.name");

            if (!artifactId.startsWith("mapgis-"))
                artifactId = "mapgis-" + artifactId;
        }

        File configFile = new File("config/" + artifactId + ".yml");

        if (configFile.exists()) {
            logger.info("yaml config file exist, path: " + configFile.getAbsolutePath());

            yaml.setResources(new FileSystemResource("config/" + artifactId + ".yml"));//File引入

            Properties outProperties = Objects.requireNonNull(yaml.getObject());

            configurer.setPropertiesArray(properties, outProperties);
        } else {
            logger.error("yaml config file not exist, path: " + configFile.getAbsolutePath());

            configurer.setProperties(properties);
        }

        return configurer;
    }
}
