/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tiny.spring.framework.websocket;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

import com.tiny.spring.common.Utils;

/**
 * Encodes incoming buffers in a manner that makes the receiving client type transparent to the 
 * encoders further up in the filter chain. If the receiving client is a native client then
 * the buffer contents are simply passed through. If the receiving client is a websocket, it will encode
 * the buffer contents in to WebSocket DataFrame before passing it along the filter chain.
 * 
 * Note: you must wrap the IoBuffer you want to send around a WebSocketCodecPacket instance.
 * 
 * @author DHRUV CHOPRA
 */
public class WebSocketEncoder extends ProtocolEncoderAdapter{

    @Override
    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
        boolean isHandshakeResponse = message instanceof WebSocketHandShakeResponse;
        boolean isDataFramePacket = message instanceof WebSocketCodecPacket;
        boolean isRemoteWebSocket = session.containsAttribute(WebSocketUtils.SessionAttribute) && (true==(Boolean)session.getAttribute(WebSocketUtils.SessionAttribute));
        IoBuffer resultBuffer;
        if(isHandshakeResponse){
            WebSocketHandShakeResponse response = (WebSocketHandShakeResponse)message;
            resultBuffer = WebSocketEncoder.buildWSResponseBuffer(response);
        }
        else if(isDataFramePacket){
            WebSocketCodecPacket packet = (WebSocketCodecPacket)message;
            resultBuffer = isRemoteWebSocket ? WebSocketEncoder.buildWSDataFrameBuffer(packet.getPacket(), packet.getPacketType(), packet.hasMask()) : packet.getPacket();
        }
        else{
            throw (new Exception("message not a websocket type"));
        }
        
        out.write(resultBuffer);
    }
    
    // Web Socket handshake response go as a plain string.
    private static IoBuffer buildWSResponseBuffer(WebSocketHandShakeResponse response) {                
        IoBuffer buffer = IoBuffer.allocate(response.getResponse().getBytes().length, false);
        buffer.setAutoExpand(true);
        buffer.put(response.getResponse().getBytes());
        buffer.flip();
        return buffer;
    }
    
    // Encode the in buffer according to the Section 5.2. RFC 6455
    private static IoBuffer buildWSDataFrameBuffer(IoBuffer buf, int type, boolean hasMask) {
        int dataLength = buf.limit();
        IoBuffer buffer = IoBuffer.allocate(dataLength + 9, false);
        buffer.setAutoExpand(true);
    	buffer.put((byte) (0x80 | (0x0F & type)));
        if(dataLength <= 125){
            byte capacity = (byte)(dataLength);
            buffer.put(hasMask ? (byte)(126|0x80) : capacity);
        } else if (dataLength <= 65535){
            buffer.put(hasMask ? (byte)(126|0x80) : (byte)126);
            buffer.put((byte)(dataLength >> 8 & 0xFF));
            buffer.put((byte)(dataLength & 0xFF));
        } else {
            buffer.put(hasMask ? (byte)(127|0x80) : (byte)127);
            for (int i = 0; i < 8; i++) {
                buffer.put((byte)(dataLength >> (8 - i - 1) * 8 & 0xFF));
            }
        }
        if (hasMask) {
	        byte[] mask = new byte[4];
	        for (int i = 0; i < 4; i++) {
	        	mask[i] = (byte)Utils.getRandomValue();
	        	buffer.put(mask[i]);
	        }
	        for (int i = 0; i < dataLength; i++) {
	            byte maskedByte = buf.get();
	            buffer.put((byte) (maskedByte ^ mask[i % 4]));
	        }
        } else {
        	buffer.put(buf);
        }
        buffer.flip();
        return buffer;
    }
    
}
