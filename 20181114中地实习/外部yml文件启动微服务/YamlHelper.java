package com.mapgis.agent.utils;

import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class YamlHelper {
    public static Map loadYaml(String path) {
        try {
            //初始化Yaml解析器
            Yaml yaml = new Yaml();

            //读入文件
            Object result = yaml.load(new FileInputStream(path));

            System.out.println(result.getClass());
            System.out.println(result);

            if (result instanceof Map) {
                return (Map) result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void dumpYaml(Map yamlMap, String outPath) {
        try {
            //初始化Yaml解析器
            Yaml yaml = new Yaml();

            String result = yaml.dumpAsMap(yamlMap);

            System.out.println(result);

            FileUtils.writeStringToFile(new File(outPath), result, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
