package com.eseasky.business.appservice;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.*;

/**
 * @author zhanghai by 2019/8/5
 */
public class SensitiveHandler {
    //将路径和其对应的值存入map中
    private Map<String, Object> hashmap = new LinkedHashMap();

    //传入的json
    private String json = null;

    //粗解析json成List<Map>类型
    private List<Map<String, Object>> pathList = new ArrayList<>();

    //构造函数 初始化json及pathList
    public SensitiveHandler(String json) {
        this.json = json;
        parseString2Object(json);
        if (pathList.size() == 1)
            changeToMap("root", pathList.get(0));
        else
            changeToMap("root", pathList);
    }

    /**
     * @param str 单个路径
     * @return 对应的值
     */
    public Map<String, Object> get(String str) {
        Map<String, Object> resultMap = new LinkedHashMap();
        for (Object obj : hashmap.keySet()) {
            if (anotherEquals(str, (String) obj))
                resultMap.put((String) obj, hashmap.get(obj));
        }
        return resultMap;
    }

    /**
     * @param str 多个路径
     * @return 对应的多个键和值
     */
    public List<Object> get(String[] str) {
        List<Object> list = new ArrayList<>();
        for (String s : str) {
            list.add(get(s));
        }
        return list;
    }

    /**
     * @param str 多个路径
     * @return 对应的多个键和值
     */
    public List<Object> get(List<String> str) {
        List<Object> list = new ArrayList<>();
        for (String s : str) {
            list.add(get(s));
        }
        return list;
    }

    /**
     * 遍历pathList 递归获取相应键值对 并依次存入hashMap中
     * 当遍历的obj不为集合时即可存入map中
     *
     * @param totalPathName 键名
     * @param obj           需要遍历的对象
     */
    private void changeToMap(String totalPathName, Object obj) {
        Object oldKey = null;
        int oldNum = -1;
        if (obj instanceof List) {
            List list = (List) obj;

            for (int i = 0; i < list.size(); i++) {
                if (oldNum != -1)
                    totalPathName = totalPathName.replace(String.valueOf(oldNum), String.valueOf(i));
                else
                    totalPathName += "[" + i + "]";
                changeToMap(totalPathName, list.get(i));
                oldNum = i;
            }
        } else if (obj instanceof Map) {
            Map map = (Map) obj;

            for (Object k : map.keySet()) {
                if (oldKey != null)
                    totalPathName = totalPathName.replace((String) oldKey, (String) k);
                else
                    totalPathName = totalPathName + "." + (String) k;
                changeToMap(totalPathName, map.get(k));
                oldKey = k;
            }
        } else {
            hashmap.put(totalPathName, obj);
        }
    }

    /**
     * 自定义比较规则 当查询带* 路径时，判断是否与map中的值相同
     *
     * @param str1 传入的path路径（带*的路径）
     * @param str2
     * @return true or false
     */
    private boolean anotherEquals(String str1, String str2) {
        String[] split1 = str1.split("\\.");
        String[] split2 = str2.split("\\.");

        boolean flag = false;
        if (split1.length == split2.length) {
            for (int i = 0; i < split1.length; i++) {
                if (split1[i].contains("*"))
                    flag = true;
                else if (split1[i].equals(split2[i]))
                    flag = true;
                else
                    flag = false;
                if (!flag)
                    return false;
            }
        }
        return flag;
    }

    /**
     * 粗解析json成List<Map>类型 并存入pathList中
     *
     * @param content 传入的json字符串
     */
    private void parseString2Object(String content) {
        String currentString = content.trim();
        if (currentString.startsWith("[") && currentString.endsWith("]")) {
            //jsarray
            pathList.addAll(formatJsonArray(JSON.parseArray(currentString)));
        } else if (currentString.startsWith("{") && currentString.endsWith("}")) {
            //jsonmap
            pathList.add(formatJsonObject(currentString));
        } else if (currentString.indexOf("=") > 0) {
            //querystring
            pathList.add(formatJsonObject(parseQUeryString(content).toJSONString()));
        }
    }

    /**
     * 格式化JsonArray形式的json数据
     * @param jsonArray
     * @return
     */
    private List<Map<String, Object>> formatJsonArray(JSONArray jsonArray) {

        ArrayList<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            list.add(formatJsonObject(jsonObject.toJSONString()));
        }
        return list;
    }

    /**
     * 格式化JsonObject形式的json数据
     * @param jsonStr
     * @return
     */
    public Map<String, Object> formatJsonObject(String jsonStr) {
        Map<String, Object> map = new HashMap<String, Object>();
        JSONObject json = JSON.parseObject(jsonStr);
        for (Object k : json.keySet()) {
            Object v = json.get(k);
            if (v instanceof JSONArray) {
                List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
                Iterator it = ((JSONArray) v).iterator();
                while (it.hasNext()) {
                    Object json2 = it.next();
                    list.add(formatJsonObject(json2.toString()));
                }
                map.put(k.toString(), list);
            } else {
                map.put(k.toString(), v);
            }
        }
        return map;
    }

    /**
     * 格式化 a=1&b=2&c=3 形式的数据
     * @param content
     * @return
     */
    private JSONObject parseQUeryString(String content) {
        if (content.indexOf("?") >= 0) {
            content = content.substring(content.indexOf("?"));
        }
        String[] sections = content.split("&");
        JSONObject parsedContent = new JSONObject();
        String[] kv = null;
        for (String section : sections) {
            kv = section.split("=");
            if (kv.length == 2) {
                parsedContent.put(kv[0], kv[1]);
            } else {
                return null;
            }
        }
        return parsedContent;
    }
}