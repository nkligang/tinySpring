package com.tiny.spring.framework;

import java.lang.reflect.Method;
import java.util.HashMap;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.http.api.HttpRequest;

import com.alibaba.fastjson.JSONObject;

public class Controller {
	protected HttpRequest request = null;
	protected IoSession session = null;
	protected HashMap<String, Object> parameters = null;
	
	public Controller() {
	}
	
	public void setRequest(HttpRequest request) {
		this.request = request;
	}
	
	public void setSession(IoSession session) {
		this.session = session;
	}
	
	public void setParameters(HashMap<String, Object> parameters) {
		this.parameters = parameters;
	}
	
	public Object getParameter(String key) {
		return parameters.get(key);
	}
	
	public String getStringParameter(String key) {
		return (String)parameters.get(key);
	}
	
	protected JSONObject onException(Exception e) {
		return new JSONObject();
	}
	
	protected JSONObject onRequest(Method method, String requestPath) throws Exception {
		return new JSONObject();
	}
}
