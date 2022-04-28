package com.fenglinga.tinyspring.framework;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.HashMap;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.http.api.HttpStatus;
import org.apache.mina.http.api.HttpVersion;

public class HttpServletResponse {
    private HashMap<String, String> headers = new HashMap<String, String>();
    private ByteArrayOutputStream mOutputStream = new ByteArrayOutputStream();
    private HttpVersion version = HttpVersion.HTTP_1_1;
    private HttpStatus status = HttpStatus.SUCCESS_OK;

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
    
    public void setStatus(HttpStatus hs) {
    	status = hs;
    }

    public HttpStatus getStatus() {
        return status;
    }
    
    public void setProtocolVersion(HttpVersion hv) {
    	version = hv;
    }

    public HttpVersion getProtocolVersion() {
        return version;
    }
}
