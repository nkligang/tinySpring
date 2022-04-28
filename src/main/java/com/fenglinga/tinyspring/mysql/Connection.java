package com.fenglinga.tinyspring.mysql;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fenglinga.tinyspring.common.Constants;
import com.fenglinga.tinyspring.common.Utils;

public class Connection extends BaseObject {
    private java.sql.Connection mConnection = null;
    private String queryStr = "";
    private String lastTransaction = null;

    private JSONObject config = new JSONObject();
    
    public Connection(JSONObject config) {
        this.config.put("type", "");
        this.config.put("hostname", "");
        this.config.put("database", "");
        this.config.put("username", "");
        this.config.put("password", "");
        this.config.put("hostport", "");
        this.config.put("dsn", "");
        this.config.put("params", new JSONArray());
        this.config.put("charset", "utf8");
        this.config.put("prefix", "");
        this.config.put("debug", false);
        this.config.put("deploy", 0);
        this.config.put("rw_separate", false);
        this.config.put("master_num", 1);
        this.config.put("slave_no", "");
        this.config.put("fields_strict", true);
        this.config.put("result_type", "assoc");
        this.config.put("resultset_type", "array");
        this.config.put("auto_timestamp", false);
        this.config.put("datetime_format", "Y-m-d H:i:s");
        this.config.put("sql_explain", false);
        this.config.put("builder", "");
        this.config.put("break_reconnect", false);
        this.config.put("show-sql", false);
        
        if (!empty(config)) {
            this.config = array_merge(this.config, config);
        }
    }
    
    public void initConnect(boolean master) throws Exception {
        connect();
    }
    
    public void connect() throws Exception {
        connect(new JSONObject(), 0, false);
    }
    
    public void connect(JSONObject config, int linkNum, boolean autoConnection) throws Exception {
        if (mConnection != null && !mConnection.isClosed()) {
            return;
        }
        if (empty(config)) {
            config = this.config;
        } else {
            config = array_merge(this.config, config);
        }
        if (empty(config.getString("dsn"))) {
            config.put("dsn", this.parseDsn(config));
            
            try {
                // The newInstance() call is a work around for some
                // broken Java implementations
                Class.forName("com.mysql.jdbc.Driver").newInstance();
            } catch (Exception ex) {
                // handle the error
                throw ex;
            }
            try {
                // get connection
                mConnection = DriverManager.getConnection(config.getString("dsn"));
                if (mConnection == null) {
                    Constants.LOGGER.error("connection is null");
                }
            } catch (SQLException ex) {
                // handle any errors
                throw ex;
            }
        }
        Constants.LOGGER.info(mConnection.toString() + ":OPEN");
    }
    
    public JSONArray query(String sql) throws Exception {
        this.initConnect(false);
        if (this.mConnection == null) {
            return null;
        }

        //记录SQL
        this.queryStr = sql;
        
        long s = Utils.getMSTime();
        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = mConnection.createStatement(
                    ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_UPDATABLE);
            
            rs = stmt.executeQuery(sql);
        } catch (SQLException ex){
            throw ex;
        } finally {
            if (this.config.getBooleanValue("show-sql")) {
                Constants.LOGGER.info(mConnection.toString() + "[" + (Utils.getMSTime() - s) + "ms]:" + sql);
            }
            // it is a good idea to release
            // resources in a finally{} block
            // in reverse-order of their creation
            // if they are no-longer needed
            if (rs == null) {
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (SQLException sqlEx) {
                    }
    
                    stmt = null;
                }
            }
        }
        
        JSONArray result = new JSONArray();
        if (rs != null) {
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            while(rs.next()) {
                JSONObject obj = new JSONObject();
                for (int i = 1; i <= columnCount; i++) {
                    Object v = rs.getObject(i);
                    if (v instanceof java.sql.Timestamp) {
                        java.sql.Timestamp timestamp = (java.sql.Timestamp)v;
                        obj.put(rsmd.getColumnName(i), BaseObject.date("yyyy-MM-dd HH:mm:ss", timestamp.getTime()/1000));
                    } else {
                        obj.put(rsmd.getColumnName(i), v);
                    }
                }
                result.add(obj);
            }
            rs.getStatement().close();
            rs.close();
        }
        return result;        
    }
    
    public int execute(String sql) throws Exception {
        this.initConnect(false);
        if (this.mConnection == null) {
            return -1;
        }

        //记录SQL
        this.queryStr = sql;
        
        long s = Utils.getMSTime();
        Statement stmt = null;
        ResultSet rs = null;
        boolean result = false;
        int id = -1;

        try {
            stmt = mConnection.createStatement(
                    ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_UPDATABLE);
            
            if (sql.startsWith("UPDATE") || sql.startsWith("DELETE")) {
                id = stmt.executeUpdate(sql);
                result = false;
            } else {
                result = stmt.execute(sql,Statement.RETURN_GENERATED_KEYS);
                rs = stmt.getGeneratedKeys();
                if(rs.next()) {
                    id = rs.getInt(1);
                }
            }
        } catch (SQLException ex){
            throw ex;
        } finally {
            if (this.config.getBooleanValue("show-sql")) {
                Constants.LOGGER.info(mConnection.toString() + "[" + (Utils.getMSTime() - s) + "ms]:" + sql);
            }
            // it is a good idea to release
            // resources in a finally{} block
            // in reverse-order of their creation
            // if they are no-longer needed
            if (result == false) {
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (SQLException sqlEx) {
                    }
    
                    stmt = null;
                }
            }
        }
        
        return id;
    }
    
    public void close() throws SQLException {
        if (this.mConnection == null) {
            return;
        }
        Constants.LOGGER.info(mConnection.toString() + ":CLOSE");
        this.mConnection.close();
    }
    
    public String parseDsn(JSONObject config) {
        StringBuilder dsn = new StringBuilder();
        dsn.append("jdbc:mysql://").append(config.getString("hostname"));
        if (!empty(config.getString("hostport"))) {
            dsn.append(":").append(config.getString("hostport"));
        }
        dsn.append("/").append(config.getString("database")).append("?user=").append(config.getString("username")).append("&password=").append(config.getString("password"));
        if (!empty(config.getString("charset"))) {
            dsn.append("&useUnicode=true").append("&characterEncoding=").append(config.getString("charset"));
        }
        return dsn.toString();
    }
    
    public Object getConfig(String name) {
        return !empty(name) ? this.config.get(name) : this.config;
    }
    
    public JSONObject getFields(String tableName) throws Exception {
        initConnect(false);
        String sql = "SHOW COLUMNS FROM " + tableName;
        JSONObject fields = new JSONObject();
        JSONArray result = query(sql);
        for (int i = 0; i < result.size(); i++) {
            JSONObject obj = result.getJSONObject(i);
            obj = array_change_key_case(obj, true);
            JSONObject info = new JSONObject();
            info.put("name", obj.getString("field"));
            info.put("type", obj.getString("type"));
            info.put("notnull", obj.getString("null").equals("NO"));
            info.put("default", obj.getString("default"));
            info.put("primary", obj.getString("key").toLowerCase().equals("pri"));
            info.put("autoinc", obj.getString("extra").toLowerCase().equals("auto_increment"));
            fields.put(obj.getString("field"), info);
        }
        return fields;
    }
    
    public String quote(Object str) throws Exception {
        return quote(str, false);
    }
    
    public String quote(Object str, boolean master) throws Exception {
        return "'" + String.valueOf(str) + "'";
    }
    
    public String getRealSql(String sql, JSONObject bind) throws Exception
    {        
        for (String key : bind.keySet()) {
            Object val = bind.get(key);
            Object value = is_array(val) ? ((JSONArray)val).get(0) : val;
            int type = is_array(val) ? ((JSONArray)val).getInteger(1) : PDO.PARAM_STR;
            if (PDO.PARAM_STR == type) {
                value = this.quote(value);
            } else if (PDO.PARAM_INT == type) {
                // TODO:
            }
            // 判断占位符
            sql = is_numeric(key) ?
            substr_replace(sql, (String)value, strpos(sql, "?"), 1) :
            str_replace(
                new String[] {":" + key + ")", ":" + key + ",", ":" + key + " "},
                new String[] {value + ")", value + ",", value + " "},
                sql + " ");
        }
        return rtrim(sql);
    }
    
    public String getLastSql() {
        return queryStr;
    }
    
    // 修正COMMIT之后不更新数据库的问题
    public void startTransaction() throws Exception {
        initConnect(false);
        //mConnection.setAutoCommit(false);
        //System.out.println(mConnection.toString() + ":BEGIN;");
        execute("BEGIN;");
        lastTransaction = "BEGIN;";
    }
    
    public void commit() throws Exception {
        if (lastTransaction == null || !lastTransaction.equals("BEGIN;")) {
        	return;
        }
        initConnect(false);
        //mConnection.commit();
        //System.out.println(mConnection.toString() + ":COMMIT;");
        execute("COMMIT;");
        lastTransaction = "COMMIT;";
    }
    
    public void rollback() throws Exception {
        if (lastTransaction == null || !lastTransaction.equals("BEGIN;")) {
        	return;
        }
        initConnect(false);
        //mConnection.rollback();
        //System.out.println(mConnection.toString() + ":ROLLBACK;");
        execute("ROLLBACK;");
        lastTransaction = "ROLLBACK;";
    }
}
