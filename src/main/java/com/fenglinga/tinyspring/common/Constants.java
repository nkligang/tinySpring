package com.fenglinga.tinyspring.common;

import com.alibaba.fastjson.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Constants
{
    public static String AppAssetsPath;
    public static JSONObject Config = new JSONObject();
    public final static Logger LOGGER = LoggerFactory.getLogger(Constants.class);
    public static String REMOTE_DISCONNECT_MESSAGE = "远程主机强迫关闭了一个现有的连接。";
    public static String REMOTE_DISCONNECT_MESSAGE_EN = "An existing connection was forcibly closed by the remote host";
    public static String REMOTE_CONNECTION_INTERRUPT_MESSAGE = "您的主机中的软件中止了一个已建立的连接。";
    
    public static void Init() 
    {
    	String userDir = System.getProperty("user.dir");
    	System.out.println("User dir: " + userDir);
    	AppAssetsPath = userDir + "/assets/";
    	System.out.println("Application assets path: " + AppAssetsPath);
    	String propertiesString = Utils.LoadStringFromFile(AppAssetsPath + "application.properties");
    	String [] properties = propertiesString.split("\r\n");
    	for (String property : properties) {
    		int idx = property.indexOf('=');
    		if (idx > 0) {
    			Config.put(property.substring(0, idx), property.substring(idx + 1, property.length()));
    		} else {
    			System.out.println("invalid application.properties:" + property);
    		}
    	}
    }
    
    public static JSONObject getConfig(String group) {
    	JSONObject result = new JSONObject();
    	for (String key : Config.keySet()) {
    		if (key.startsWith(group + '.')) {
    			result.put(key.substring(group.length() + 1), Config.get(key));
    		}
    	}
    	return result;
    }
}
