package jerry.jsonbeans.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jerry.jsonbeans.models.ElementalDescribe;

import java.util.*;

/**
 * @author zhanghai by 2019/8/20
 */
public class JsonUtils {

    public static List<Map<String, Object>> formatJsonArray(JSONArray jsonArray) {

        ArrayList<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            list.add(formatJsonObject(jsonObject.toJSONString()));
        }
        return list;
    }

    public static Map<String, Object> formatJsonObject(String jsonStr) {
        Map<String, Object> map = new HashMap<>();
        JSONObject json = JSON.parseObject(jsonStr);
        for (Object k : json.keySet()) {
            Object v = json.get(k);
            if (v instanceof JSONArray) {
                List<Map<String, Object>> list = new ArrayList<>();
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

    public static Map<String, ElementalDescribe> jsonToMap(String str) {
        Map<String, Object> map = formatJsonObject(str);
        Map<String, ElementalDescribe> elementMap = new HashMap<>();
        for (String s : map.keySet()) {
            ElementalDescribe elementalDescribe = JSONObject.parseObject(map.get(s).toString(), ElementalDescribe.class);
            if (elementalDescribe.inner != null) {
                elementalDescribe.inner = jsonToMap(elementalDescribe.inner.toString());
            }
            elementMap.put(s, elementalDescribe);
        }
        return elementMap;
    }

}
