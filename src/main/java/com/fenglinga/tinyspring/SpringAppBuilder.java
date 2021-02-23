package com.fenglinga.tinyspring;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.http.HttpServerCodec;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import com.fenglinga.tinyspring.common.Constants;
import com.fenglinga.tinyspring.framework.HttpServerHandler;
import com.fenglinga.tinyspring.framework.WebsocketServerHandler;
import com.fenglinga.tinyspring.framework.websocket.WebSocketDecoder;
import com.fenglinga.tinyspring.framework.websocket.WebSocketEncoder;
import com.fenglinga.tinyspring.mysql.Db;
import com.fenglinga.tinyspring.scheduling.ScheduledTaskRegistrar;

public class SpringAppBuilder {
    private NioSocketAcceptor mAcceptor = null;
    private HttpServerHandler mHandler = null;
    
    private NioSocketAcceptor mWebsocketAcceptor = null;
    private WebsocketServerHandler mWebsocketHandler = null;

    public final ScheduledThreadPoolExecutor mExecutor = new ScheduledThreadPoolExecutor(20);
    public ScheduledTaskRegistrar mScheduledTaskRegistrar = null;

    private boolean mShutdownEnabled = false;
    private long mShutdownStartTime = 0;
    private long mShutdownDelayTime = 0;
    private boolean mMainThreadExit = false;
    
    private Runnable mInitializer = null;
    
    public SpringAppBuilder setInitializer(Runnable init) {
        mInitializer = init;
        return this;
    }

    public void run(String[] args) throws Exception
    {
        try {
            System.out.println("Default Charset=" + Charset.defaultCharset());
            String sv = System.getProperty("java.version");
            System.out.println("java version:" + sv);
            if (sv.compareTo("1.8.0") < 0) {
                System.out.println("java版本号太低，无法支持");
                return;
            }
            
            System.out.println("Application arguments:");
            for (int i = 0; i < args.length; i++)
            {
                System.out.println("[" + i + "]" + args[i]);
            }
            System.out.println("");
                        
            Constants.Init();

            int port = Constants.Config.getIntValue("server.port");
            
            if (mInitializer != null) {
                mInitializer.run();
            }

            // Create an acceptor
            mAcceptor = new NioSocketAcceptor();

            DefaultIoFilterChainBuilder chain = mAcceptor.getFilterChain();
            // Create a service configuration
            chain.addLast("protocolFilter", new HttpServerCodec());
            
            mHandler = new HttpServerHandler();
            mAcceptor.setHandler(mHandler);
            mAcceptor.bind(new InetSocketAddress(port));

            System.out.println("Http manager server now listening on port " + port);

            Integer websocketPort = Constants.Config.getInteger("webserver.port");
            if (websocketPort != null) {
                // Create an acceptor
                mWebsocketAcceptor = new NioSocketAcceptor();

                DefaultIoFilterChainBuilder chainWebsocket = mWebsocketAcceptor.getFilterChain();
                // Create a service configuration
                chainWebsocket.addLast("protocolFilter", new ProtocolCodecFilter(new WebSocketEncoder(), new WebSocketDecoder()));

                mWebsocketHandler = new WebsocketServerHandler();
                mWebsocketAcceptor.setHandler(mWebsocketHandler);
                mWebsocketAcceptor.bind(new InetSocketAddress(websocketPort));

                System.out.println("Websocket server now listening on port " + websocketPort);
            }

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        int delay = 5;
                        mShutdownEnabled = true;
                        mShutdownStartTime = System.currentTimeMillis();
                        mShutdownDelayTime = (long)(delay * 1000);
                        Constants.LOGGER.debug("application shutdown begin");
                        do {
                            Thread.sleep(1000);
                        } while (!mMainThreadExit);
                        Constants.LOGGER.debug("application shutdown end");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            
            mScheduledTaskRegistrar = new ScheduledTaskRegistrar(mExecutor);
            mExecutor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    Db.ping();
                }
            }, 30 * 60, 30 * 60, TimeUnit.SECONDS);

            long debugSessionStartTime = System.currentTimeMillis();
            long debugSessionDelayTime = 10000;
            long gcStartTime = System.currentTimeMillis();
            long gcDelayTime = 15 * 60 * 1000; // 15 minutes
            long shutdownTimeLeft = 0;
            for (;;) {
                try {
                    long currentTimeMillis = System.currentTimeMillis();
                    if (mShutdownEnabled) {
                        long shutdownTimeLeftMillis = mShutdownStartTime + mShutdownDelayTime - currentTimeMillis;
                        long timeLeft = shutdownTimeLeftMillis / 1000;
                        if (timeLeft != shutdownTimeLeft) {
                            shutdownTimeLeft = timeLeft;
                            System.out.println("Server will be shutdown after " + shutdownTimeLeft + " seconds");
                        }
                        if (currentTimeMillis > mShutdownStartTime + mShutdownDelayTime) {
                            break;
                        }
                    }
                    if (currentTimeMillis > debugSessionStartTime + debugSessionDelayTime) {
                        debugSessionStartTime = currentTimeMillis;
                    }
                    if (currentTimeMillis > gcStartTime + gcDelayTime) {
                        gcStartTime = currentTimeMillis;
                        System.gc();
                    }
                    Thread.sleep(10);
                } catch (Exception e) {
                    Constants.LOGGER.error(e.getMessage(), e);
                    mShutdownEnabled = true;
                    break;
                }
            }
            
            if (websocketPort != null) {
                mWebsocketAcceptor.unbind();
                mWebsocketAcceptor.dispose();
    
                mWebsocketHandler.onApplicationShutdown();
            }

            mAcceptor.unbind();
            mAcceptor.dispose();

            mHandler.onApplicationShutdown();
            
            mExecutor.shutdown();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        mMainThreadExit = true;
    }

    public boolean shutdown(float fDelay)
    {
        if (mHandler != null) {
            mHandler.onApplicationShutdown();
        }
        if (mAcceptor != null)  {
            mAcceptor.unbind();
            mAcceptor.dispose();
        }
        return true;
    }
}
