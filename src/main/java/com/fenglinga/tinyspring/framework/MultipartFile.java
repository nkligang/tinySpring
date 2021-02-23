package com.fenglinga.tinyspring.framework;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.mina.core.buffer.IoBuffer;

import com.fenglinga.tinyspring.framework.HttpServerHandler.MultipartFormData;

public class MultipartFile {    
    private IoBuffer mBuffer;
    private String mOriginalFilename;
    private String mContentType;
    private ByteArrayInputStream mInputStream;
    
    public MultipartFile(String originalFilename, String contentType, MultipartFormData formData) {
        mOriginalFilename = originalFilename;
        if (contentType.equals("image/png") && !originalFilename.endsWith(".png")) {
            mOriginalFilename += ".png";
        } else if (contentType.equals("image/jpeg") && !originalFilename.endsWith(".jpg")) {
            mOriginalFilename += ".jpg";
        }
        mContentType = contentType;
        mBuffer = IoBuffer.allocate(formData.content.remaining());
        mBuffer.setAutoExpand(false);
        mBuffer.put(formData.content.array(), formData.content.position(), formData.content.remaining());
        mBuffer.flip();
        mInputStream = new ByteArrayInputStream(mBuffer.array());
    }
    
    public String getContentType() {
        return mContentType;
    }

    public String getOriginalFilename() {
        return mOriginalFilename;
    }
    
    public byte[] getBytes() {
        return mBuffer.array();
    }
    
    public InputStream getInputStream() {
        return mInputStream;
    }
}
