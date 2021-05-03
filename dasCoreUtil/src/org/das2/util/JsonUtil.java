
package org.das2.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Useful JSON utilities.
 * @author jbf
 */
public class JsonUtil {
    
    /**
     * convert JSONObject to Map&lt;String,Object&gt;
     * @param jsonobj
     * @return List
     * @throws JSONException 
     */
    public static Map<String, Object> jsonToMap(JSONObject jsonobj)  throws JSONException {
        Map<String, Object> map = new HashMap<>();
        Iterator<String> keys = jsonobj.keys();
        while(keys.hasNext()) {
            String key = keys.next();
            Object value = jsonobj.get(key);
            if (value instanceof JSONArray) {
                value = jsonToList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = jsonToMap((JSONObject) value);
            }   
            map.put(key, value);
        }   
        return map;
    }

    /**
     * convert JSONArray to List&lt;Object&gt;
     * @param array
     * @return List
     * @throws JSONException 
     */
    public static List<Object> jsonToList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONArray) {
                value = jsonToList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = jsonToMap((JSONObject) value);
            }
            list.add(value);
        }   
        return list;
    }    
}
