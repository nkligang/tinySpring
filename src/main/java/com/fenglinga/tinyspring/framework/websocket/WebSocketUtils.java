/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenglinga.tinyspring.framework.websocket;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.http.ArrayUtil;
import org.apache.mina.http.HttpRequestImpl;
import org.apache.mina.http.HttpServerCodec;
import org.apache.mina.http.api.DefaultHttpResponse;
import org.apache.mina.http.api.HttpMethod;
import org.apache.mina.http.api.HttpStatus;
import org.apache.mina.http.api.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static utility class containing methods used for websocket encoding
 * and decoding.
 *
 * @author DHRUV CHOPRA
 */
public class WebSocketUtils {
    
	public static final String SessionAttribute = "isWEB";
    public static final String SessionHttpRequest = "httpRequest";
    public static final String SessionHttpResponse = "httpResponse";
    public static final String SessionHttpRequestRunnable = "httpRequestRunnable";
    public static final String SessionHttpResponseRunnable = "httpResponseRunnable";
    public static final String SessionHttpRequestURI = "httpRequestURI";
    
    // Construct a successful websocket handshake response using the key param
    // (See RFC 6455).
    static WebSocketHandShakeResponse buildWSHandshakeResponse(String key){
        String response = "HTTP/1.1 101 Web Socket Protocol Handshake\r\n";
        response += "Upgrade: websocket\r\n";
        response += "Connection: Upgrade\r\n";
        response += "Sec-WebSocket-Accept: " + key + "\r\n";
        response += "\r\n";        
        return new WebSocketHandShakeResponse(response);
    }
    
    // Parse the string as a websocket request and return the value from
    // Sec-WebSocket-Key header (See RFC 6455). Return empty string if not found.
    static String getClientWSRequestKey(String WSRequest) {
        String[] headers = WSRequest.split("\r\n");
        String socketKey = "";
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].contains("Sec-WebSocket-Key")) {
                socketKey = (headers[i].split(":")[1]).trim();
                break;
            }
        }
        return socketKey;
    }    
    
    // 
    // Builds the challenge response to be used in WebSocket handshake.
    // First append the challenge with "258EAFA5-E914-47DA-95CA-C5AB0DC85B11" and then
    // make a SHA1 hash and finally Base64 encode it. (See RFC 6455)
    static String getWebSocketKeyChallengeResponse(String challenge) throws NoSuchAlgorithmException, UnsupportedEncodingException {

        challenge += "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        MessageDigest cript = MessageDigest.getInstance("SHA-1");
        cript.reset();
        cript.update(challenge.getBytes("utf8"));
        byte[] hashedVal = cript.digest();        
        return Base64.encodeBytes(hashedVal);
    }
    
    private static final Logger LOG = LoggerFactory.getLogger(HttpServerCodec.class);

    /** Regex to parse HttpRequest Request Line */
    public static final Pattern REQUEST_LINE_PATTERN = Pattern.compile(" ");

    /** Regex to parse out QueryString from HttpRequest */
    public static final Pattern QUERY_STRING_PATTERN = Pattern.compile("\\?");

    /** Regex to parse out parameters from query string */
    public static final Pattern PARAM_STRING_PATTERN = Pattern.compile("\\&|;");

    /** Regex to parse out key/value pairs */
    public static final Pattern KEY_VALUE_PATTERN = Pattern.compile("=");

    /** Regex to parse raw headers and body */
    public static final Pattern RAW_VALUE_PATTERN = Pattern.compile("\\r\\n\\r\\n");

    /** Regex to parse raw headers from body */
    public static final Pattern HEADERS_BODY_PATTERN = Pattern.compile("\\r\\n");

    /** Regex to parse header name and value */
    public static final Pattern HEADER_VALUE_PATTERN = Pattern.compile(": ");

    /** Regex to split cookie header following RFC6265 Section 5.4 */
    public static final Pattern COOKIE_SEPARATOR_PATTERN = Pattern.compile(";");

    /** Regex to parse HttpRequest Request Line */
    public static final Pattern RESPONSE_LINE_PATTERN = Pattern.compile(" ");

    public static HttpRequestImpl parseHttpRequestHead(final IoBuffer buffer) {
    	// Java 6 >> String raw = new String(buffer.array(), 0, buffer.limit(), Charset.forName("UTF-8"));
        final String raw = new String(buffer.array(), 0, buffer.limit(), Charset.forName("UTF-8"));
        LOG.info("[Request Headers]\r\n" + raw.trim());
        final String[] headersAndBody = RAW_VALUE_PATTERN.split(raw, -1);

        if (headersAndBody.length <= 1) {
            // we didn't receive the full HTTP head
            return null;
        }

        String[] headerFields = HEADERS_BODY_PATTERN.split(headersAndBody[0]);
        headerFields = ArrayUtil.dropFromEndWhile(headerFields, "");

        final String requestLine = headerFields[0];
        final Map<String, String> generalHeaders = new HashMap<String, String>();

        for (int i = 1; i < headerFields.length; i++) {
            final String[] header = HEADER_VALUE_PATTERN.split(headerFields[i]);
            generalHeaders.put(header[0].toLowerCase(), header[1]);
        }

        final String[] elements = REQUEST_LINE_PATTERN.split(requestLine);
        final HttpMethod method = HttpMethod.valueOf(elements[0]);
        final HttpVersion version = HttpVersion.fromString(elements[2]);
        final String[] pathFrags = QUERY_STRING_PATTERN.split(elements[1]);
        final String requestedPath = pathFrags[0];
        final String queryString = pathFrags.length == 2 ? pathFrags[1] : "";

        // we put the buffer position where we found the beginning of the HTTP body
        int iHeaderAndBodyLen = headersAndBody[0].length();
        if (iHeaderAndBodyLen + 4 == buffer.remaining()) {
        	buffer.position(iHeaderAndBodyLen + 4);
        } else {
        	// handle invalid header(include none-ASCII characters)
        	try {
        		String strASCII = new String(headersAndBody[0].getBytes(), "US-ASCII");
        		buffer.position(strASCII.length() + 4);
        	} catch (UnsupportedEncodingException e) {
        		buffer.position(buffer.limit());
    		}
        }

        return new HttpRequestImpl(version, method, requestedPath, queryString, generalHeaders);
    }
    
    public static DefaultHttpResponse parseHttpReponseHead(final IoBuffer buffer) {
        // Java 6 >> String raw = new String(buffer.array(), 0, buffer.limit(), Charset.forName("UTF-8"));
        final String raw = new String(buffer.array(), 0, buffer.limit());
        final String[] headersAndBody = RAW_VALUE_PATTERN.split(raw, -1);
        if (headersAndBody.length <= 1) {
            // we didn't receive the full HTTP head
            return null;
        }

        String[] headerFields = HEADERS_BODY_PATTERN.split(headersAndBody[0]);
        headerFields = ArrayUtil.dropFromEndWhile(headerFields, "");

        final String requestLine = headerFields[0];
        final Map<String, String> generalHeaders = new HashMap<String, String>();

        for (int i = 1; i < headerFields.length; i++) {
            final String[] header = HEADER_VALUE_PATTERN.split(headerFields[i]);
            generalHeaders.put(header[0].toLowerCase(), header[1]);
        }

        final String[] elements = RESPONSE_LINE_PATTERN.split(requestLine);
        HttpStatus status = null;
        final int statusCode = Integer.valueOf(elements[1]);
        for (int i = 0; i < HttpStatus.values().length; i++) {
            status = HttpStatus.values()[i];
            if (statusCode == status.code()) {
                break;
            }
        }
        final HttpVersion version = HttpVersion.fromString(elements[0]);

        // we put the buffer position where we found the beginning of the HTTP body
        buffer.position(headersAndBody[0].length() + 4);

        return new DefaultHttpResponse(version, status, generalHeaders);
    }
}
