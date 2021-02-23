/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenglinga.tinyspring.framework.websocket;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.http.HttpRequestImpl;
import org.apache.mina.http.api.DefaultHttpResponse;

/**
 * Decodes incoming buffers in a manner that makes the sender transparent to the 
 * decoders further up in the filter chain. If the sender is a native client then
 * the buffer is simply passed through. If the sender is a websocket, it will extract
 * the content out from the dataframe and parse it before passing it along the filter
 * chain.
 * 
 * @author DHRUV CHOPRA
 */
public class WebSocketDecoder extends CumulativeProtocolDecoder {
    private boolean mIsServer = true;
    
    public WebSocketDecoder() {
        mIsServer = true;
    }
    
    public WebSocketDecoder(boolean isServer) {
        mIsServer = isServer;
    }
    
    @Override
    protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {        
        IoBuffer resultBuffer;
        if(!session.containsAttribute(WebSocketUtils.SessionAttribute)){
            // first message on a new connection. see if its from a websocket or a 
            // native socket.
            if(mIsServer && tryWebSockeHandShake(session, in, out)) {
                // websocket handshake was successful. Don't write anything to output
                // as we want to abstract the handshake request message from the handler.
                in.position(in.limit());
                
                if (session.containsAttribute(WebSocketUtils.SessionHttpRequestRunnable)) {
                    Runnable command = (Runnable)session.getAttribute(WebSocketUtils.SessionHttpRequestRunnable);
                    if (command != null) {
                        command.run();
                    }
                }
                return true;
            } else if(!mIsServer && tryWebSockeHandShakeResponse(session, in, out)) {
                // websocket handshake was successful. Don't write anything to output
                // as we want to abstract the handshake response message from the handler.
                in.position(in.limit());
                
                if (session.containsAttribute(WebSocketUtils.SessionHttpResponseRunnable)) {
                    Runnable command = (Runnable)session.getAttribute(WebSocketUtils.SessionHttpResponseRunnable);
                    if (command != null) {
                        command.run();
                    }
                }
                return true;
            }
            else{
                // message is from a native socket. Simply wrap and pass through.
                resultBuffer = IoBuffer.wrap(in.array(), 0, in.limit());
                in.position(in.limit());
                session.setAttribute(WebSocketUtils.SessionAttribute, false);
            }
        }
        else if(session.containsAttribute(WebSocketUtils.SessionAttribute) && true==(Boolean)session.getAttribute(WebSocketUtils.SessionAttribute)){            
            // there is incoming data from the websocket. Decode and send to handler or next filter.     
            int startPos = in.position();
            WebSocketCodecPacket packet = WebSocketDecoder.buildWSDataBuffer(in, session);
            if(packet == null){
                // There was not enough data in the buffer to parse. Reset the in buffer
                // position and wait for more data before trying again.
                in.position(startPos);
                return false;
            }
            if (packet.getPacket().remaining() > 0) {
                out.write(packet);
            }
            return true;
        }
        else{
            // session is known to be from a native socket. So
            // simply wrap and pass through.
            resultBuffer = IoBuffer.wrap(in.array(), 0, in.limit());    
            in.position(in.limit());
        }                        
        out.write(resultBuffer);        
        return true;
    }

    /**
    *   Try parsing the message as a websocket handshake request. If it is such
    *   a request, then send the corresponding handshake response (as in Section 4.2.2 RFC 6455).
    */
    private boolean tryWebSockeHandShake(IoSession session, IoBuffer in, ProtocolDecoderOutput out) {
        
        try{
            HttpRequestImpl request = WebSocketUtils.parseHttpRequestHead(in);
            String socketKey = request.getHeader("sec-websocket-key");
            if(socketKey.length() <= 0){
                return false;
            }
            session.setAttribute(WebSocketUtils.SessionHttpRequest, request);
            String challengeAccept = WebSocketUtils.getWebSocketKeyChallengeResponse(socketKey);            
            WebSocketHandShakeResponse wsResponse = WebSocketUtils.buildWSHandshakeResponse(challengeAccept);
            session.setAttribute(WebSocketUtils.SessionAttribute, true);
            session.write(wsResponse);
            return true;
        }
        catch(Exception e){
            // input is not a websocket handshake request.
            return false;
        }        
    }
    
    private boolean tryWebSockeHandShakeResponse(IoSession session, IoBuffer in, ProtocolDecoderOutput out) {
        
        try{
            DefaultHttpResponse response = WebSocketUtils.parseHttpReponseHead(in);
            String socketKey = response.getHeader("sec-websocket-accept");
            if(socketKey.length() <= 0){
                return false;
            }
            session.setAttribute(WebSocketUtils.SessionHttpResponse, response);
            session.setAttribute(WebSocketUtils.SessionAttribute, true);
            return true;
        }
        catch(Exception e){
            // input is not a websocket handshake request.
            return false;
        }        
    }
    
    // Decode the in buffer according to the Section 5.2. RFC 6455
    // If there are multiple websocket dataframes in the buffer, this will parse
    // all and return one complete decoded buffer.
    private static WebSocketCodecPacket buildWSDataBuffer(IoBuffer in, IoSession session) {

        boolean bBinary = true;
        WebSocketCodecPacket packet = null;
        IoBuffer resultBuffer = null;
        do{
            byte frameInfo = in.get();            
            byte opCode = (byte) (frameInfo & 0x0f);
            if (opCode == 8) {
                // opCode 8 means close. See RFC 6455 Section 5.2
                // return what ever is parsed till now.
                session.close(true);
                return packet;
            } else if (opCode == 1) {
                bBinary = false;
            }
            
            byte secByte = in.get();
            int dataLength = 0;
            byte payload = (byte) (secByte & 0x7f);
            if (payload == 126) {
                dataLength = in.getUnsignedShort();
            } else if (payload == 127) {
                dataLength = (int)in.getLong();
            } else {
                dataLength = payload;
            }
            
            boolean hasMask = (secByte & 0x80) > 0;
            if (hasMask) {
                // Validate if we have enough data in the buffer to completely
                // parse the WebSocket DataFrame. If not return null.
                if(dataLength+4 > in.remaining()){
                    return null;
                }
                
                byte mask[] = new byte[4];
                for (int i = 0; i < 4; i++) {
                    mask[i] = in.get();
                }
    
                /*  now un-mask frameLen bytes as per Section 5.3 RFC 6455
                    Octet i of the transformed data ("transformed-octet-i") is the XOR of
                    octet i of the original data ("original-octet-i") with octet at index
                    i modulo 4 of the masking key ("masking-key-octet-j"):
    
                    j                   = i MOD 4
                    transformed-octet-i = original-octet-i XOR masking-key-octet-j
                * 
                */
                
                byte[] unMaskedPayLoad = new byte[dataLength];
                for (int i = 0; i < dataLength; i++) {
                    byte maskedByte = in.get();
                    unMaskedPayLoad[i] = (byte) (maskedByte ^ mask[i % 4]);
                }
                
                if(resultBuffer == null){
                    resultBuffer = IoBuffer.wrap(unMaskedPayLoad);
                    resultBuffer.position(resultBuffer.limit());
                    resultBuffer.setAutoExpand(true);
                }
                else{
                    resultBuffer.put(unMaskedPayLoad);
                }
            } else {
                // Validate if we have enough data in the buffer to completely
                // parse the WebSocket DataFrame. If not return null.
                if(dataLength > in.remaining()){
                    return null;
                }
                
                if(resultBuffer == null){
                    resultBuffer = IoBuffer.allocate(dataLength);
                    resultBuffer.setAutoExpand(true);
                    resultBuffer.put(in.array(), in.position(), dataLength);
                    in.position(in.position() + dataLength);
                }
                else{
                    resultBuffer.put(in.array(), in.position(), dataLength);
                    in.position(in.position() + dataLength);
                }
            }
            
            if (opCode == 0xA) {
                WebSocketCodecPacket result = WebSocketCodecPacket.buildPacketBinary(resultBuffer, opCode);
                session.write(result);
            }
        }
        while(false);
        
        resultBuffer.flip();
        if (bBinary) {
            packet = WebSocketCodecPacket.buildPacket(resultBuffer);
        } else {
            packet = WebSocketCodecPacket.buildPacketText(resultBuffer);
        }
        return packet;

    }    
}
