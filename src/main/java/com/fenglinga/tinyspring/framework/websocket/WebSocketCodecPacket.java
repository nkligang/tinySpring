/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenglinga.tinyspring.framework.websocket;

import org.apache.mina.core.buffer.IoBuffer;

/**
 * Defines the class whose objects are understood by websocket encoder.
 * 
 * @author DHRUV CHOPRA
 */
public class WebSocketCodecPacket {
    public static final int PACKET_TYPE_TEXT = 1;
    public static final int PACKET_TYPE_BINARY = 2;
    public static final int PACKET_TYPE_PING = 9;
    
    private int type = PACKET_TYPE_BINARY;
    private boolean mask = false;
    private IoBuffer packet;
    
    /*
     * Builds an instance of WebSocketCodecPacket that simply wraps around 
     * the given IoBuffer.
     */
    public static WebSocketCodecPacket buildPacket(IoBuffer buffer){
        return new WebSocketCodecPacket(buffer);
    }
    public static WebSocketCodecPacket buildPacketBinary(IoBuffer buffer, int type){
        return new WebSocketCodecPacket(buffer, type, false);
    }
    public static WebSocketCodecPacket buildPacketBinary(IoBuffer buffer, int type, boolean hasMask){
        return new WebSocketCodecPacket(buffer, type, hasMask);
    }
    
    /*
     * Builds an instance of WebSocketCodecPacket that simply wraps around 
     * the given String.
     */
    public static WebSocketCodecPacket buildPacket(String text){
        return new WebSocketCodecPacket(text);
    }
    public static WebSocketCodecPacket buildPacketText(IoBuffer buffer){
        return new WebSocketCodecPacket(buffer, PACKET_TYPE_TEXT, false);
    }
    
    private WebSocketCodecPacket(IoBuffer buffer){
        packet = buffer;
        type = PACKET_TYPE_BINARY;
    }
    
    private WebSocketCodecPacket(IoBuffer buffer, int _type, boolean hasMask){
        packet = buffer;
        type = _type;
        mask = hasMask;
    }
    
    private WebSocketCodecPacket(String text){
        packet = IoBuffer.wrap(text.getBytes());
        type = PACKET_TYPE_TEXT;
    }
    
    public IoBuffer getPacket(){
        return packet;
    }
    
    public String getString() {
        return new String(packet.array(), packet.position(), packet.remaining());
    }
    
    public int getPacketType(){
        return type;
    }
    
    public boolean isPacketText(){
        return type == PACKET_TYPE_TEXT;
    }
    
    public boolean hasMask() {
        return mask;
    }
}
