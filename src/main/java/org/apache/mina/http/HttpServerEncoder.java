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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.http.api.HttpEndOfContent;
import org.apache.mina.http.api.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServerEncoder implements ProtocolEncoder {
    private static final Logger LOG = LoggerFactory.getLogger(HttpServerCodec.class);
    public static boolean LOG_ENABLED = false;
    private static final CharsetEncoder ENCODER = Charset.forName("UTF-8").newEncoder();

    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
    	//LOG.debug("encode {}", message.getClass().getCanonicalName());
        if (message instanceof HttpResponse) {
            HttpResponse msg = (HttpResponse) message;
            if (LOG_ENABLED) LOG.debug("Response Header: {}", message.getClass());
            StringBuilder sb = new StringBuilder(msg.getStatus().line());

            for (Map.Entry<String, String> header : msg.getHeaders().entrySet()) {
                sb.append(header.getKey());
                sb.append(": ");
                sb.append(header.getValue());
                sb.append("\r\n");
            }
            sb.append("\r\n");
            // Java 6 >> byte[] bytes = sb.toString().getBytes(Charset.forName("UTF-8"));
            // byte[] bytes = sb.toString().getBytes();
            // out.write(ByteBuffer.wrap(bytes));
            IoBuffer buf = IoBuffer.allocate(sb.length()).setAutoExpand(true);
            if (LOG_ENABLED) LOG.debug("[Response Headers]\r\n" + sb.toString().trim());
            buf.putString(sb.toString(), ENCODER);
            buf.flip();
            out.write(buf);
        } else if (message instanceof ByteBuffer) {
        	if (LOG_ENABLED) LOG.debug("Response Body: {}", message);
        	ByteBuffer buffer = (ByteBuffer) message;
        	IoBuffer buf = IoBuffer.allocate(buffer.capacity()).setAutoExpand(true);
        	String bodyType = (String)session.getAttribute("BodyType");
        	if (bodyType == null || bodyType.equals("String")) {
	        	String bodyString = new String(buffer.array(), "UTF-8");
	        	if (bodyString.length() > 4096) {
	        		if (LOG_ENABLED) LOG.debug("[Response Body]\r\n" + bodyString.trim().substring(0, 4096) + "...");
	        	} else {
	        		if (LOG_ENABLED) LOG.debug("[Response Body]\r\n" + bodyString.trim());
	        	}
        	}
        	buf.put(buffer.array());
        	buf.flip();
        	out.write(buf);
        } else if (message instanceof HttpEndOfContent) {
        	if (LOG_ENABLED) LOG.debug("Response End: {}", message);
            // end of HTTP content
            // keep alive ?
        }

    }

    public void dispose(IoSession session) throws Exception {
        // TODO Auto-generated method stub
    }
}
