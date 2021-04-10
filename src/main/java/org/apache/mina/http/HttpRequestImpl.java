/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.mina.http;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.mina.http.api.HttpMethod;
import org.apache.mina.http.api.HttpRequest;
import org.apache.mina.http.api.HttpVersion;

public class HttpRequestImpl implements HttpRequest {
	
    private final HttpVersion version;

    private final HttpMethod method;

    private final String requestedPath;
    
    private final String queryString;

    private final Map<String, String> headers;

    public HttpRequestImpl(HttpVersion version, HttpMethod method, String requestedPath, String queryString, Map<String, String> headers) {
        this.version = version;
        this.method = method;
        this.requestedPath = requestedPath;
        this.queryString = queryString;
        this.headers = headers;//Collections.unmodifiableMap(headers);
    }

    public HttpVersion getProtocolVersion() {
        return version;
    }

    public String getContentType() {
        return headers.get("content-type");
    }

    public boolean isKeepAlive() {
        // TODO Auto-generated method stub
        return false;
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    public boolean containsHeader(String name) {
        return headers.containsKey(name);
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public boolean containsParameter(String name) {
    	Matcher m = parameterPattern(name);
    	return m.find();
    }

    public String getParameter(String name) {
    	Matcher m = parameterPattern(name);
    	if (m.find()) {
    		return m.group(1);
    	} else {
    		return null;
    	}
    }
    
    protected Matcher parameterPattern(String name) {
    	return Pattern.compile("[&]"+name+"=([^&]*)").matcher("&"+queryString);
    }

    public Map<String, List<String>> getParameters() {
    	Map<String, List<String>> parameters = new HashMap<String, List<String>>();
        String[] params = queryString.split("&");
        if (params.length == 1 && params[0].length() == 0) {
        	return parameters;
        }
        for (int i = 0; i < params.length; i++) {
			String[] param = params[i].split("=");
			String name = param[0];
			String value = param.length == 2 ? param[1] : "";
			if (!parameters.containsKey(name)) {
				parameters.put(name, new ArrayList<String>());
			}
			parameters.get(name).add(value);
		}
        return parameters;
    }
    
    public String getQueryString() {
    	return queryString;
    }

    public HttpMethod getMethod() {
        return method;
    }
    
    public String getRequestPath() {
    	return requestedPath;
    }

    public String toString() {
    	StringBuilder rsb = new StringBuilder();
        rsb.append("[URL]\n");
    	rsb.append(method).append(" ")
    	   .append(requestedPath).append(" ")
    	   .append(version).append("\n");

        rsb.append("[Request Headers]\n");
        for (String key : headers.keySet()) {
            String value = headers.get(key);
            rsb.append(key).append(": ").append(value).append("\n");
        }

        Map<String, List<String>> parameters = getParameters();
        if (parameters.size() > 0) {
	        rsb.append("[Query String Parameters]\n");
	        for (String key : parameters.keySet()) {
	        	Collection<String> values = parameters.get(key);
	        	for (String value : values) { 
	        		rsb.append(key).append(": ").append(value).append("\n");
	        	}
	        }
        }
        
        return rsb.toString();
    }
}
