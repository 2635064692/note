package jerry.jsonbeans;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jerry.jsonbeans.models.ClassDescribe;
import jerry.jsonbeans.models.ElementalDescribe;
import jerry.jsonbeans.utils.FileUtils;
import jerry.jsonbeans.utils.JsonUtils;
import jerry.jsonbeans.utils.StringUtils;

import java.io.*;
import java.util.*;

/**
 * @author Jerry Ou
 * @version 1.0 2016-05-03 13:55
 * @since JDK 1.6
 */
public class JsonBeans {

    private ClassDescribe classDescribe;
    private List<String> serviceList;
    private static String serviceName;

    public static Map<String, String> primitiveWrappers = new HashMap<String, String>() {
        private static final long serialVersionUID = 8015328537607906480L;

        {
            put("int", "");
            put("char", "");
            put("string", "");
            put("double", "");
            put("boolean", "");
            put("float", "");
            put("long", "");
            put("byte", "");
            put("short", "");

            put("list", "java.util.*;");
            put("set", "java.util.*;");
            put("map", "java.util.*;");
            put("timestamp", "java.sql.Timestamp;");
        }
    };

    public static void main(String[] args) {
        JsonBeans jsonBeans = new JsonBeans();
        String[] strings = FileUtils.readFile(FileUtils.filePath("jsonbeans.txt"));

        ClassDescribe classInfo = new ClassDescribe(strings[0], strings[1], "service");

        Object data = JSONObject.parseObject(strings[2]).get("data");
        if (data instanceof JSONArray) {
            List<Map<String, Object>> maps = JsonUtils.formatJsonArray((JSONArray) data);
            for (Map map : maps) {
                jsonBeans.json2bean(map, classInfo);
            }
        } else {
            Map<String, Object> map = JsonUtils.formatJsonObject(data.toString());
            jsonBeans.json2bean(map, classInfo);
        }
        TransToJar transToJar = new TransToJar("output", "classes", serviceName + ".jar", "lib");
        transToJar.compile();
        System.out.println(new File("lib").getAbsolutePath());
    }

    public void json2bean(Map jsonMap, ClassDescribe classInfo) {
        serviceList = new ArrayList<>();
        this.classDescribe = classInfo;
        this.serviceName = classInfo.className;
        String methodName = jsonMap.get("operation").toString();

        serviceList.add(StringUtils.firstCharToUpperCase(methodName));
        for (Object o : jsonMap.keySet()) {
            //request或者response
            if (jsonMap.get(o) instanceof JSONObject) {
                serviceList.add(StringUtils.firstCharToUpperCase(o.toString()));
                JSONObject jsonObject = (JSONObject) jsonMap.get(o);
                Map<String, ElementalDescribe> map = JsonUtils.jsonToMap(jsonObject.toJSONString());
                jsonToClass(map, StringUtils.firstCharToUpperCase(o.toString()), "entity");
            }
        }
        jsonToClass(null, serviceName, "service");

    }

    public void jsonToClass(Map<String, ElementalDescribe> jsonMap, String className, String type) {
        //主体内容流
        StringBuilder sb = new StringBuilder();
        //包名流
        StringBuilder packagePart = new StringBuilder();
        //导包流
        StringBuilder importPart = new StringBuilder();
        //类起始流
        StringBuilder classBeginPart = new StringBuilder();
        //类终止流
        StringBuilder classEndPart = new StringBuilder();
        //类内容流
        StringBuilder classContentPart = new StringBuilder();
        //import set
        HashSet<String> hashSet = new HashSet<>();
        packagePart.append("package " + classDescribe.packageName + ";");
        sb.append(packagePart).append(Const._R_N);

        classEndPart.append(Const._R_N).append("}");

        if (type.equals("service")) {
            classBeginPart.append(Const._R_N).append("//Json2Bean 自动生成")
                    .append(Const._R_N).append("@Service")
                    .append(Const._R_N).append("public interface ").append(className).append(" {").append(Const._R_N);
            hashSet.add("org.springframework.stereotype.Service;");
            classContentPart.append(Const._R_N).append(Const._SPACE).append(serviceList.get(2) + " " + serviceList.get(0) + " (" + serviceList.get(1) + " arg1);");
        } else {
            classBeginPart.append(Const._R_N).append("//Json2Bean 自动生成")
                    .append(Const._R_N).append("@Data")
                    .append(Const._R_N).append("public class ").append(className).append(" implements Serializable {")
                    .append(Const._R_N).append(Const._SPACE).append("private static final long serialVersionUID = 1L;").append(Const._R_N);
            hashSet.add("io.swagger.annotations.ApiModelProperty;");
            hashSet.add("java.io.Serializable;");
            hashSet.add("javax.validation.constraints.NotNull;");
            hashSet.add("lombok.Data;");


            for (String key : jsonMap.keySet()) {
                ElementalDescribe describe = jsonMap.get(key);
                classContentPart.append(Const._R_N).append(Const._SPACE).append("@ApiModelProperty(value = " + "\"" + describe.note + "\"" + ")");
                if (primitiveWrappers.containsKey(describe._type))
                    hashSet.add(primitiveWrappers.get(describe._type));
                if (describe.optional)
                    classContentPart.append(Const._R_N).append(Const._SPACE).append("@NotNull");
                if (describe.inner != null) {
                    if (describe._type.equals("list") || describe._type.equals("set") || describe._type.equals("map")) {
                        classContentPart.append(Const._R_N).append(Const._SPACE).append("private " + StringUtils.firstCharToUpperCase(describe._type) + "<" + StringUtils.firstCharToUpperCase(key) + "> " + key + ";");

                    } else
                        classContentPart.append(Const._R_N).append(Const._SPACE).append("private " + StringUtils.firstCharToUpperCase(describe._type) + " " + key + ";");
                    jsonToClass((Map<String, ElementalDescribe>) describe.inner, StringUtils.firstCharToUpperCase(key), "entity");
                } else if (describe.defaultValue != null) {
                    if (describe.defaultValue instanceof String)
                        classContentPart.append(Const._R_N).append(Const._SPACE).append("private " + StringUtils.firstCharToUpperCase(describe._type) + " " + key + " = " + "\"" + describe.defaultValue + "\"" + ";");
                    else
                        classContentPart.append(Const._R_N).append(Const._SPACE).append("private " + StringUtils.firstCharToUpperCase(describe._type) + " " + key + " = " + describe.defaultValue + ";");
                } else
                    classContentPart.append(Const._R_N).append(Const._SPACE).append("private " + StringUtils.firstCharToUpperCase(describe._type) + " " + key + ";");
            }
        }

        for (String s : hashSet) {
            if (s != "")
                importPart.append(Const._R_N).append("import ").append(s);
        }

        sb.append(Const._R_N).append(importPart).append(Const._R_N).append(classBeginPart).append(classContentPart).append(Const._R_N).append(classEndPart);
        classDescribe.className = className;
        genFile(sb.toString(), classDescribe);

    }

    private void genFile(String s, ClassDescribe describe) {
        File file = new File("output/" + describe.packageName.replaceAll("[.]", "/"), describe.className + ".java");
        System.out.println(file.getAbsolutePath());
        BufferedReader bf = null;
        StringBuilder sb = new StringBuilder();
        if (file.getAbsolutePath().contains(serviceName) && file.length() > 0) {
            try {
                bf = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                String line;
                while ((line = bf.readLine()) != null) {
                    sb.append(line).append(Const._R_N);
                    if (line.contains("{")) {
                        sb.append(Const._R_N).append(Const._SPACE).append(serviceList.get(2) + " " + serviceList.get(0) + " (" + serviceList.get(1) + " arg1);");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            FileUtils.write(sb.toString(), file);
        } else
            FileUtils.write(s, file);
    }
}
