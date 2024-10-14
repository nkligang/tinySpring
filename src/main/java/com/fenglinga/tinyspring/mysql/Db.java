package com.fenglinga.tinyspring.mysql;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fenglinga.tinyspring.common.Constants;
import com.fenglinga.tinyspring.common.Pair;
import com.fenglinga.tinyspring.common.Utils;

public class Db extends BaseObject {
    private static ConcurrentHashMap<String, Pair<Long, Connection>> instance = new ConcurrentHashMap<String,Pair<Long, Connection>>();
    
    // 数据库初始化 并取得数据库类实例
    public static Connection connect(JSONObject config, String name) {
        if (empty(name)) {
            name = Utils.HashToMD5Hex(config.toString() + Thread.currentThread().getId());
        }
        if (!instance.containsKey(name)) {
            // 解析连接参数 支持数组和字符串
            JSONObject options = parseConfig(config);
            Connection connection = new Connection(options);
            instance.put(name, new Pair<Long, Connection>(Thread.currentThread().getId(), connection));
            return connection;
        }
        Pair<Long, Connection> pair = instance.get(name);
        return pair.second;
    }
    
    public static void ping() {
        do {
            boolean hasInstanceRemoved = false;
            for (Entry<String,Pair<Long, Connection>> entry : instance.entrySet()) {
                try {
                	Pair<Long, Connection> pair = entry.getValue();
                	Connection conn =  pair.second;
                    String lastSql = conn.getLastSql();
                    if (lastSql.equals("SELECT 1")) {
                    	conn.close();
                        instance.remove(entry.getKey());
                        hasInstanceRemoved = true;
                        break;
                    }
                    conn.execute("SELECT 1");
                } catch (Exception e) {
                	Constants.LOGGER.error(e.getMessage(), e);
                    instance.remove(entry.getKey());
                    hasInstanceRemoved = true;
                }
            }
            if (!hasInstanceRemoved) {
                break;
            }
        } while (true);
    }
    
    public static void recycle() {
        do {
            boolean hasInstanceRemoved = false;
            try {
	            Thread[] threads = new Thread[Thread.activeCount()];
	            Thread.enumerate(threads);
	            Set<Long> threadIds = new HashSet<Long>();
	            for (Thread thread : threads) {
	            	threadIds.add(thread.getId());
	            }
	            for (Entry<String,Pair<Long, Connection>> entry : instance.entrySet()) {
	                Pair<Long, Connection> pair = entry.getValue();
	                if (threadIds.contains(pair.first)) {
	                	continue;
	                }
                	Connection conn =  pair.second;
	                conn.close();
                    instance.remove(entry.getKey());
                    hasInstanceRemoved = true;
                    break;
                }
            } catch (Exception e) {
            	Constants.LOGGER.error(e.getMessage(), e);
            }
            if (!hasInstanceRemoved) {
                break;
            }
        } while (true);
    }
    
    public static void clear() {
        for (Entry<String,Pair<Long, Connection>> entry : instance.entrySet()) {
            try {
            	Pair<Long, Connection> pair = entry.getValue();
            	Connection conn = pair.second;
            	conn.close();
                instance.remove(entry.getKey());
            } catch (Exception e) {
            	Constants.LOGGER.error(e.getMessage(), e);
            }
        }
    }

    public static JSONObject parseConfig(JSONObject config) {
        if (empty(config)) {
            config = Constants.getConfig("database");
        }
        return config;
    }
    
    public static Query name(String name) {
        Query query = new Query(null);
        query.name(name);
        return query;
    }
    
    public static void startTransaction() throws Exception {
        Query query = new Query(null);
        query.startTransaction();
    }
    
    public static void commit() throws Exception {
        Query query = new Query(null);
        query.commit();
    }
    
    public static void rollback() throws Exception {
        Query query = new Query(null);
        query.rollback();
    }
    
    public static JSONArray query(String sql) throws Exception {
        Query query = new Query(null);
        return (JSONArray)query.query(sql);
    }
    
    public static int execute(String sql) throws Exception {
        Query query = new Query(null);
        return query.execute(sql);
    }
    
    public static String getLastSql() {
        Query query = new Query(null);
        return query.getLastSql();
    }
}
