package com.fenglinga.tinyspring.mysql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fenglinga.tinyspring.common.Utils;

public class Builder extends BaseObject {
    // connection对象实例
    protected Connection connection;
    // 查询对象实例
    protected Query query;
    
	public static HashMap<String, String> exp = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;
		{
			put("eq", "=");put("neq", "<>");put("gt", ">");put("egt", ">=");put("lt", "<");put("elt", "<=");put("notlike", "NOT LIKE");put("like", "LIKE");put("in", "IN");put("exp", "EXP");put("notin", "NOT IN");
			put("not in", "NOT IN");put("between", "BETWEEN");put("not between", "NOT BETWEEN");put("exists", "EXISTS");put("notexists", "NOT EXISTS");put("not exists", "NOT EXISTS");put("null", "NULL");put("notnull", "NOT NULL");
			put("not null", "NOT NULL");put("> time", "> TIME");put("< time", "< TIME");put(">= time", ">= TIME");put("<= time", "<= TIME");put("between time", "BETWEEN TIME");put("not between time", "NOT BETWEEN TIME");put("notbetween time", "NOT BETWEEN TIME");
		}
	};

    protected final String selectSql    = "SELECT%DISTINCT% %FIELD% FROM %TABLE%%FORCE%%JOIN%%WHERE%%GROUP%%HAVING%%ORDER%%LIMIT% %UNION%%LOCK%%COMMENT%";
    protected final String insertSql    = "%INSERT% INTO %TABLE% (%FIELD%) VALUES (%DATA%) %COMMENT%";
    protected final String insertAllSql = "INSERT INTO %TABLE% (%FIELD%) %DATA% %COMMENT%";
    protected final String updateSql    = "UPDATE %TABLE% SET %SET% %JOIN% %WHERE% %ORDER%%LIMIT% %LOCK%%COMMENT%";
    protected final String deleteSql    = "DELETE FROM %TABLE% %USING% %JOIN% %WHERE% %ORDER%%LIMIT% %LOCK%%COMMENT%";

    public Builder(Connection connection, Query query) {
    	this.connection = connection;
    	this.query = query;
    }
    
    // 将SQL语句中的__TABLE_NAME__字符串替换成带前缀的表名（小写）
    protected String parseSqlTable(String sql) {
    	return this.query.parseSqlTable(sql);
    }
    
    protected String parseKey(String key, JSONObject options) {
    	return key;
    }
    
    protected String parseTable(Object tables, JSONObject options) {
    	List<String> item = new ArrayList<String>();
    	if (tables instanceof String) {
    		String table = (String)tables;
    		table = parseSqlTable(table);
    		JSONObject alias = options.getJSONObject("alias");
    		if (alias != null && alias.containsKey(table)) {
    			item.add(this.parseKey(table, options) + " " + this.parseKey(alias.getString(table), options));
    		} else {
    			item.add(this.parseKey(table, options));
    		}
    	} else if (tables instanceof JSONObject) {
    		JSONObject tableObject = (JSONObject)tables;
    		for (String key : tableObject.keySet()) {
                key = this.parseSqlTable(key);
    			item.add(this.parseKey(key, options) + " " + this.parseKey(tableObject.getString(key), options));
    		}
    	}
    	return Utils.buildStringArray(item, ",");
    }
    
    protected String parseDistinct(Object distinct) {
    	return !empty(distinct) ? " DISTINCT " : "";
    }
    
    protected String parseField(Object fields, JSONObject options) {
    	List<String> item = new ArrayList<String>();
    	if (empty(fields)) {
    		item.add("*");
    	}
    	if (fields instanceof String) {
    		String field = (String)fields;
    		if (field.equals("*")) {
        		item.add("*");
    		}
    	} else if (fields instanceof JSONObject) {
    		// 支持 'field1'=>'field2' 这样的字段别名定义
    		JSONObject fieldObject = (JSONObject)fields;
    		for (String key : fieldObject.keySet()) {
    			String field = fieldObject.getString(key);
    			item.add(this.parseKey(key, options) + " AS " + this.parseKey(field, options));
    		}
    	} else if (fields instanceof JSONArray) {
    		JSONArray fieldArray = (JSONArray)fields;
    		for (int i = 0; i < fieldArray.size(); i++) {
    			String field = fieldArray.getString(i);
    			item.add(this.parseKey(field, options));
    		}
    	}
    	return Utils.buildStringArray(item, ",");
    }
    
    protected String parseJoin(Object join, JSONObject options) throws Exception {
    	String joinStr = "";
    	if (!empty(join) && is_array(join)) {
    		JSONArray joinArray = (JSONArray)join;
    		for (int i = 0; i < joinArray.size(); i++) {
    			JSONArray item = joinArray.getJSONArray(i);
    			Object table = item.get(0);
    			String type = item.getString(1);
    			JSONArray on = new JSONArray();
    			if (is_string(item.get(2))) {
    				on.add(item.getString(2));
    			} else if (is_array(item.get(2))) {
    				on = item.getJSONArray(2);
    			} else {
    				throw new Exception("invalid join 'on' type");
    			}
    			JSONArray condition = new JSONArray();
    			for (int j = 0; j < on.size(); j++) {
    				String val = on.getString(j);
    				if (val.indexOf("=") > 0) {
    					JSONArray vals = explode("=", val);
    					String val1 = vals.getString(0);
    					String val2 = vals.getString(1);
    					condition.add(this.parseKey(val1, options) + "=" + this.parseKey(val2, options));
    				} else {
    					condition.add(val);
    				}
    			}
                String tableStr = this.parseTable(table, options);
                joinStr += ' ' + type + " JOIN " + tableStr + " ON " + implode(" AND ", condition);
    		}
    	}
    	return joinStr;
    }
    
    protected String parseWhere(JSONObject where, JSONObject options) throws Exception {
    	String whereStr = this.buildWhere(where, options);
    	return empty(whereStr) ? "" : " WHERE " + whereStr;
    }
    
    private String buildWhere(JSONObject where, JSONObject options) throws Exception {
    	if (empty(where)) {
            where = new JSONObject();
        }
    	StringBuilder whereStr = new StringBuilder();
    	JSONObject binds = this.query.getFieldsBind(options);
    	for (String key : where.keySet()) {
    		JSONArray str = new JSONArray();
    		JSONObject val = where.getJSONObject(key);
    		for (String field : val.keySet()) {
    			Object value = val.get(field);
    			if (field.indexOf("|") >= 0) {
    				// 不同字段使用相同查询条件（OR）
    				JSONArray array = explode("|", field);
    				JSONArray item = new JSONArray();
    				for (int i = 0; i < array.size(); i++) {
    					String k = array.getString(i);
    					item.add(this.parseWhereItem(k, value, "", options, binds));
    				}
    				str.add(" " + key + " ( " + implode(" OR ", item) + " )");
    			} else if (field.indexOf("&") >= 0) {
    				// 不同字段使用相同查询条件（AND）
    				JSONArray array = explode("&", field);
    				JSONArray item = new JSONArray();
    				for (int i = 0; i < array.size(); i++) {
    					String k = array.getString(i);
    					item.add(this.parseWhereItem(k, value, "", options, binds));
    				}
    				str.add(" " + key + " ( " + implode(" AND ", item) + " )");
    			} else {
                    // 对字段使用表达式查询
    				field = is_string(field) ? field : "";
    				str.add(" " + key + " " + this.parseWhereItem(field, value, key, options, binds));
    			}
    		}
    		
    		whereStr.append(whereStr.length() == 0 ? substr(implode(" ", str), strlen(key) + 1) : implode(" ", str));
    	}
    	return whereStr.toString();
    }
    
    protected String parseWhereItem(String field, Object val, String rule, JSONObject options, JSONObject binds) throws Exception {
    	return parseWhereItem(field, val, rule, options, binds, null);
    }
    
    protected String parseWhereItem(String field, Object val, String rule, JSONObject options, JSONObject binds, String bindName) throws Exception {
    	String key = !empty(field) ? this.parseKey(field, options) : "";
    	
    	// 查询规则和条件
    	if (!is_array(val)) {
    		val = array(new Object[]{"=", val});
    	}
    	JSONArray valArray = (JSONArray)val;
    	Object exp = valArray.get(0);
    	Object value = valArray.size() > 1 ? valArray.get(1) : null;
    	
    	// 对一个字段使用多个查询条件
        if (is_array(exp)) {
        	Object item = array_pop(valArray);
            // 传入 or 或者 and
            if (is_string(item) && in_array((String)item, new String[]{"AND", "and", "OR", "or"})) {
                rule = (String)item;
            } else {
                array_push(valArray, item);
            }
            JSONArray str = new JSONArray();
            for (int i = 0; i < valArray.size(); i++) {
                bindName = "where_" + str_replace(".", "_", field) + "_" + i;
                str.add(this.parseWhereItem(field, valArray.get(i), rule, options, binds, bindName));
            }
            return "( " + implode(" " + rule + " ", str) + " )";
        }
        
        // 检测操作符
        // TODO:
        if (Builder.exp.containsKey(exp)) {
            exp = strtolower((String)exp);
            if (Builder.exp.containsKey(exp)) {
                exp = Builder.exp.get(exp);
            } else {
                throw new Exception("where express error:" + exp);
            }
        }
        
        bindName = !empty(bindName) ? bindName : "where_" + str_replace(new String[]{".", "-"}, "_", field);
        if (preg_match(".*\\W.*", bindName)) {
            // 处理带非单词字符的字段名
            bindName = md5(bindName);
        }
        
        int bindType = binds.containsKey(field) ? binds.getInteger(field) : PDO.PARAM_STR;
        if (is_scalar(value) && array_key_exists(field, binds) && !in_array((String)exp, new String[] {"EXP", "NOT NULL", "NULL", "IN", "NOT IN", "BETWEEN", "NOT BETWEEN"}) && strpos((String)exp, "TIME") < 0) {
        	if (strpos(String.valueOf(value), ":") != 0 || !this.query.isBind(substr(String.valueOf(value), 1))) {
                if (this.query.isBind(bindName)) {
                    bindName += "_" + str_replace(".", "_", uniqid("", true));
                }
                this.query.bind(bindName, value, bindType);
                value = ":" + bindName;
            }
        }

        String whereStr = "";
        if (in_array((String)exp, new String[] {"=", "<>", ">", ">=", "<", "<="})) {
            // 比较运算 及 模糊匹配
            whereStr += key + " " + exp + " " + this.parseValue(value, field);
        } else if ("LIKE".equals((String)exp) || "NOT LIKE".equals((String)exp)) {
            if (is_array(value)) {
            	JSONArray array = new JSONArray();
                for (int i = 0; i < valArray.size(); i++) {
                	Object item = valArray.get(i);
                	array.add(key + " " + exp + " " + this.parseValue(item, field));
                }
                String logic = valArray.size() > 2 ? valArray.getString(2) : "AND";
                whereStr += "(" + implode(" " + strtoupper(logic) + " ", array) + ")";
            } else {
                whereStr += key + " " + exp + " " + this.parseValue(value, field);
            }
        } else if ("EXP".equals(exp)) {
            // 表达式查询
            whereStr += "( " + key + " " + value + " )";
        } else if (in_array((String)exp, new String[] {"NOT NULL", "NULL"})) {
            // NULL 查询
            whereStr += key + " IS " + exp;
        } else if (in_array((String)exp, new String[] {"NOT IN", "IN"})) {
            // IN 查询
            value = is_array(value) ? value : explode(",", (String)value);
            valArray = (JSONArray)value;
            String zone = null;
            if (array_key_exists(field, binds)) {
                JSONObject bind = new JSONObject();
                JSONArray array = new JSONArray();
                for (int i = 0; i < valArray.size(); i++) {
                	String k = String.valueOf(i);
                	Object v = valArray.get(i);
                	String bindKey = null;
                    if (this.query.isBind(bindName + "_in_" + k)) {
                        bindKey = bindName + "_in_" + uniqid() + "_" + k;
                    } else {
                        bindKey = bindName + "_in_" + k;
                    }
                    bind.put(bindKey, array(new Object[] {v, bindType}));
                    array.add(":" + bindKey);
                }
                this.query.bind(bind);
                zone = implode(",", array);
            } else {
                zone = implode(",", (JSONArray)this.parseValue(value, field));
            }
            whereStr += key + " " + exp + " (" + (empty(zone) ? "''" : zone) + ")";
        } else if (in_array((String)exp, new String[] {"NOT BETWEEN", "BETWEEN"})) {
            // BETWEEN 查询
        	JSONArray data = (JSONArray)(is_array(value) ? value : explode(",", (String)value));
        	String between = null;
            if (array_key_exists(field, binds)) {
            	String bindKey1 = null;
            	String bindKey2 = null;
                if (this.query.isBind(bindName + "_between_1")) {
                    bindKey1 = bindName + "_between_1" + uniqid();
                    bindKey2 = bindName + "_between_2" + uniqid();
                } else {
                    bindKey1 = bindName + "_between_1";
                    bindKey2 = bindName + "_between_2";
                }
                JSONObject bind = new JSONObject();
                bind.put(bindKey1, array(new Object[] {data.get(0), bindType}));
                bind.put(bindKey2, array(new Object[] {data.get(1), bindType}));
                this.query.bind(bind);
                between = ":" + bindKey1 + " AND :" + bindKey2;
            } else {
                between = this.parseValue(data.get(0), field) + " AND " + this.parseValue(data.get(1), field);
            }
            whereStr += key + " " + exp + " " + between;
        } else if (in_array((String)exp, new String[] {"NOT EXISTS", "EXISTS"})) {
            // EXISTS 查询
            whereStr += exp + " (" + value + ")";
        } else if (in_array((String)exp, new String[] {"< TIME", "> TIME", "<= TIME", ">= TIME"})) {
            whereStr += key + " " + substr((String)exp, 0, 2) + " " + this.parseDateTime((String)value, field, options, bindName, bindType);
        } else if (in_array((String)exp, new String[] {"BETWEEN TIME", "NOT BETWEEN TIME"})) {
            if (is_string(value)) {
                value = explode(",", (String)value);
            }

            JSONArray data = (JSONArray)value;
            whereStr += key + " " + substr((String)exp, 0, -4) + this.parseDateTime(data.getString(0), field, options, bindName + "_between_1", bindType) + " AND " + this.parseDateTime(data.getString(1), field, options, bindName + "_between_2", bindType);
        }
        return whereStr;
    }
    
    protected String parseDateTime(String value, String key, JSONObject options, String bindName, int bindType) throws Exception
    {
    	String table = null;
        // 获取时间字段类型
        if (strpos(key, ".") >= 0) {
        	JSONArray arr = explode(".", key);
        	table = arr.getString(0);
        	key = arr.getString(1);
            if (options.containsKey("alias")) {
            	String pos = array_search(table, (JSONObject)options.get("alias"));
            	if (pos != null) {
                    table = pos;
            	}
            }
        } else {
            table = options.getString("table");
        }
        String info = null;
        JSONObject type = (JSONObject)this.query.getTableInfo(table, "type");
        if (type.containsKey(key)) {
            info = type.getString(key);
        }
        if (info != null) {
            if (is_string(value)) {
            	int t = strtotime(value);
                value = String.valueOf(t > 0 ? t : value);
            }

            if (preg_match("/(datetime|timestamp)/is", info)) {
                // 日期及时间戳类型
                value = date("yyyy-MM-dd HH:mm:ss", value);
            } else if (preg_match("/(date)/is", info)) {
                // 日期及时间戳类型
                value = date("yyyy-MM-dd", value);
            }
        }
        bindName = !empty(bindName) ? bindName : key;
        this.query.bind(bindName, value, bindType);
        return ":" + bindName;
    }

    protected Object parseValue(Object value, String field) throws Exception {
    	if (is_string(value)) {
            value = strpos((String)value, ":") == 0 && this.query.isBind(substr((String)value, 1)) ? value : this.connection.quote((String)value);
        } else if (is_array(value)) {
        	JSONArray valueArray = (JSONArray)value;
        	for (int i = 0; i < valueArray.size(); i++) {
        		Object v = valueArray.get(i);
        		valueArray.set(i, this.parseValue(v, field));
        	}
        } else if (is_bool(value)) {
            value = (Boolean)value ? "1" : "0";
        } else if (is_null(value)) {
            value = "null";
        }
        return value;
    }
    
    protected String parseGroup(String group) {
    	return !empty(group) ? " GROUP BY " + group : "";
    }
    
    protected String parseHaving(String having) {
    	return !empty(having) ? " HAVING " + having : "";
    }
    
    protected String parseRand() {
    	return "rand()";
    }
    
    protected String parseOrder(Object order, JSONObject options) throws Exception {
        if (is_map(order)) {
            JSONArray array = new JSONArray();
            JSONObject orderObj = (JSONObject)order;
            for (String key : orderObj.keySet()) {
            	String val = orderObj.getString(key);
                if (is_numeric(key)) {
                    if ("[rand]".equals(val)) {
                    	array.add(this.parseRand());
                    } else if (strpos(val, "(") < 0) {
                    	array.add(this.parseKey(val, options));
                    } else {
                    	array.add(val);
                    }
                } else {
                    String sort = in_array(strtolower(trim(val)), new String[] {"asc", "desc"}) ? " " + val : "";
                    array.add(this.parseKey(key, options) + " " + sort);
                }
            }
            order = implode(",", array);
        }
        return !empty(order) ? " ORDER BY " + order : "";
    }
    
    protected String parseLimit(String limit) {
        return (!empty(limit) && limit.indexOf("(") < 0) ? " LIMIT " + limit + " " : "";
    }
    
    protected String parseUnion(Object union) {
        if (empty(union)) {
            return "";
        }
        JSONObject unionObject = (JSONObject)union;
        String type = unionObject.getString("type");
        unionObject.remove("type");
        JSONArray sql = new JSONArray();
        for (String key : unionObject.keySet()) {
        	Object u = unionObject.get(key);
            if (is_string(u)) {
            	sql.add(type + " " + this.parseSqlTable((String)u));
            }
        }
        return implode(" ", sql);
    }
    
    protected String parseLock(Boolean lock) {
    	return lock ? " FOR UPDATE " : "";
    }
    
    protected String parseComment(String comment) {
    	return !empty(comment) ? " /* " + comment + " */" : "";
    }
    
    protected String parseForce(Object index) throws Exception {
    	if (empty(index)) {
    		return "";
    	}
    	if (is_array(index)) {
    		String indexStr = implode(",", (JSONArray)index);
    		return String.format(" FORCE INDEX ( %s ) ", indexStr);
    	}
    	if (is_string(index)) {
    		return String.format(" FORCE INDEX ( %s ) ", (String)index);
    	}
    	throw new Exception("invalid force 'index' type");
    }
    
    protected JSONObject parseData(JSONObject data, JSONObject options) throws Exception
    {
        if (empty(data)) {
            return new JSONObject();
        }

        // 获取绑定信息
        JSONArray fields = null;
        JSONObject bind = this.query.getFieldsBind(options);
        if (is_string(options.get("field")) && "*".equals(options.getString("field"))) {
            fields = array_keys(bind);
        } else {
            fields = options.getJSONArray("field");
        }

        JSONObject result = new JSONObject();
        for (String key : data.keySet()) {
        	Object val = data.get(key);
        	String item = this.parseKey(key, options);
        	if (strpos(key, ".") < 0 && !in_array(key, fields)) {
                if (options.containsKey("strict")) {
                    throw new Exception("fields not exists:[" + key + "]");
                }
            } else if (is_array(val)) {
            	JSONArray valArray = (JSONArray)val;
            	if ("exp".equals(valArray.getString(0))) {
            		result.put(item, valArray.getString(1));
            	}
            } else if (is_null(val)) {
            	result.put(item, "NULL");
            } else if (is_scalar(val)) {
                // 过滤非标量数据
                if (0 == strpos(String.valueOf(val), ":") && this.query.isBind(substr(String.valueOf(val), 1))) {
                    result.put(item, val);
                } else {
                    key = str_replace(".", "_", key);
                    this.query.bind("__data__" + key, val, bind.containsKey(key) ? bind.getIntValue(key) : PDO.PARAM_STR);
                    result.put(item, ":__data__" + key);
                }
            } else if (is_object(val)) {
                // 对象数据写入
            	result.put(item, val.toString());
            }
        }
        return result;
    }
    
    protected JSONArray parseDataSet(JSONArray dataSet, JSONObject options) throws Exception
    {
        // 获取绑定信息
        JSONArray fields = null;
        JSONObject bind = this.query.getFieldsBind(options);
        if (is_string(options.get("field")) && "*".equals(options.getString("field"))) {
            fields = array_keys(bind);
        } else {
            fields = options.getJSONArray("field");
        }

        JSONArray values = new JSONArray();
        for (int i = 0; i < dataSet.size(); i++) {
        	JSONObject data = dataSet.getJSONObject(i);
	        JSONObject result = new JSONObject();
	        for (String key : data.keySet()) {
	        	Object val = data.get(key);
	        	if (strpos(key, ".") < 0 && !in_array(key, fields)) {
	                if (options.containsKey("strict")) {
	                    throw new Exception("fields not exists:[" + key + "]");
	                }
	            } else if (is_null(val)) {
	            	result.put(key, "NULL");
	            } else if (is_scalar(val)) {
                    result.put(key, this.parseValue(val, key));
	            } else if (is_object(val)) {
	                // 对象数据写入
	            	result.put(key, val.toString());
	            }
	        }
            JSONArray value = array_values(result);
            values.add("SELECT " + implode(",", value));
        }
        return values;
    }

    public String select(JSONObject options) throws Exception {
    	String sql = selectSql.replace("%TABLE%", this.parseTable(options.get("table"), options));
    	sql = sql.replace("%DISTINCT%", this.parseDistinct(options.get("distinct")));
    	sql = sql.replace("%FIELD%", this.parseField(options.get("field"), options));
    	sql = sql.replace("%JOIN%", this.parseJoin(options.get("join"), options));
    	sql = sql.replace("%WHERE%", this.parseWhere(options.getJSONObject("where"), options));
    	sql = sql.replace("%GROUP%", this.parseGroup(options.getString("group")));
    	sql = sql.replace("%HAVING%", this.parseHaving(options.getString("having")));
    	sql = sql.replace("%ORDER%", this.parseOrder(options.get("order"), options));
    	sql = sql.replace("%LIMIT%", this.parseLimit(options.getString("limit")));
    	sql = sql.replace("%UNION%", this.parseUnion(options.get("union")));
    	sql = sql.replace("%LOCK%", this.parseLock(options.getBoolean("lock")));
    	sql = sql.replace("%COMMENT%", this.parseComment(options.getString("comment")));
    	sql = sql.replace("%FORCE%", this.parseForce(options.get("force")));
    	return sql;
    }
    
    public String insert(JSONObject data, JSONObject options, boolean replace) throws Exception {
        // 分析并处理数据
        data = this.parseData(data, options);
        if (empty(data)) {
            return "0";
        }
        JSONArray fields = array_keys(data);
        JSONArray values = array_values(data);
        
    	String sql = insertSql.replace("%INSERT%", replace ? "REPLACE" : "INSERT");
    	sql = sql.replace("%TABLE%", this.parseTable(options.get("table"), options));
    	sql = sql.replace("%FIELD%", implode(" , ", fields));
    	sql = sql.replace("%DATA%", implode(" , ", values));
    	sql = sql.replace("%COMMENT%", this.parseComment(options.getString("comment")));
    	return sql;
    }
    
    public String insertAll(JSONArray dataSet, final JSONObject options) throws Exception {
    	JSONArray values = parseDataSet(dataSet, options);
    	JSONArray fields = array_map(new callback() {
			@Override
			public String execute(String str) {
				return Builder.this.parseKey(str, options);
			}
        }, array_keys((JSONObject)reset(dataSet)));
    	String sql = insertAllSql.replace("%TABLE%", this.parseTable(options.get("table"), options));
    	sql = sql.replace("%FIELD%", implode(" , ", fields));
    	sql = sql.replace("%DATA%", implode(" UNION ALL ", values));
    	sql = sql.replace("%COMMENT%", this.parseComment(options.getString("comment")));
    	return sql;
    }
    
	public String update(JSONObject data, JSONObject options) throws Exception {
        data = this.parseData(data, options);
        if (empty(data)) {
            return "";
        }
        JSONArray set = new JSONArray();
        for (String key : data.keySet()) {
        	set.add(key + "=" + data.getString(key));
        }
    	String sql = updateSql.replace("%TABLE%", this.parseTable(options.get("table"), options));
    	sql = sql.replace("%SET%", implode(" , ", set));
    	sql = sql.replace("%JOIN%", this.parseJoin(options.get("join"), options));
    	sql = sql.replace("%WHERE%", this.parseWhere(options.getJSONObject("where"), options));
    	sql = sql.replace("%ORDER%", this.parseOrder(options.get("order"), options));
    	sql = sql.replace("%LIMIT%", this.parseLimit(options.getString("limit")));
    	sql = sql.replace("%LOCK%", this.parseLock(options.getBoolean("lock")));
    	sql = sql.replace("%COMMENT%", this.parseComment(options.getString("comment")));
    	return sql;
	}
    
	public String delete(JSONObject options) throws Exception {
    	String sql = deleteSql.replace("%TABLE%", this.parseTable(options.get("table"), options));
    	sql = sql.replace("%USING%", !empty(options.get("using")) ? " USING " + this.parseTable(options.get("using"), options) + " " : "");
    	sql = sql.replace("%JOIN%", this.parseJoin(options.get("join"), options));
    	sql = sql.replace("%WHERE%", this.parseWhere(options.getJSONObject("where"), options));
    	sql = sql.replace("%ORDER%", this.parseOrder(options.get("order"), options));
    	sql = sql.replace("%LIMIT%", this.parseLimit(options.getString("limit")));
    	sql = sql.replace("%LOCK%", this.parseLock(options.getBoolean("lock")));
    	sql = sql.replace("%COMMENT%", this.parseComment(options.getString("comment")));
    	return sql;
	}
}
