package com.fenglinga.tinyspring.mysql;

import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fenglinga.tinyspring.common.Utils;

public class BaseObject {
    /** 判断对象是否为空 */
    public static boolean empty(Object obj) {
        if (obj == null) return true;
        if (obj instanceof Boolean) {
            if ((Boolean)obj == false) {
                return true;
            }
        }
        if (obj instanceof String) {
            if (((String)obj).length() == 0) {
                return true;
            }
        }
        if (obj instanceof JSONObject) {
            if (((JSONObject)obj).size() == 0) {
                return true;
            }
        }
        if (obj instanceof JSONArray) {
            if (((JSONArray)obj).size() == 0) {
                return true;
            }
        }
        return false;
    }
    
    /** 判断是否为空 */
    public static boolean is_null(Object obj) {
        return obj == null;
    }
    
    /** 判断是否是纯数字 */
    public static boolean is_numeric(String str) {
        return StringUtils.isNumeric(str);
    }
    
    /** 判断是否是字符串 */
    public static boolean is_string(Object obj) {
        return obj instanceof String;
    }
    
    /** 判断是否是布尔值 */
    public static boolean is_bool(Object obj) {
        return obj instanceof Boolean;
    }
    
    /** 判断是否是数组 */
    public static boolean is_array(Object obj) {
        return obj instanceof JSONArray;
    }
    
    /** 判断是否是Map */
    public static boolean is_map(Object obj) {
        return obj instanceof JSONObject;
    }
    
    public static boolean is_scalar(Object obj) {
        if (obj instanceof Integer) return true;
        if (obj instanceof Long) return true;
        if (obj instanceof Boolean) return true;
        if (obj instanceof String) return true;
        if (obj instanceof Float) return true;
        if (obj instanceof Double) return true;
        return false;
    }
    
    public static boolean is_object(Object obj) {
        return true;
    }
    
    /** 判断数组中是否存在关键词 */
    public static boolean array_key_exists(String key, JSONObject array) {
        return array.containsKey(key);
    }
    
    /** 数组合并 */
    public static JSONObject array_merge(JSONObject a1, JSONObject a2) {
        JSONObject result = new JSONObject();
        JSONObject a1Copy = (JSONObject)a1.clone();
        for (String key : a1Copy.keySet()) {
            result.put(key, a1Copy.get(key));
        }
        JSONObject a2Copy = (JSONObject)a2.clone();
        for (String key : a2Copy.keySet()) {
            if (result.containsKey(key) && a2Copy.containsKey(key) && (result.get(key) instanceof JSONArray) && (a2Copy.get(key) instanceof JSONArray)) {
                JSONArray aa1 = result.getJSONArray(key);
                JSONArray aa2 = a2Copy.getJSONArray(key);
                aa1.addAll(aa2);
                result.put(key, aa1);
            } else {
                result.put(key, a2Copy.get(key));
            }
        }
        return result;
    }
    
    /** 数组合并 */
    public static JSONArray array_merge(JSONArray a1, JSONArray a2) {
        JSONArray result = new JSONArray();
        for (int i = 0; i < a1.size(); i++) {
            Object v = a1.get(i);
            result.add(v);
        }
        for (int i = 0; i < a2.size(); i++) {
            Object v = a2.get(i);
            result.add(v);
        }
        return result;
    }
    
    /** 数组取唯一 */
    public static JSONArray array_unique(JSONArray a1) {
        JSONArray result = new JSONArray();
        for (int i = 0; i < a1.size(); i++) {
            String v = a1.getString(i);
            boolean exist = false;
            for (int j = 0; j < result.size(); j++) {
                String vv = result.getString(j);
                if (vv.equals(v)) {
                    exist = true;
                    break;
                }
            }
            if (!exist) {
                result.add(v);
            }
        }
        return result;
    }
    
    /** 取数据中的关键词形成一个数组 */
    public static JSONArray array_keys(JSONObject arr) {
        JSONArray result = new JSONArray();
        for (String key : arr.keySet()) {
            result.add(key);
        }
        return result;
    }
    
    public static String key(JSONArray array) {
        return array.getString(0);
    }
    
    /** 取数据中的关键词形成一个数组 */
    public static JSONArray array_values(JSONObject arr) {
        JSONArray result = new JSONArray();
        for (String key : arr.keySet()) {
            result.add(arr.get(key));
        }
        return result;
    }
    
    public static JSONArray array_column(JSONArray array, String field) {
        JSONArray result = new JSONArray();
        for (int i = 0; i < array.size(); i++) {
            JSONObject obj = array.getJSONObject(i);
            result.add(obj.get(field));
        }
        return result;
    }
    
    /** 数组中的关键词修改大小写 */
    public static JSONObject array_change_key_case(JSONObject obj, boolean lowerCase) {
        JSONObject result = new JSONObject();
        for (String key : obj.keySet()) {
            result.put(key.toLowerCase(), obj.get(key));
        }
        return result;
    }
    
    public static interface callback {
        public String execute(String str);
    }
    /** 对数组中的每一个元素制定特定的函数操作 */
    public static JSONArray array_map(callback callback, JSONArray array) {
        JSONArray ret = new JSONArray();
        for (int i = 0; i < array.size(); i++) {
            ret.add(callback.execute(array.getString(i)));
        }
        return ret;
    }
    
    /** 获取两个数组中不同的元素 */
    public static JSONArray array_diff(JSONArray array1, JSONArray array2) {
        JSONArray ret = new JSONArray();
        for (int i = 0; i < array1.size(); i++) {
            String s1 = array1.getString(i);
            boolean exist = false;
            for (int j = 0; j < array2.size(); j++) {
                String s2 = array2.getString(j);
                if (s1.equals(s2)) {
                    exist = true;
                    break;
                }
            }
            if (!exist) {
                ret.add(s1);
            }
        }
        return array1;
    }
    
    public static String array_search(Object needle, JSONObject obj) {
        for (String key : obj.keySet()) {
            if (obj.get(key).equals(needle)) {
                return key;
            }
        }
        return null;
    }
    
    /** 判断元素是否在数组中存在 */
    public static boolean in_array(String needle, String [] haystack) {
        for (String item : haystack) {
            if (item.equals(needle)) {
                return true;
            }
        }
        return false;
    }
    
    /** 判断元素是否在数组中存在 */
    public static boolean in_array(Object needle, JSONArray haystack) {
        for (int i = 0; i < haystack.size(); i++) {
            if (haystack.get(i).equals(needle)) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean in_array(Integer needle, Integer [] haystack) {
        for (Integer item : haystack) {
            if (item == needle) {
                return true;
            }
        }
        return false;
    }
    
    /** 弹出数组的最后一个元素 */
    public static Object array_pop(JSONArray array) {
        return array.remove(array.size() - 1);
    }
    
    /** 弹出数组的第一个元素 */
    public static void array_shift(JSONArray array) {
        array.remove(0);
    }
    
    public static int array_push(JSONArray array, Object elem) {
        array.add(elem);
        return array.size();
    }
    
    /** 字符串拆分成数组 */
    public static JSONArray explode(String explode, String string) {
        JSONArray ret = new JSONArray();
        if (explode == "|") {
            explode = "\\|";
        }
        String [] explodeValues = string.split(explode);
        for (String explodeValue : explodeValues) {
            ret.add(explodeValue);
        }
        return ret;
    }
    
    /** 根据分隔符组装字符串 */
    public static String implode(String glue, JSONArray pieces) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < pieces.size(); i++) {
            if (i != 0) {
                ret.append(glue);
            }
            ret.append(pieces.getString(i));
        }
        return ret.toString();
    }
    
    /** 获取数组的关键词 */
    public static String key(JSONObject obj) {
        for (String k : obj.keySet()) {
            return k;
        }
        return null;
    }
    
    /** 正则表达式匹配 */
    public static boolean preg_match(String pattern, String subject) {
        return Pattern.matches(pattern, subject);
    }
    
    /** 组装数组 */
    public static JSONArray array(Object [] arr) {
        JSONArray ret = new JSONArray();
        for (Object item : arr) {
            ret.add(item);
        }
        return ret;
    }
    
    public static Object reset(JSONArray pieces) {
        return pieces.get(0);
    }
    
    /** 判断对象类型 */
    public static Object judgeObject(Object obj) {
        if (obj == null) return obj;
        if (obj instanceof String) {
            String str = (String)obj;
            if (str.startsWith("{") && str.endsWith("}")) {
                return JSON.parseObject(str);
            }
            if (str.startsWith("[") && str.endsWith("]")) {
                return JSON.parseArray(str);
            }
        }
        return obj;
    }
    
    /** 获取字符串长度 */
    public static int strlen(String s) {
        return s.length();
    }
    
    /** 取字符串子串 */
    public static String substr(String string, int start) {
        return substr(string, start, null);
    }
    
    /** 取字符串子串 */
    public static String substr(String string, int start, Integer length) {
        if (length == null) {
            return string.substring(start);
        } else {
            return string.substring(start, start + length);
        }
    }
    
    /** 字符串替换 */
    public static String str_replace(String search, String replace, String subject) {
        return subject.replace(search, replace);
    }
    
    /** 字符串替换 */
    public static String str_replace(String[] search, String replace, String subject) {
        String result = subject;
        for (String s : search) {
            result = result.replace(s, replace);
        }
        return result;
    }
    
    public static String str_replace(String[] search, String [] replace, String subject) {
        for (int i = 0; i < search.length && i < replace.length; i++) {
            subject = subject.replace(search[i], replace[i]);
        }
        return subject;
    }
    
    public static String substr_replace(String string, String replacement, int start) {
        return substr_replace(string, replacement, start, null);
    }
    
    public static String substr_replace(String string, String replacement, int start, Integer length) {
        StringBuilder sb = new StringBuilder();
        if (length == null) {
            if (start >= 0) {
                for (int i = 0; i < start; i++) {
                    sb.append(string.charAt(i));
                }
                for (int i = 0; i < replacement.length(); i++) {
                    sb.append(replacement.charAt(i));
                }
                for (int i = start + replacement.length(); i < string.length(); i++) {
                    sb.append(string.charAt(i));
                }
            } else {
                for (int i = 0; i < string.length() + start; i++) {
                    sb.append(string.charAt(i));
                }
                for (int i = 0; i < replacement.length(); i++) {
                    sb.append(replacement.charAt(i));
                }
                for (int i = string.length() + start + replacement.length(); i < string.length(); i++) {
                    sb.append(string.charAt(i));
                }
            }
        } else {
            if (start >= 0) {
                for (int i = 0; i < start; i++) {
                    sb.append(string.charAt(i));
                }
                if (length > 0) {
                    for (int i = 0; i < length; i++) {
                        sb.append(replacement.charAt(i));
                    }
                    for (int i = start + length; i < string.length(); i++) {
                        sb.append(string.charAt(i));
                    }
                } else if (length < 0) {
                    for (int i = replacement.length() + length; i < replacement.length(); i++) {
                        sb.append(replacement.charAt(i));
                    }
                    for (int i = start - length; i < string.length(); i++) {
                        sb.append(string.charAt(i));
                    }
                } else {
                    for (int i = 0; i < replacement.length(); i++) {
                        sb.append(replacement.charAt(i));
                    }
                    for (int i = start; i < string.length(); i++) {
                        sb.append(string.charAt(i));
                    }
                }
            } else {
                for (int i = 0; i < string.length() + start; i++) {
                    sb.append(string.charAt(i));
                }
                if (length > 0) {
                    for (int i = 0; i < length; i++) {
                        sb.append(replacement.charAt(i));
                    }
                    for (int i = string.length() + start + length; i < string.length(); i++) {
                        sb.append(string.charAt(i));
                    }
                } else if (length < 0) {
                    for (int i = replacement.length() + length; i < replacement.length(); i++) {
                        sb.append(replacement.charAt(i));
                    }
                    for (int i = string.length() + start - length; i < string.length(); i++) {
                        sb.append(string.charAt(i));
                    }
                } else {
                    for (int i = 0; i < replacement.length(); i++) {
                        sb.append(replacement.charAt(i));
                    }
                    for (int i = string.length() + start; i < string.length(); i++) {
                        sb.append(string.charAt(i));
                    }
                }
            }
        }
        return sb.toString();
    }
    
    public static int strpos(String haystack, String needle) {
        return haystack.indexOf(needle);
    }
    
    public static String strtolower(String str) {
        return str.toLowerCase();
    }
    
    public static String strtoupper(String str) {
        return str.toUpperCase();
    }
    
    public static String md5(String str) {
        return Utils.HashToMD5Hex(str);
    }
    
    public static String uniqid() {
        return uniqid("", false);
    }
    
    public static String uniqid(String prefix, boolean more_entropy) {
        return UUID.randomUUID().toString();
    }
    
    public static int strtotime(String str) {
        if (str.length() == 10) {
            return (int)Utils.getTime(str, "yyyy-MM-dd");
        } else {
            return (int)Utils.getTime(str, "yyyy-MM-dd HH:mm:ss");
        }
    }
    
    public static String date(String format, String timestamp) {
        return Utils.formatTimeString(Utils.parseLongValue(timestamp, 0), format);
    }
    
    public static String date() {
        return date("yyyy-MM-dd HH:mm:ss", Utils.getTimeNow());
    }
    
    public static String date(String format) {
        return date(format, Utils.getTimeNow());
    }
    
    public static String date(String format, long timestamp) {
        return Utils.formatTimeString(timestamp, format);
    }

    public static String ltrim(String s) {
        int i = 0;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) { i++; }
        return s.substring(i);
    }
    
    public static String rtrim(String s) {
        int i = s.length() - 1;
        while (i >= 0 && Character.isWhitespace(s.charAt(i))) { i--; }
        return s.substring(0,i+1);
    }
    
    public static String trim(String str) {
        return str.trim();
    }
    
    public static final int FILE_APPEND = 8;
    public static void file_put_contents(String filename, String data, int flags) {
        boolean append = (flags & FILE_APPEND) != 0;
        try { 
            //打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件 
            FileWriter writer = new FileWriter(filename, append); 
            writer.write(data); 
            writer.close(); 
        } catch (IOException e) { 
            e.printStackTrace(); 
        } 
    }
    
    public static String file_get_contents(String filename) {
        return Utils.LoadStringFromFile(filename);
    }

    public static int time() {
        return (int)Utils.getTimeNow();
    }
    
    public static int rand(int min, int max) {
        return Utils.getRandom(min, max);
    }
}
