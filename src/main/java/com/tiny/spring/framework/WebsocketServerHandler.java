package com.tiny.spring.framework;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.http.api.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tiny.spring.common.Utils;
import com.tiny.spring.framework.annotation.websocket.OnClose;
import com.tiny.spring.framework.annotation.websocket.OnError;
import com.tiny.spring.framework.annotation.websocket.OnMessage;
import com.tiny.spring.framework.annotation.websocket.OnOpen;
import com.tiny.spring.framework.annotation.websocket.ServerEndpoint;
import com.tiny.spring.framework.websocket.WebSocketCodecPacket;
import com.tiny.spring.framework.websocket.WebSocketUtils;

public class WebsocketServerHandler extends IoHandlerAdapter {
	private static class EndpointStruct {
		public Class<?> klass;
		public Method onOpen;
		public Method onClose;
		public Method onMessageText;
		public Method onMessageBinary;
		public Method onError;
	};
	
	private static class EndpointEntry {
		public EndpointStruct struct;
		public Object instance;
	}
	
	public final static String SESSION_ATTR_ENDPOINT = "endpoint";

	private final static Logger LOGGER = LoggerFactory.getLogger(WebsocketServerHandler.class);
	private static boolean mShutdown = false;
	protected HashMap<String, EndpointStruct> mEndpointMap = new HashMap<String, EndpointStruct>();
	
	public WebsocketServerHandler() {
		Set<Class<?>> klassSet = Utils.getClasses("endpoint");
		for (Class<?> klass : klassSet) {
			Annotation[] annos = klass.getAnnotations();
            for (Annotation annotation : annos) {
            	Class<?> annotationType = annotation.annotationType();
            	if (!annotationType.getSimpleName().equals(ServerEndpoint.class.getSimpleName())) {
            		continue;
            	}
            	ServerEndpoint end = klass.getAnnotation(ServerEndpoint.class);
            	if (end == null) {
            		continue;
            	}
            	EndpointStruct es = new EndpointStruct();
            	es.klass = klass;
            	Method[] methods = klass.getDeclaredMethods();
            	for (Method method : methods) {
            		OnOpen onOpen = method.getAnnotation(OnOpen.class);
                	if (onOpen != null) {
                		es.onOpen = method;
                		continue;
                	}
                	OnClose onClose = method.getAnnotation(OnClose.class);
                	if (onClose != null) {
                		es.onClose = method;
                		continue;
                	}
                	OnError onError = method.getAnnotation(OnError.class);
                	if (onError != null) {
                		es.onError = method;
                		continue;
                	}
                	OnMessage onMessage = method.getAnnotation(OnMessage.class);
                	if (onMessage != null) {
                		Parameter[] parameters = method.getParameters();
                		if (parameters.length == 0) {
                			continue;
                		}
                		Parameter parameter = parameters[0];
            			Class<?> type = parameter.getType();
            			String simpleName = type.getSimpleName();
                		if (simpleName.equals("String")) {
                    		es.onMessageText = method;
                		} else {
                    		es.onMessageBinary = method;
                		}
                		continue;
                	}
            	}
            	mEndpointMap.put(end.value(), es);
            }
		}
	}

	public void onApplicationShutdown() {
		mShutdown = true;
	}
	
    @Override
    public void sessionCreated(IoSession session) {
		// set idle time to 10 seconds
		session.getConfig().setIdleTime(IdleStatus.BOTH_IDLE, 30);
    }
    
    @Override
	public void sessionOpened(final IoSession session) {
        session.setAttribute(WebSocketUtils.SessionHttpRequestRunnable, new Runnable() {
			@Override
			public void run() {
				try {
					HttpRequest req = (HttpRequest)session.getAttribute(WebSocketUtils.SessionHttpRequest);
					String requestPath = req.getRequestPath();
	
					EndpointEntry endpoint = (EndpointEntry)session.getAttribute(SESSION_ATTR_ENDPOINT);
					if (endpoint == null) {
						if (mEndpointMap.containsKey(requestPath)) {
							EndpointStruct value = mEndpointMap.get(requestPath);
							endpoint = new EndpointEntry();
							endpoint.instance = value.klass.newInstance();
							endpoint.struct = value;
							session.setAttribute(SESSION_ATTR_ENDPOINT, endpoint);
							
							if (endpoint.struct.onOpen != null) {
								List<Object> params = new ArrayList<Object>();
								params.add(session);
								endpoint.struct.onOpen.invoke(endpoint.instance, params.toArray());
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
        });
	}

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        EndpointEntry endpoint = (EndpointEntry)session.getAttribute(SESSION_ATTR_ENDPOINT);
        if (endpoint != null && endpoint.struct.onClose != null) {
			List<Object> params = new ArrayList<Object>();
			params.add(session);
			endpoint.struct.onClose.invoke(endpoint.instance, params.toArray());
        }
    }
    
	@Override
	public void messageReceived(final IoSession session, Object message) throws Exception {
		if (mShutdown) {
			session.close(true);
			return;
		}
		// Check that we can service the request context
		if (message instanceof WebSocketCodecPacket) {
			WebSocketCodecPacket packet = (WebSocketCodecPacket)message;
			HttpRequest req = (HttpRequest)session.getAttribute(WebSocketUtils.SessionHttpRequest);
			String requestPath = req.getRequestPath();
			EndpointEntry endpoint = (EndpointEntry)session.getAttribute(SESSION_ATTR_ENDPOINT);
			if (endpoint != null) {
				if (packet.isPacketText()) {
					if (endpoint.struct.onMessageText != null) {
						List<Object> params = new ArrayList<Object>();
						params.add(packet.getString());
						params.add(session);
						endpoint.struct.onMessageText.invoke(endpoint.instance, params.toArray());
					}
				} else {
					if (endpoint.struct.onMessageBinary != null) {
						List<Object> params = new ArrayList<Object>();
						params.add(packet.getPacket().array());
						params.add(session);
						endpoint.struct.onMessageBinary.invoke(endpoint.instance, params.toArray());
					}
				}
			} else {
				LOGGER.warn("no end point has been found: " + requestPath);
			}
		}
	}

	@Override
	public void sessionIdle(IoSession session, IdleStatus status) {
		IoBuffer buffer = IoBuffer.allocate(0);
		WebSocketCodecPacket websocketPacket = WebSocketCodecPacket.buildPacketBinary(buffer, WebSocketCodecPacket.PACKET_TYPE_PING);
		session.write(websocketPacket);
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {    	
        EndpointEntry endpoint = (EndpointEntry)session.getAttribute(SESSION_ATTR_ENDPOINT);
        if (endpoint != null && endpoint.struct.onError != null) {
			List<Object> params = new ArrayList<Object>();
			params.add(session);
			params.add(cause);
			endpoint.struct.onError.invoke(endpoint.instance, params.toArray());
        }

		session.close(true);
	}
}
