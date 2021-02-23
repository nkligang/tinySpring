package com.fenglinga.tinyspring.mysql;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fenglinga.tinyspring.common.Utils;

public class Query extends BaseObject {
    // 数据库Connection对象实例
    protected Connection connection;
    // 数据库Builder对象实例
    protected Builder builder;
    // 当前数据表名称（含前缀）
    protected String table = "";
    // 当前数据表名称（不含前缀）
    protected String name = "";
    // 当前数据表主键
    protected JSONArray pk;
    // 当前数据表前缀
    protected String prefix = "";
    // 查询参数
	private JSONObject options = new JSONObject();
    // 参数绑定
    protected JSONObject bind = new JSONObject();
    // 数据表信息
    protected static JSONObject info = new JSONObject();

	public Query(Connection connection) {
		this.connection = connection == null ? Db.connect(new JSONObject(), "") : connection;
        this.prefix = (String)this.connection.getConfig("prefix");
        // 设置当前连接的Builder对象
        this.setBuilder();
	}
	
	protected void setBuilder() {
		this.builder = new Builder(this.connection, this);
	}
	
	public Query name(String name) {
		this.name = name;
		return this;
	}
	
	public Query field(Object field) throws Exception {
		return field(field, false, "", "", "");
	}
	
	public Query field(Object field, boolean except, String tableName, String prefix, String alias) throws Exception {
		if (empty(field)) {
			return this;
		}
		JSONArray fieldResult = new JSONArray();
		if (is_string(field)) {
			fieldResult = explode(",", (String)field);
			fieldResult = array_map(new callback() {
				@Override
				public String execute(String str) {
					return str.trim();
				}
			}, fieldResult);
		}
		if (is_bool(field) && (Boolean)field == true) {
			String newTableName = tableName;
			if (this.options.containsKey("table")) {
				newTableName = this.options.getString("table");
			}
			Object fields = this.getTableInfo(newTableName, "fields");
			if (empty(fields)) {
				fieldResult.add("*");
			} else {
				fieldResult = (JSONArray)fields;
			}
		} else if (except) {
			String newTableName = tableName;
			if (this.options.containsKey("table")) {
				newTableName = this.options.getString("table");
			}
			Object fields = this.getTableInfo(newTableName, "fields");
			if (!empty(fields)) {
				fieldResult = array_diff((JSONArray)fields, fieldResult);
			}
		}
		if (!empty(tableName)) {
			if (empty(prefix)) {
				prefix = tableName;
			}
			for (int i = 0; i < fieldResult.size(); i++) {
				String fr = fieldResult.getString(i);
				String val = prefix + "." + fr + (!empty(alias) ? " AS " + alias + fr : "");
				fieldResult.set(i, val);
			}
		}
		if (this.options.containsKey("field")) {
			fieldResult = array_merge(this.options.getJSONArray("field"), fieldResult);
		}
		this.options.put("field", array_unique(fieldResult));
		return this;
	}
	
	public Query join(Object join) throws Exception {
		return join(join, null, "INNER");
	}
	
	public Query join(Object join, Object condition) throws Exception {	
		return join(join, condition, "INNER");
	}
	
	public Query join(Object join, Object condition, String type) throws Exception {
		return parseJoin(judgeObject(join), judgeObject(condition), type);
	}
	
	private Query parseJoin(Object join, Object condition, String type) throws Exception {
		if (empty(condition)) {
			// 如果为组数，则循环调用join
			if (join instanceof JSONObject) {
				JSONObject joinObj = (JSONObject)join;
				for (String key : joinObj.keySet()) {
					Object value = joinObj.get(key);
					if (is_array(value)) {
						JSONArray valueArray = (JSONArray)value;
						if (valueArray.size() >= 2) {
							this.join(valueArray.get(0), valueArray.get(1), valueArray.size() > 2 ? valueArray.getString(2) : type);
						}
					}
				}
			}
		} else {
			Object table = this.getJoinTable(join, null);
			
			JSONArray array = new JSONArray();
			array.add(table);
			array.add(type.toUpperCase());
			array.add(condition);
			if (this.options.containsKey("join")) {
				JSONArray joinArray = this.options.getJSONArray("join");
				joinArray.add(array);
			} else {
				JSONArray joinArray = new JSONArray();
				joinArray.add(array);
				this.options.put("join", joinArray);
			}
		}
		return this;
	}
	
	// 获取Join表名及别名 支持 ['prefix_table或者子查询'=>'alias'] 'prefix_table alias' 'table alias'
	public Object getJoinTable(Object join, String alias) throws Exception {
		Object table = null;
		if (is_map(join)) {
			JSONObject joinObject = (JSONObject)join;
			for (String key : joinObject.keySet()) {
				table = key;
				alias = joinObject.getString(key);
				break;
			}
		} else if (is_string(join)) {
			String joinStr = (String)join;
			if (joinStr.indexOf("(") >= 0) {
				table = joinStr;
			} else {
				String prefix = this.prefix;
				if (joinStr.indexOf(" ") >= 0) {
					JSONArray joins = explode(" ", joinStr);
					if (joins.size() >= 2) {
						table = joins.getString(0);
						alias = joins.getString(1);
					}
				} else {
					table = joinStr;
					if (joinStr.indexOf(".") < 0 && joinStr.indexOf("__") != 0) {
						alias = joinStr;
					}
				}
				if (!empty(prefix) && ((String)table).indexOf(".") < 0 && ((String)table).indexOf(prefix) != 0 && ((String)table).indexOf("__") != 0) {
					table = this.getTable(((String)table));
				}
			}
		} else {
			throw new Exception("invalid parameter 'join' type");
		}
		if (alias != null) {
			JSONObject aliasObj = this.options.getJSONObject("alias");
			if (aliasObj != null) {
				if (aliasObj.containsKey(table)) {
					table = table + "@think" + Utils.generateKeyString();
				}
				JSONObject tableObj = new JSONObject();
				tableObj.put((String)table, alias);
				table = tableObj;
				this.alias(tableObj);
			}
		}
		return table;
	}
	
	private void addOptionAlias(String key, String value) {
		JSONObject aliasObj = this.options.getJSONObject("alias");
		if (aliasObj == null) {
			aliasObj = new JSONObject();
			aliasObj.put(key, value);
			this.options.put("alias", aliasObj);
		} else {
			aliasObj.put(key, value);
		}
	}
	
	public Query alias(Object alias) throws Exception {
		return parseAlias(judgeObject(alias));
	}
	
	private Query parseAlias(Object alias) throws Exception {
		if (is_map(alias)) {
			JSONObject aliasObject = (JSONObject)alias;
			for (String key : aliasObject.keySet()) {
				this.addOptionAlias(key, aliasObject.getString(key));
			}
		} else if (is_string(alias)) {
			if (this.options.containsKey("table")) {
				String table = null;
				Object tableObj = this.options.get("table");
				if (is_map(tableObj)) {
					table = key((JSONObject)tableObj);
				} else if (is_string(tableObj)) {
					table = (String)tableObj;
				} else {
					throw new Exception("invalid option 'table' type");
				}
				if (table.indexOf("__") >= 0) {
					table = this.parseSqlTable(table);
				}
			} else {
				table = this.getTable();
			}
			
			this.addOptionAlias(table, (String)alias);
		} else {
			throw new Exception("invalid parameter 'alias' type");
		}
		return this;
	}
	
	public Query where(Object field) {
		return where(field, null, null);
	}
	
	public Query where(Object field, Object op) {
		return where(field, op, null);
	}
	
	public Query where(Object field, Object op, Object condition) {
		field = judgeObject(field);
		op = judgeObject(op);
		condition = judgeObject(condition);
		JSONArray param = array(new Object[] {field, op, condition});
		array_shift(param);
		this.parseWhereExp("AND", field, op, condition, param);
		return this;
	}
	
	private void addOptionMulti(String logic, String field, Object value) {
		JSONObject multiObj = this.options.getJSONObject("multi");
		if (multiObj == null) {
			multiObj = new JSONObject();
			this.options.put("multi", multiObj);
		}
		JSONObject logicObj = multiObj.getJSONObject(logic);
		if (logicObj == null) {
			logicObj = new JSONObject();
			multiObj.put(logic, logicObj);
		}
		JSONArray fieldArray = logicObj.getJSONArray(field);
		if (fieldArray == null) {
			fieldArray = new JSONArray();
			logicObj.put(field, fieldArray);
		}
		fieldArray.add(value);
	}
	
	private JSONArray getOptionMultiField(String logic, String field) {
		JSONObject multiObj = this.options.getJSONObject("multi");
		if (multiObj == null) {
			multiObj = new JSONObject();
			this.options.put("multi", multiObj);
		}
		JSONObject logicObj = multiObj.getJSONObject(logic);
		if (logicObj == null) {
			logicObj = new JSONObject();
			multiObj.put(logic, logicObj);
		}
		return logicObj.getJSONArray(field);
	}
	
	protected void parseWhereExp(String logic, Object field, Object op, Object condition, JSONArray param) {
		logic = logic.toUpperCase();
		
		JSONObject where = null;
		if (is_string(field) && !empty(this.options.get("via")) && ((String)field).indexOf(".") < 0) {
			field = this.options.getString("via") + "." + (String)field;
		}
		if (is_string(field) && preg_match(".*(,|=|>|<|'|\"|\\(|\\s).*", (String)field)) {
			JSONArray value = array(new Object[]{"exp", field});
			where = new JSONObject();
			JSONArray arr = new JSONArray();
			arr.add(value);
			where.put("", arr);
			if (is_map(op)) {
				this.bind(op);
			}
		} else if (is_null(op) && is_null(condition)) {
			if (is_map(field)) {
				// 数组批量查询
				where = (JSONObject)field;
				for (String key : where.keySet()) {
					this.addOptionMulti(logic, key, where.get(key));
				}
			} else if (!empty(field) && is_string(field)) {
				// 字符串查询
				JSONArray value = array(new Object[]{"null", ""});
				where = new JSONObject();
				where.put((String)field, value);
				this.addOptionMulti(logic, (String)field, value);
			}
		} else if (is_array(op)) {
			// TODO:
			where = new JSONObject();
			where.put((String)field, param);
		} else if (in_array((String.valueOf(op)).toLowerCase(), new String[]{"null", "notnull", "not null"})) {
			JSONArray value = array(new Object[]{(String)op, ""});
			where = new JSONObject();
			where.put((String)field, param);
			this.addOptionMulti(logic, (String)field, value);
		} else if (is_null(condition)) {
			JSONArray value = array(new Object[]{"eq", op});
			// 字段相等查询
			where = new JSONObject();
			where.put((String)field, value);
			if (!"AND".equals(logic)) {
				this.addOptionMulti(logic, (String)field, value);
			}
		} else {
			JSONArray value = array(new Object[]{op, condition, param.size() > 2 ? param.get(2) : null});
			where = new JSONObject();
			where.put((String)field, value);
			if ("exp".equals(((String)op).toLowerCase()) && param.size() > 2 && is_map(param.get(2))) {
				// 参数绑定
				this.bind(param.get(2));
			}
            // 记录一个字段多次查询条件
			this.addOptionMulti(logic, (String)field, value);
		}
		if (!empty(where)) {
			JSONObject whereObj = this.options.getJSONObject("where");
			if (whereObj == null) {
				whereObj = new JSONObject();
				this.options.put("where", whereObj);
			}
			JSONObject logicObject = whereObj.getJSONObject(logic);
			if (logicObject == null) {
				logicObject = new JSONObject();
				whereObj.put(logic, logicObject);
			}

			if (is_string(field) && this.checkMultiField((String)field, logic)) {
				where.put((String)field, this.getOptionMultiField(logic, (String)field));
			} else if (is_map(field)) {
				JSONObject fieldObject = (JSONObject)field;
				for (String key : fieldObject.keySet()) {
					if (this.checkMultiField(key, logic)) {
						where.put(key, this.getOptionMultiField(logic, key));
					}
				}
			}
			
			whereObj.put(logic, array_merge(logicObject, where));
		}
	}
	
	private boolean checkMultiField(String field, String logic) {
		if (!this.options.containsKey("multi")) {
			return false;
		}
		JSONObject multiObj = this.options.getJSONObject("multi");
		if (!multiObj.containsKey(logic)) {
			return false;
		}
		JSONObject logicObj = multiObj.getJSONObject(logic);
		if (!logicObj.containsKey(field)) {
			return false;
		}
		JSONArray fieldArray = logicObj.getJSONArray(field);
		return fieldArray.size() > 1;
	}
	
	// 把主键值转换为查询条件 支持复合主键
	protected void parsePkWhere(Object data, JSONObject options) throws Exception {
		Object pk = this.getPk(options);
        // 获取当前数据表
        String table = is_array(options.get("table")) ? key(options.getJSONArray("table")) : options.getString("table");
        JSONObject aliasObj = options.getJSONObject("alias");
		if (aliasObj == null) {
			aliasObj = new JSONObject();
			options.put("alias", aliasObj);
		}
        String alias = null;
        if (!empty(aliasObj.get(table))) {
            alias = aliasObj.getString(table);
        }
        JSONObject where = new JSONObject();
        if (is_string(pk)) {
        	String pkStr = (String)pk;
            String key = !empty(alias) ? alias + "." + pkStr : pkStr;
            // 根据主键查询
            if (is_map(data)) {
            	JSONObject dataObj = (JSONObject)data;
            	where.put(key, dataObj.containsKey(pkStr) ? dataObj.get(pkStr) : array(new Object[] {"in", data}));
            } else if (is_array(data)) {
            	where.put(key, array(new Object[] {"in", data}));
            } else {
            	where.put(key, strpos(String.valueOf(data), ",") >= 0 ? array(new Object[] {"in", data}) : data);
            }
        } else if (is_array(pk) && !empty(data) && is_map(data)) {
        	JSONArray pkArr = (JSONArray)pk;
        	JSONObject dataObj = (JSONObject)data;
            // 根据复合主键查询
            for (int i = 0; i < pkArr.size(); i++) {
            	String key = pkArr.getString(i);
                if (dataObj.containsKey(key)) {
                    String attr = !empty(alias) ? alias + "." + key : key;
                    where.put(attr, dataObj.get(key));
                } else {
                    throw new Exception("miss complex primary data");
                }
            }
        }

        if (!empty(where)) {
			JSONObject whereObj = options.getJSONObject("where");
			if (whereObj == null) {
				whereObj = new JSONObject();
				options.put("where", whereObj);
			}
			if (whereObj.containsKey("AND")) {
				whereObj.put("AND", array_merge(whereObj.getJSONObject("AND"), where));
			} else {
				whereObj.put("AND", where);
			}
        }
	}
	
	public Query bind(Object key) {
		return bind(key, false, PDO.PARAM_STR);
	}
	
	public Query bind(Object key, Object value) {
		return bind(key, value, PDO.PARAM_STR);
	}
	
	public Query bind(Object key, Object value, int type) {
		return parseBind(judgeObject(key), judgeObject(value), type);
	}
	
	private Query parseBind(Object key, Object value, int type) {
		if (is_map(key)) {
			this.bind = array_merge(this.bind, (JSONObject)key);
		} else if (is_string(key)) {
			this.bind.put((String)key, array(new Object[]{value, type}));
		}
		return this;
	}
	
	public boolean isBind(String key) {
		return this.bind.containsKey(key);
	}
	
    public JSONObject getBind()
    {
        JSONObject bind = JSON.parseObject(this.bind.toString());
        this.bind = new JSONObject();
        return bind;
    }

	// 获取当前数据表字段类型
	public Object getFieldsType(JSONObject options) throws Exception {
		return this.getTableInfo(options.getString("table"), "type");
	}
	
	// 获取当前数据表绑定信息
	public JSONObject getFieldsBind(JSONObject options) throws Exception {
		Object types = this.getFieldsType(options);
		JSONObject bind = new JSONObject();
		if (!empty(types) && is_map(types)) {
			JSONObject typesObject = (JSONObject)types;
			for (String key : typesObject.keySet()) {
				String type = typesObject.getString(key);
				bind.put(key, this.getFieldBindType(type));
			}
		}
		return bind;
	}

	public Query order(Object field) {
		return order(field, null);
	}
	
    public Query order(Object field, String order) {
    	return parseOrder(judgeObject(field), order);
	}

	// 指定排序 order('id','desc') 或者 order(['id'=>'desc','create_time'=>'desc'])
    protected Query parseOrder(Object field, String order)
    {
        if (!empty(field)) {
            if (is_string(field)) {
                if (!empty(this.options.get("via"))) {
                    field = this.options.getString("via") + "." + field;
                }
                JSONObject fieldObj = new JSONObject();
                fieldObj.put((String)field, order);
                field = empty(order) ? field : fieldObj;
            } else if (!empty(this.options.get("via"))) {
            	JSONObject fieldObj = (JSONObject)field;
                for (String key : fieldObj.keySet()) {
                	Object val = fieldObj.get(key);
                    if (is_numeric(key)) {
                        fieldObj.put(key, this.options.getString("via") + "." + val);
                    } else {
                        fieldObj.put(this.options.getString("via") + "." + key, val);
                        fieldObj.remove(key);
                    }
                }
            }
            if (!this.options.containsKey("order")) {
                this.options.put("order", new JSONObject());
            }
            if (is_map(field)) {
                this.options.put("order", array_merge(this.options.getJSONObject("order"), (JSONObject)field));
            }
        }
        return this;
    }
    
    // 指定group查询
    public Query group(String group) {
    	this.options.put("group", group);
    	return this;
    }
    
    // 指定having查询
    public Query having(String having) {
    	this.options.put("having", having);
    	return this;
    }
    
    // 指定查询lock
    public Query lock(boolean lock) {
    	this.options.put("lock", lock);
    	this.options.put("master", true);
    	return this;
    }

    // 指定distinct查询
    public Query distinct(String distinct) {
    	this.options.put("distinct", distinct);
    	return this;
    }
    
    public Query union(Object union)
    {
    	return union(union, false);
    }
    
    public Query union(Object union, boolean all)
    {
    	JSONObject unionObj = this.options.getJSONObject("union");
    	if (unionObj == null) {
    		unionObj = new JSONObject();
    		this.options.put("union", unionObj);
    	}
    	unionObj.put("type", all ? "UNION ALL" : "UNION");
        if (is_map(union)) {
        	this.options.put("union", array_merge(unionObj, (JSONObject)union));
        }
        return this;
    }
    
    public Query limit(Object offset) {
    	return limit(offset, null);
    }
    
    public Query limit(Object offset, Integer length) {
        if (is_null(length) && strpos(String.valueOf(offset), ",") >= 0) {
            JSONArray offArray = explode(",", String.valueOf(offset));
            offset = offArray.getInteger(0);
            length = offArray.getInteger(1);
        }
        this.options.put("limit", offset + (!empty(length) ? "," + length : ""));
        return this;
    }

    // 查询注释
    public Query comment(String comment) {
    	this.options.put("comment", comment);
    	return this;
    }

	public JSONObject find() throws Exception {
		JSONObject options = this.parseExpress();
		//JSONArray pk = this.getPk(options);
        //System.out.println(pk.toString());
		options.put("limit", 1);
        // 生成查询SQL
        String sql = this.builder.select(options);
        //System.out.println(sql);
        // 获取参数绑定
        JSONObject bind = this.getBind();
    	// 获取实际执行的SQL语句
        String realSql = this.connection.getRealSql(sql, bind);
        //System.out.println(realSql);
        // 执行查询
        JSONArray resultArray = this.connection.query(realSql);
        if (resultArray.size() >= 1) {
        	return resultArray.getJSONObject(0);
        }
        return new JSONObject();
	}
	
	public JSONArray select() throws Exception {
		JSONObject options = this.parseExpress();
		//Object pk = this.getPk(options);
        //System.out.println(pk.toString());
        // 生成查询SQL
        String sql = this.builder.select(options);
        //System.out.println(sql);
        // 获取参数绑定
        JSONObject bind = this.getBind();
    	// 获取实际执行的SQL语句
        String realSql = this.connection.getRealSql(sql, bind);
        //System.out.println(realSql);
        // 执行查询
        return this.connection.query(realSql);
	}
	
	public int count() throws Exception {
		return count("*");
	}
	
	public int count(String field) throws Exception {
		JSONArray resultArray = this.field("COUNT(" + field + ") AS tp_count").limit(1).getPdo();
		if (resultArray.size() != 1) {
			throw new Exception("invalid sql result set size");
        }
		JSONObject result = resultArray.getJSONObject(0);
		return result.getIntValue("tp_count");
	}
	
    public JSONArray getPdo() throws Exception
    {
        // 分析查询表达式
    	JSONObject  options = this.parseExpress();
        // 生成查询SQL
    	String sql = this.builder.select(options);
        // 获取参数绑定
    	JSONObject bind = this.getBind();
        // 获取实际执行的SQL语句
        String realSql = this.connection.getRealSql(sql, bind);
        // 执行查询操作
        return this.connection.query(realSql);
    }

	public Object value(String field) throws Exception {
		return value(field, null);
	}
	
	public Object value(String field, Object def) throws Exception {
		JSONArray resultArray = this.field(field).limit(1).getPdo();
		if (resultArray.size() == 1) {
			JSONObject result = resultArray.getJSONObject(0);
			return result.get(field);
        }
		return def;
	}
	
	public Object query(String sql) throws Exception {
		return this.connection.query(sql);
	}
	
	public Query fetchSql(boolean fetch) {
		this.options.put("fetch_sql", fetch);
		return this;
	}
	
	public String getLastSql() {
		return this.connection.getLastSql();
	}
	
	public String getTable(String name) {
		String tableName = "";
        if (!empty(name) || empty(this.table)) {
            name = !empty(name) ? name : this.name;
            tableName = this.prefix;
            if (!empty(name)) {
                tableName += name;
            }
        } else {
            tableName = this.table;
        }
        return tableName;
	}
	
	public String getTable() {
		return getTable("");
	}
	
	private Object getConfig(String name) {
		return this.connection.getConfig(name);
	}
	
	private JSONObject parseExpress() {
		JSONObject options = JSON.parseObject(this.options.toString());
		// 获取数据表
		if (!options.containsKey("table")) {
			options.put("table", this.getTable());
		}
		
		if (!options.containsKey("where")) {
			options.put("where", new JSONObject());
		}

		if (!options.containsKey("field")) {
			options.put("field", "*");
        }
		
		if (!options.containsKey("data")) {
			options.put("data", new JSONObject());
		}

		if (!options.containsKey("strict")) {
			options.put("strict", this.getConfig("fields_strict"));
        }

		String [] names = {"master", "lock", "fetch_pdo", "fetch_sql", "distinct"};
		for (String name : names) {
			if (!options.containsKey(name)) {
				options.put(name, false);
			}
		}

		String [] names2 = {"join", "union", "group", "having", "limit", "order", "force", "comment"};
		for (String name : names2) {
			if (!options.containsKey(name)) {
				options.put(name, "");
			}
		}

		if (options.containsKey("page")) {
			JSONArray pageArray = options.getJSONArray("page");
			int page = pageArray.getInteger(0);
			int listRows = pageArray.getInteger(1);
			page = page > 0 ? page : 1;
			String limit = options.getString("limit");
			listRows = listRows > 0 ? listRows : (is_numeric(limit) ? Utils.parseIntValue(limit, 0) : 20);
			int offset = listRows * (page - 1);
			options.put("limit", offset + ',' + listRows);
        }
		
		//this.options = new JSONObject();
		return options;
	}
	
	// 将SQL语句中的__TABLE_NAME__字符串替换成带前缀的表名（小写）
	public String parseSqlTable(String sql) {
		if (sql.indexOf("__") > 0) {
			// TODO:
		}
		return sql;
	}
	
	public JSONObject getTableInfo(String tableName) throws Exception {
		if (empty(tableName)) {
			tableName = this.getTable();
		}
		if (tableName.indexOf(",") > 0) {
			// 多表不获取字段信息
			return null;
		} else {
			tableName = this.parseSqlTable(tableName);
		}

        // 修正子查询作为表名的问题
        if (tableName.indexOf(")") > 0) {
            return new JSONObject();
        }
        
        String [] tableNames = tableName.split(" ");
        String guid = tableNames[0];
        String db = (String)this.getConfig("database");
        String infoKey = db + "." + guid;
        if (!Query.info.containsKey(infoKey)) {
        	JSONObject info = this.connection.getFields(guid);
        	JSONArray fields = array_keys(info);
        	JSONObject bind = new JSONObject();
        	JSONObject type = new JSONObject();
        	JSONArray pk = new JSONArray();
        	for (String key : info.keySet()) {
        		JSONObject infoObj = info.getJSONObject(key);
        		type.put(key, infoObj.getString("type"));
        		bind.put(key, getFieldBindType(infoObj.getString("type")));
        		if (infoObj.getBoolean("primary") == true) {
        			pk.add(key);
        		}
        	}
        	Object pkValue = null;
        	if (!empty(pk)) {
        		pkValue = pk.size() > 1 ? pk : pk.get(0);
        	}
        	
        	JSONObject newInfo = new JSONObject();
        	newInfo.put("fields", fields);
        	newInfo.put("type", type);
        	newInfo.put("bind", bind);
        	newInfo.put("pk", pkValue);
        	Query.info.put(infoKey, newInfo);
        }
        return Query.info.getJSONObject(infoKey);
	}
	
	public Object getTableInfo(String tableName, String fetch) throws Exception {
		JSONObject tableInfo = getTableInfo(tableName);
		return tableInfo.get(fetch);
	}
	
	private int getFieldBindType(String type) {
		if (type.contains("int") || type.contains("double") || type.contains("float") || type.contains("decimal") || type.contains("real") || type.contains("numeric") || type.contains("serial") || type.contains("bit")) {
			return PDO.PARAM_INT;
		} else if (type.contains("bool")) {
			return PDO.PARAM_BOOL;
		} else {
			return PDO.PARAM_STR;
		}
	}
	
	private Object getPk(JSONObject options) throws Exception {
		Object pk = null;
        if (!empty(this.pk)) {
            pk = this.pk;
        } else {
            pk = this.getTableInfo(options.getString("table"), "pk");
        }
        return pk;
	}

	public int insert() throws Exception {
		return insert(new JSONObject(), false);
	}
	
	public int insert(JSONObject data) throws Exception {
		return insert(data, false);
	}
	
	public int insert(JSONObject data, boolean replace) throws Exception {
		data = this.allowField(data);
        // 分析查询表达式
    	JSONObject options = this.parseExpress();
        data = array_merge(options.getJSONObject("data"), data);
        // 生成SQL语句
        String sql = this.builder.insert(data, options, replace);
        // 获取参数绑定
    	JSONObject bind = this.getBind();
        // 获取实际执行的SQL语句
        String realSql = this.connection.getRealSql(sql, bind);
		return this.connection.execute(realSql);
	}

	public int insertAll(JSONArray dataSet) throws Exception {
        // 分析查询表达式
		JSONObject options = this.parseExpress();
        // 生成SQL语句
        String sql = this.builder.insertAll(dataSet, options);
        // 获取参数绑定
        JSONObject bind = this.getBind();
        // 获取实际执行的SQL语句
        String realSql = this.connection.getRealSql(sql, bind);
		return this.connection.execute(realSql);
	}
	
	private JSONObject allowField(JSONObject data) throws Exception {
        JSONArray field = (JSONArray)this.getTableInfo("", "fields");
        // 检测字段
        if (!empty(field) && is_map(data)) {
        	JSONObject dataObj = (JSONObject)data;
            for (String key : dataObj.keySet()) {
                if (!in_array(key, field)) {
                	dataObj.remove(key);
                }
            }
        }                                   
        return data;
	}
	
	public int update() throws Exception {
		return update(new JSONObject());
	}
	public int update(JSONObject data) throws Exception {
		data = this.allowField(data);
        // 分析查询表达式
    	JSONObject options = this.parseExpress();
        data = array_merge(options.getJSONObject("data"), data);
		Object pk = this.getPk(options);
        if (empty(options.get("where"))) {
        	JSONObject where = new JSONObject();
            // 如果存在主键数据 则自动作为更新条件
            if (is_string(pk) && data.containsKey(pk)) {
                where.put((String)pk, data.get(pk));
                data.remove(pk);
            } else if (is_array(pk)) {
            	JSONArray pkArray = (JSONArray)pk;
                // 增加复合主键支持
                for (int i = 0; i < pkArray.size(); i++) {
                	String field = pkArray.getString(i);
                    if (data.containsKey(field)) {
                    	where.put(field, data.get(field));
                    } else {
                        // 如果缺少复合主键数据则不执行
                        throw new Exception("miss complex primary data");
                    }
                    data.remove(field);
                }
            }
            if (where.isEmpty()) {
                // 如果没有任何更新条件则不执行
                throw new Exception("miss update condition");
            } else {
    			JSONObject whereObj = options.getJSONObject("where");
    			if (whereObj == null) {
    				whereObj = new JSONObject();
    				options.put("where", whereObj);
    			}
    			whereObj.put("AND", where);
            }
        }

        // 生成SQL语句
        String sql = this.builder.update(data, options);
        // 获取参数绑定
    	JSONObject bind = this.getBind();
        // 获取实际执行的SQL语句
        String realSql = this.connection.getRealSql(sql, bind);
		return this.connection.execute(realSql);
	}
	
	// 设置记录的某个字段值,支持使用数据库字段和方法
    public int setField(Object field, Object value) throws Exception
    {
    	JSONObject data = null;
        if (is_map(field)) {
            data = (JSONObject)field;
        } else {
        	data = new JSONObject();
        	data.put((String)field, value);
        }
        return this.update(data);
    }

    // 字段值增长
    public int setInc(String field, int step) throws Exception {
    	return this.setField(field, array(new String[] { "exp", field + "+" + step }));
    }

    // 字段值减少
    public int setDec(String field, int step) throws Exception {
    	return this.setField(field, array(new String[] { "exp", field + "-" + step }));
    }
    
    // 设置数据
    public Query data(Object field, Object value) {
    	if (is_map(field)) {
    		if (this.options.containsKey("data")) {
    			this.options.put("data", array_merge(this.options.getJSONObject("data"), (JSONObject)field));
    		} else {
    			this.options.put("data", (JSONObject)field);
    		}
    	} else if (is_string(field)) {
    		JSONObject dataObj = this.options.getJSONObject("data");
    		if (dataObj == null) {
    			dataObj = new JSONObject();
    			this.options.put("data", dataObj);
    		}
    		dataObj.put((String)field, value);
    	}
    	return this;
    }
    
    // 字段值增加
    public Query inc(Object field, int step) {
    	JSONArray fields = is_string(field) ? explode(",", (String)field) : (JSONArray)field;
    	for (int i = 0; i < fields.size(); i++) {
    		String key = fields.getString(i);
    		this.data(key, new String[] {"exp", key + "+" + step});
    	}
    	return this;
    }
    
    // 字段值减少
    public Query dec(Object field, int step) {
    	JSONArray fields = is_string(field) ? explode(",", (String)field) : (JSONArray)field;
    	for (int i = 0; i < fields.size(); i++) {
    		String key = fields.getString(i);
    		this.data(key, new String[] {"exp", key + "-" + step});
    	}
    	return this;
    }
    
    // 使用表达式设置数据
    public Query exp(String field, String value) {
    	this.data(field, new String[] {"exp", value});
    	return this;
    }
	
	public int delete() throws Exception {
		return delete(null);
	}
	
	public int delete(Object data) throws Exception {
        // 分析查询表达式
    	JSONObject options = this.parseExpress();
    	
		if (!is_null(data)) {
            // AR模式分析主键条件
            this.parsePkWhere(data, options);
        }
		
        // 生成SQL语句
        String sql = this.builder.delete(options);
        // 获取参数绑定
    	JSONObject bind = this.getBind();
        // 获取实际执行的SQL语句
        String realSql = this.connection.getRealSql(sql, bind);
		return this.connection.execute(realSql);
	}
	
	public void startTransaction() throws Exception {
		this.connection.startTransaction();
	}
	
	public void commit() throws Exception {
		this.connection.commit();
	}
	
	public void rollback() throws Exception {
		this.connection.rollback();
	}
}
