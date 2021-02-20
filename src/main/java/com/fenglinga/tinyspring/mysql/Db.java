package com.fenglinga.tinyspring.mysql;

import java.util.HashMap;
import java.util.Map.Entry;

import com.alibaba.fastjson.JSONObject;
import com.fenglinga.tinyspring.common.Constants;
import com.fenglinga.tinyspring.common.Utils;

public class Db extends BaseObject {
	private static HashMap<String,Connection> instance = new HashMap<String,Connection>();
	
	// 数据库初始化 并取得数据库类实例
	public static Connection connect(JSONObject config, String name) {
		if (empty(name)) {
			name = Utils.HashToMD5Hex(config.toString() + Thread.currentThread().getId());
		}
		if (!instance.containsKey(name)) {
			// 解析连接参数 支持数组和字符串
			JSONObject options = parseConfig(config);
			Connection connection = new Connection(options);
			instance.put(name, connection);
			return connection;
		}
		return instance.get(name);
	}
	
	public static void ping() {
		do {
			boolean hasInstanceRemoved = false;
			for (Entry<String,Connection> entry : instance.entrySet()) {
				try {
					String lastSql = entry.getValue().getLastSql();
					if (lastSql.equals("SELECT 1")) {
						entry.getValue().close();
						instance.remove(entry.getKey());
						hasInstanceRemoved = true;
						break;
					}
					entry.getValue().execute("SELECT 1");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (!hasInstanceRemoved) {
				break;
			}
		} while (true);
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
	
	public static String getLastSql() {
		Query query = new Query(null);
		return query.getLastSql();
	}
}
