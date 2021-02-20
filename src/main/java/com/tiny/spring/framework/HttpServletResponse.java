package com.tiny.spring.framework;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.HashMap;

import org.apache.mina.core.buffer.IoBuffer;

public class HttpServletResponse {
	private HashMap<String, String> headers = new HashMap<String, String>();
	private ByteArrayOutputStream mOutputStream = new ByteArrayOutputStream();
	
	public HttpServletResponse() {
	}
	
	public HashMap<String, String> getHeaders() { return headers; }
	
	public void setHeader(String name, String value) {
		headers.put(name, value);
	}
	
	public IoBuffer getBuffer() {
		return IoBuffer.wrap(mOutputStream.toByteArray());
	}
	
	public OutputStream getOutputStream() {
		return mOutputStream;
	}
}
