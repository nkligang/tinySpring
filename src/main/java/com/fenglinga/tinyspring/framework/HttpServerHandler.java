package com.fenglinga.tinyspring.framework;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.http.api.DefaultHttpResponse;
import org.apache.mina.http.api.HttpEndOfContent;
import org.apache.mina.http.api.HttpMethod;
import org.apache.mina.http.api.HttpRequest;
import org.apache.mina.http.api.HttpResponse;
import org.apache.mina.http.api.HttpStatus;
import org.apache.mina.http.api.HttpVersion;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.fenglinga.tinyspring.common.Constants;
import com.fenglinga.tinyspring.common.Utils;
import com.fenglinga.tinyspring.framework.Controller.ResolveParameterTypeResult;
import com.fenglinga.tinyspring.framework.annotation.AfterReturning;
import com.fenglinga.tinyspring.framework.annotation.Aspect;
import com.fenglinga.tinyspring.framework.annotation.Controller;
import com.fenglinga.tinyspring.framework.annotation.KeepOriginParameter;
import com.fenglinga.tinyspring.framework.annotation.RequestBody;
import com.fenglinga.tinyspring.framework.annotation.RequestMapping;
import com.fenglinga.tinyspring.framework.annotation.ResponseBody;
import com.fenglinga.tinyspring.framework.annotation.RestController;
import com.fenglinga.tinyspring.framework.annotation.Transactional;
import com.fenglinga.tinyspring.mysql.Db;
import org.objectweb.asm.*;

public class HttpServerHandler extends IoHandlerAdapter {
    private final static Logger LOGGER = LoggerFactory.getLogger(HttpServerHandler.class);
    public static boolean LOG_ENABLED = true;
    private static boolean mShutdown = false;
    protected HashMap<String, String> mContentTypeMap = new HashMap<String, String>();
    private VelocityEngine mVelocityEngine = null;
    private final ScheduledThreadPoolExecutor mExecutor = new ScheduledThreadPoolExecutor(10);
    protected HashMap<String, Method> mRestControllerMethodMap = new HashMap<String, Method>();
    protected HashMap<String, Method> mControllerMethodMap = new HashMap<String, Method>();
    protected List<Method> mAfterReturningMethodList = new ArrayList<Method>();
    /** 是否支持断点续传 */
    private static boolean mRangeSupport = false;
    private static final String BODY_LOG = "body.log";
    private static final String PARSE_PARAMETER_LOG = "parse.parameter.log";
    private static final String REQUEST_ENTER_TIME = "request.enter.time";
    private static final int LOG_MAX_STRING_LENGTH = 512;
    private static boolean mParameterNamesCacheEnabled = true;
    protected static HashMap<Method, String []> mParameterNamesMap = new HashMap<Method, String []>();
    
    public HttpServerHandler() {
        // Images
        mContentTypeMap.put(".jpg", "image/jpeg");
        mContentTypeMap.put(".jpeg", "image/jpeg");
        mContentTypeMap.put(".gif", "image/gif");
        mContentTypeMap.put(".tiff", "image/tiff");
        mContentTypeMap.put(".ico", "image/x-icon");
        mContentTypeMap.put(".png", "image/png");
        mContentTypeMap.put(".bmp", "application/x-bmp");
        // Others
        mContentTypeMap.put(".js", "application/x-javascript");
        mContentTypeMap.put(".css", "text/css");
        mContentTypeMap.put(".html", "text/html");
        mContentTypeMap.put(".jsp", "text/html");

        Velocity.init();
        Properties properties = new Properties();
        properties.setProperty(VelocityEngine.FILE_RESOURCE_LOADER_PATH, Constants.AppAssetsPath + "templates");
        mVelocityEngine = new VelocityEngine(properties);
        
        Set<Class<?>> klassSet = Utils.getClasses("controller");
        for (Class<?> klass : klassSet) {
            boolean isRestController = false;
            boolean isController = false;
            boolean isAspect = false;
            Annotation[] annos = klass.getAnnotations();
            for (Annotation annotation : annos) {
                Class<?> annotationType = annotation.annotationType();
                if (annotationType.getSimpleName().equals(RestController.class.getSimpleName())) {
                    isRestController = true;
                    break;
                }
                if (annotationType.getSimpleName().equals(Controller.class.getSimpleName())) {
                    isController = true;
                    break;
                }
                if (annotationType.getSimpleName().equals(Aspect.class.getSimpleName())) {
                    isAspect = true;
                    break;
                }
            }
            Method[] methods = klass.getDeclaredMethods();
            for (Method method : methods) {
                if (isRestController || isController) {
                    RequestMapping rm = method.getAnnotation(RequestMapping.class);
                    if (rm == null) {
                        continue;
                    }
                    for (String v : rm.value()) {
                        if (isRestController) {
                            mRestControllerMethodMap.put(v, method);
                            cacheParameterNames(method);
                        }
                        if (isController) {
                            mControllerMethodMap.put(v, method);
                            cacheParameterNames(method);
                        }
                    }
                }
                if (isAspect) {
                    AfterReturning ar = method.getAnnotation(AfterReturning.class);
                    if (ar != null) {
                        mAfterReturningMethodList.add(method);
                    }
                }
            }
        }
        if (LOG_ENABLED) LOGGER.debug("Found rest controller method:" + mRestControllerMethodMap.size());
        if (LOG_ENABLED) LOGGER.debug("Found controller method:" + mControllerMethodMap.size());
    }
    
    public HashMap<String, Method> getControllerMethodMaps(boolean isRest) {
        return isRest ? mRestControllerMethodMap : mControllerMethodMap;
    }
    
    private void cacheParameterNames(Method method) {
    	if (!mParameterNamesCacheEnabled) {
    		return;
    	}
    	String cachePath = Constants.AppAssetsPath + "parameter_names.cache";
    	String fileContent = Utils.LoadStringFromFile(cachePath);
    	JSONObject obj = fileContent != null ? JSON.parseObject(fileContent) : new JSONObject();
        Class<?> klass = method.getDeclaringClass();
        String key = klass.getName() + "." + method.getName();
        try {
            String[] paramNames = getMethodParameterNamesByAsm(klass, method);
            mParameterNamesMap.put(method, paramNames);
            obj.put(key, paramNames);
            Utils.SaveStringToFile(cachePath, obj.toJSONString());
        } catch (Exception e) {
        	JSONArray array = obj.getJSONArray(key);
        	String[] paramNames = array != null ? (String[])array.toArray(new String[array.size()]) : new String[] {};
            mParameterNamesMap.put(method, paramNames);
        }
    }

    public void onApplicationShutdown() {
        mShutdown = true;
    }

    @Override
    public void sessionCreated(IoSession session) {
        // set idle time to 10 seconds
        session.getConfig().setIdleTime(IdleStatus.BOTH_IDLE, 10);
    }

    @Override
    public void sessionOpened(IoSession session) {
        //if (LOG_ENABLED) LOGGER.debug("OPENED:" + this + "  Session: " + session);
    }
    
    public static String getSessionIPAddress(IoSession session) {
        if (session == null) return "";
        String remoteAddress = session.getRemoteAddress().toString();
        int iStartPos = remoteAddress.indexOf("/");
        int iEndPos = remoteAddress.lastIndexOf(":");
        if (iStartPos < 0 || iEndPos < 0) return "";
        return remoteAddress.substring(iStartPos+1, iEndPos);
    }

    private static String getSessionIPAddress(HttpRequest request, IoSession session) {
        if (request.containsHeader("x-forwarded-for")) {
            return request.getHeader("x-forwarded-for");
        }
        if (request.containsHeader("x_forward_for")) {
            return request.getHeader("x_forward_for");
        }
        return getSessionIPAddress(session);
    }

    private void writeLog(IoSession session, HttpRequest request, DefaultHttpResponse response, String content) {
        if (!LOG_ENABLED) return;
        StringBuilder sb = new StringBuilder();
        sb.append(getSessionIPAddress(request, session)).append(" ");
        long enterTime = (Long)session.getAttribute(REQUEST_ENTER_TIME);
        sb.append("[").append(Utils.getMSTime() - enterTime).append("ms]").append(" ");
        sb.append("\"");
        sb.append(request.getMethod());
        sb.append(" ").append(request.getRequestPath());
        if (request.getQueryString().length() > 0) sb.append("?").append(request.getQueryString());
        sb.append(" ").append(response.getProtocolVersion());
        sb.append("\"");
        sb.append(" ").append(response.getStatus().code());
        sb.append(" ").append(response.containsHeader("Content-Length") ? response.getHeader("Content-Length") : "-");
        sb.append(" \"").append(request.getHeader("user-agent")).append("\"");
        String bodyLog = (String)session.getAttribute(BODY_LOG);
        if (bodyLog != null) {
            sb.append("\r\n");
            if (bodyLog.length() > LOG_MAX_STRING_LENGTH) {
                sb.append(bodyLog.substring(0, LOG_MAX_STRING_LENGTH)).append("...");
            } else {
                sb.append(bodyLog);
            }
        }
        String parseParameterLog = (String)session.getAttribute(PARSE_PARAMETER_LOG);
        if (parseParameterLog != null) {
            sb.append("\r\n").append(parseParameterLog);
        }
        if (mRestControllerMethodMap.containsKey(request.getRequestPath()) && content != null) {
            sb.append("\r\n").append("[RESPONSE]");
            if (content.length() > LOG_MAX_STRING_LENGTH) {
                sb.append(content.substring(0, LOG_MAX_STRING_LENGTH)).append("...");
            } else {
                sb.append(content);
            }
        }
        LOGGER.info(sb.toString());
    }
    
    protected HttpResponse pageAccess(HttpRequest request, String requestPath) {
        return null;
    }
    
    protected HttpResponse pagePreAccess(HttpRequest request, String requestPath) {
        return null;
    }

    protected void pageNotFound(final IoSession session, HttpRequest request) {
        HashMap<String, String> headers = new HashMap<String, String>();
        DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.CLIENT_ERROR_NOT_FOUND, headers);
        session.write(response).addListener(IoFutureListener.CLOSE);
        writeLog(session, request, response, null);
    }

    public static byte[] compress(byte[] bytes) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        GZIPOutputStream gunzip = new GZIPOutputStream(out);
        int n;
        byte[] buffer = new byte[1024];
        while ((n = in.read(buffer)) >= 0) {
            gunzip.write(buffer, 0, n);
        }
        in.close();
        gunzip.close();
        return out.toByteArray();
    }
    
    private void addCrossOriginHeaders(HttpRequest request, HashMap<String, String> headers) {
        String crossOriginHeaders = Constants.Config.getString("server.cross_origin_headers");
        if (crossOriginHeaders != null && crossOriginHeaders.length() > 0) {
            String [] heads = crossOriginHeaders.split(";");
            for (String head : heads) {
                int idx = head.indexOf(":");
                if (idx > 0) {
                    String key = head.substring(0, idx).trim();
                    String value = head.substring(idx + 1, head.length()).trim();
                    if (key.toLowerCase().equals("access-control-allow-origin") && value.equals("*")) {
                        value = request.getHeader("origin");
                    }
                    headers.put(key, value);
                }
            }
        }
    }
    
    public void redirect(IoSession session, HttpRequest request, String redirectUrl, String setCookie) {
        String result = "";
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "text/html; charset=utf-8");
        headers.put("Content-Length", String.valueOf(Utils.getContentLenth(result)));
        headers.put("Location", redirectUrl);
        if (setCookie != null && setCookie.length() > 0) {
            headers.put("Set-Cookie", setCookie);
        }
        addCrossOriginHeaders(request, headers);
        DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.REDIRECTION_FOUND, headers);
        session.write(response).addListener(IoFutureListener.CLOSE);
        writeLog(session, request, response, null);
    }

    private void writeResponse(IoSession session, HttpRequest request, String content, String contentType, HttpServletResponse servletReponse) throws Exception {
        boolean gzipEnabled = false;
        String acceptEncoding = request.getHeader("accept-encoding");
        if (acceptEncoding != null && acceptEncoding.length() != 0) {
            String [] encodings = Utils.parseStringArray(acceptEncoding, ",");
            for (String encoding : encodings) {
                if (encoding.trim().equals("gzip")) {
                    gzipEnabled = true;
                    break;
                }
            }
        }
        
        IoBuffer buffer = null;
        if (content.length() > 0) {
            if (gzipEnabled) {
                buffer = IoBuffer.wrap(compress(content.getBytes("UTF-8")));
            } else {
                buffer = IoBuffer.wrap(content.getBytes("UTF-8"));
            }
        }

        HashMap<String, String> headers = servletReponse != null ? servletReponse.getHeaders() : new HashMap<String, String>();
        if (contentType.length() > 0) {
            headers.put("Content-Type", contentType);
        }
        if (gzipEnabled && buffer != null) {
            headers.put("Content-Encoding", "gzip");
            headers.put("Content-Length", String.valueOf(buffer.remaining()));
        } else {
            headers.put("Content-Length", String.valueOf(Utils.getContentLenth(content)));
        }
        addCrossOriginHeaders(request, headers);
        DefaultHttpResponse response = null;
        if (servletReponse != null) {
            response = new DefaultHttpResponse(servletReponse.getProtocolVersion(), servletReponse.getStatus(), headers);
        } else {
            response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SUCCESS_OK, headers);
        }
        session.write(response);
        if (buffer != null) {
            session.write(buffer).addListener(IoFutureListener.CLOSE);
        }
        writeLog(session, request, response, content);
    }
    
    private void writeResponse(IoSession session, HttpRequest request, HttpServletResponse servletReponse) throws Exception {
        IoBuffer buffer = servletReponse.getBuffer();
        HashMap<String, String> headers = servletReponse.getHeaders();
        if (buffer != null) {
            headers.put("Content-Length", String.valueOf(buffer.remaining()));
        }
        addCrossOriginHeaders(request, headers);
        DefaultHttpResponse response = new DefaultHttpResponse(servletReponse.getProtocolVersion(), servletReponse.getStatus(), headers);
        if (buffer != null && buffer.remaining() > 0) {
            session.write(response);
            session.write(buffer).addListener(IoFutureListener.CLOSE);
        } else {
            session.write(response).addListener(IoFutureListener.CLOSE);
        }
        writeLog(session, request, response, null);
    }
    
    private boolean containsMethod(HttpMethod[] methods, HttpMethod method) {
        boolean hasMethod = false;
        for (HttpMethod m : methods) {
            if (m == method) {
                hasMethod = true;
                break;
            }
        }
        return hasMethod;
    }
    
    private void handleRequest(final IoSession session, HttpRequest request) throws Exception {
        String requestPath = request.getRequestPath();
        requestPath = Utils.decodeURLString(requestPath);
        
        Method foundRestMethod = mRestControllerMethodMap.get(requestPath);
        if (foundRestMethod == null) {
            for (Map.Entry<String, Method> entry : mRestControllerMethodMap.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("^") && key.endsWith("$")) {
                    Matcher requestPathMatcher = Pattern.compile(key).matcher(requestPath);
                    if (requestPathMatcher.find()) {
                        foundRestMethod = entry.getValue();
                    }
                }
            }
        }
        if (foundRestMethod != null) {
            RequestMapping rm = foundRestMethod.getAnnotation(RequestMapping.class);
            if (rm == null) {
                return;
            }
            Transactional tx = foundRestMethod.getAnnotation(Transactional.class);
            Class<?> klass = foundRestMethod.getDeclaringClass();
            com.fenglinga.tinyspring.framework.Controller controller = (com.fenglinga.tinyspring.framework.Controller)klass.newInstance();
            Object result = null;
            try {
                if (tx != null) {
                    Db.startTransaction();
                }
                
                if (!containsMethod(rm.method(), request.getMethod())) {
                    throw new Exception("Request method '" + request.getMethod() + "' not supported");
                }
                
                HashMap<String, Object> allParameters = parseAllParameters(session, request);
                List<Object> params = getMethodParameters(klass, foundRestMethod, session, request, allParameters, controller);
                controller.setRequest(request);
                controller.setSession(session);
                controller.setParameters(allParameters);

                JSONObject reqObj = controller.onRequest(foundRestMethod, requestPath, null);
                if (!reqObj.isEmpty()) {
                    result = reqObj;
                } else {
                    result = foundRestMethod.invoke(controller, params.toArray());                    
                }
                if (tx != null) {
                    Db.commit();
                }
                // 后处理
                for (int i = 0; i < mAfterReturningMethodList.size(); i++) {
                    Method afterReturningMethod = mAfterReturningMethodList.get(i);
                    Class<?> aspectKlass = afterReturningMethod.getDeclaringClass();
                    Object aspect = aspectKlass.newInstance();
                    afterReturningMethod.invoke(aspect, session, request, foundRestMethod, params.toArray(), true, result);
                }
            } catch (Exception e) {
                if (tx != null) {
                    Db.rollback();
                }
                result = controller.onException(e);
            }
            HttpServletResponse servletReponse = (HttpServletResponse) session.getAttribute("Response");
            if (servletReponse != null) {
                IoBuffer buffer = servletReponse.getBuffer();
                if (buffer.remaining() > 0) {
                    writeResponse(session, request, servletReponse);
                    return;
                }
            }
            if (result instanceof JSONObject) {
                JSONObject resultObject = (JSONObject)result;
                String resultStr = resultObject.toString(SerializerFeature.DisableCircularReferenceDetect);
                writeResponse(session, request, resultStr, "application/json;charset=utf-8", servletReponse);
                return;
            } else if (result instanceof String) {
                String resultStr = (String)result;
                writeResponse(session, request, resultStr, "text/html;charset=utf-8", servletReponse);
                return;
            }
        }
        
        String localFilePath = Constants.AppAssetsPath + "static" + requestPath;
        final File resFile = new File(localFilePath);
        boolean staticFileExist = resFile.isFile() && resFile.exists();

        Method foundMethod = mControllerMethodMap.get(requestPath);
        if (foundMethod == null && !staticFileExist) {
            for (Map.Entry<String, Method> entry : mControllerMethodMap.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("^") && key.endsWith("$")) {
                    Matcher requestPathMatcher = Pattern.compile(key).matcher(requestPath);
                    if (requestPathMatcher.find()) {
                        foundMethod = entry.getValue();
                    }
                }
            }
        }
        if (foundMethod != null) {
            RequestMapping rm = foundMethod.getAnnotation(RequestMapping.class);
            if (rm == null) {
                return;
            }
            Class<?> klass = foundMethod.getDeclaringClass();
            com.fenglinga.tinyspring.framework.Controller controller = (com.fenglinga.tinyspring.framework.Controller)klass.newInstance();
            controller.setRequest(request);
            controller.setSession(session);

            HashMap<String, Object> allParameters = parseAllParameters(session, request);
            List<Object> params = getMethodParameters(klass, foundMethod, session, request, allParameters, controller);
            VelocityContext model = findMethodParameter(params, VelocityContext.class, true);
            controller.setParameters(allParameters);
            
            Object result = null;            
            JSONObject reqObj = controller.onRequest(foundMethod, requestPath, model);
            if (!reqObj.isEmpty()) {
                String login_url = Constants.Config.getString("server.login_url");
                redirect(session, request, login_url, null);
                return;
            } else {
                result = foundMethod.invoke(controller, params.toArray());
            }
            HttpServletResponse servletReponse = (HttpServletResponse) session.getAttribute("Response");
            if (servletReponse != null) {
                writeResponse(session, request, servletReponse);
                return;
            } else {
                Class<?> returnType = foundMethod.getReturnType();
                String simpleName = returnType.getSimpleName();
                if (simpleName.equals("String")) {
                    String resultStr = (String)result;
                    ResponseBody rb = foundMethod.getAnnotation(ResponseBody.class);
                    if (rb != null) {
                        writeResponse(session, request, resultStr, "text/html;charset=utf-8", servletReponse);
                        return;
                    } else {
                        String redirctPrefix = "redirect:";
                        if (resultStr.startsWith(redirctPrefix)) {
                            String redirectURL = resultStr.substring(redirctPrefix.length());
                            redirect(session, request, redirectURL, null);
                            return;
                        } else {
                            String templateFile = resultStr;
                            String localTemplateFilePath = Constants.AppAssetsPath + "templates/" + templateFile;
                            File resTemplateFile = new File(localTemplateFilePath);
                            if (resTemplateFile.exists()) {
                                StringWriter writer = new StringWriter();
                                try {
                                    Template template = mVelocityEngine.getTemplate(templateFile, "UTF-8");
                                    template.merge(model, writer);
                                } catch (Exception e) {
                                    PrintWriter printWriter = new PrintWriter(writer);
                                    e.printStackTrace(printWriter);
                                }
                                String resultString = writer.toString();
                                writeResponse(session, request, resultString, "text/html;charset=utf-8", servletReponse);
                                return;
                            }
                        }
                    }
                }
            }
        }
        
        if (staticFileExist) {
            boolean gzipEnabled = false;
            String acceptEncoding = request.getHeader("accept-encoding");
            if (acceptEncoding != null && acceptEncoding.length() != 0) {
                String [] encodings = Utils.parseStringArray(acceptEncoding, ",");
                for (String encoding : encodings) {
                    if (encoding.trim().equals("gzip")) {
                        gzipEnabled = true;
                        break;
                    }
                }
            }
            
            boolean isBigFile = resFile.length() > 4 * 1024 * 1024;
            IoBuffer buffer = null;
            if (!isBigFile) {
                byte [] data = Utils.readFileToByteArray(resFile);
                if (gzipEnabled) {
                    buffer = IoBuffer.wrap(compress(data));
                } else {
                    buffer = IoBuffer.wrap(data);
                }
            }

            HashMap<String, String> headers = new HashMap<String, String>();
            String contentType = null;
            int iLastIndex = localFilePath.lastIndexOf('.');
            if (iLastIndex >= 0) {
                String suffix = localFilePath.substring(iLastIndex, localFilePath.length());
                contentType = mContentTypeMap.get(suffix);
            }
            if (contentType != null && contentType.length() != 0) {
                headers.put("Content-Type", contentType);
            } else {
                headers.put("Content-Type", "text/plain");
            }
            HttpStatus status = HttpStatus.SUCCESS_OK;
            long rangeStart = 0;
            long rangeEnd = resFile.length() - 1;
            if (mRangeSupport) {
                headers.put("Content-Range", String.format("bytes %d-%d/%d", 0, rangeEnd, rangeEnd + 1));
                if (request.containsHeader("range")) {
                    String range = request.getHeader("range");
                    String [] rangeParts = range.split("=");
                    if (rangeParts.length == 2) {
                        String [] rangeBytes = rangeParts[1].split("-");
                        if (rangeBytes.length == 2) {
                            rangeStart = Utils.parseLongValue(rangeBytes[0], rangeStart);
                            if (rangeBytes[1].length() != 0) {
                                rangeEnd = Utils.parseLongValue(rangeBytes[1], rangeEnd);
                            }
                            status = HttpStatus.SUCCESS_PARTIAL_CONTENT;
                        } else if (rangeBytes.length == 1) {
                            rangeStart = Utils.parseLongValue(rangeBytes[0], rangeStart);
                            status = HttpStatus.SUCCESS_PARTIAL_CONTENT;
                        }
                    }
                }
            }
            if (mRangeSupport && rangeStart != 0) {
                headers.put("Content-Length", String.valueOf(rangeEnd - rangeStart + 1));
            } else {
                if (gzipEnabled && buffer != null) {
                    headers.put("Content-Encoding", "gzip");
                    headers.put("Content-Length", String.valueOf(buffer.remaining()));
                } else {
                    headers.put("Content-Length", String.valueOf(resFile.length()));
                }
            }
            final long rangeStartBytes = rangeStart;
            headers.put("Last-Modified", Utils.formatTimeMSString(resFile.lastModified(), "EEE, dd MMM yyyy HH:mm:ss 'GMT'"));
            headers.put("ETag", Utils.HashToMD5Hex(localFilePath) + ":0");
            String serverEnv = Constants.Config.getString("server.env");
            if (serverEnv != null && serverEnv.equals("dev")) {
                headers.put("Cache-Control", "no-cache");
            }
            addCrossOriginHeaders(request, headers);

            DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status, headers);
            session.write(response);
            writeLog(session, request, response, null);
            
            // Use this for reading the data.
            session.setAttribute("BodyType", "byte[]");
            if (isBigFile) {
                mExecutor.schedule(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            byte[] buffer = new byte[1000 * 1024];
                            InputStream inputStream = new FileInputStream(resFile);
                            int nRead = 0;
                            if (rangeStartBytes > 0) {
                                inputStream.skip(rangeStartBytes);
                            }
                            while ((nRead = inputStream.read(buffer)) != -1) {
                                if (nRead == 0) break;
                                ByteBuffer outbuffer = ByteBuffer.wrap(buffer, 0, nRead);
                                WriteFuture writeFuture = session.write(outbuffer);
                                while (!writeFuture.isWritten()) {
                                    Thread.sleep(100);
                                    // 如果这里不判定session的连接是否已经断开的话会导致即使已经断开的连接也无法关闭这个Runnable了
                                    if (session.isClosing()) {
                                        inputStream.close();
                                        return;
                                    }
                                }
                            }
                            inputStream.close();
                            session.close(false);
                        } catch (Exception e) {
                            Constants.LOGGER.error(e.getMessage(), e);
                        }
                    }
                }, 0, TimeUnit.SECONDS);
            } else {
                if (buffer != null) {
                    session.write(buffer).addListener(IoFutureListener.CLOSE);
                }
            }
        } else {
            pageNotFound(session, request);
        }
    }
    
    @SuppressWarnings("unchecked")
    private <A extends Object> A findMethodParameter(List<Object> params, Class<A> parameterType, boolean createWhenNull) throws InstantiationException, IllegalAccessException {
        A model = null;
        for (Object param : params) {
            if (parameterType.isInstance(param)) {
                model = (A)param;
            }
        }
        if (createWhenNull && model == null) {
            model = parameterType.newInstance();
        }
        return model;
    }
    
    public static String getMultipartBoundaryValue(HttpRequest request) {
        String boundaryValue = "";
        String contentType = request.getHeader("content-type");
        String [] contentTypeValues = Utils.parseStringArray(contentType, ";");
        if (contentTypeValues.length >= 2) {
            String v = contentTypeValues[0];
            if (v.equals("multipart/form-data")) {
                String boundary = contentTypeValues[1];
                String [] boundaryValues = Utils.parseStringArray(boundary, "=");
                if (boundaryValues.length >= 2) {
                    boundaryValue = boundaryValues[1];
                }
            }
        }
        return boundaryValue;
    }
    
    private static boolean byteEqual(byte [] src, byte [] dest, int destOffset, int destLength) {
        if (src == null && dest == null) return true;
        if (src == null && dest != null) return false;
        if (src != null && dest == null) return false;
        if (src.length != destLength) return false;
        for (int i = 0; i < src.length; i++) {
            if (src[i] != dest[destOffset + i]) return false;
        }
        return true;
    }
    
    public static class MultipartFormData {
        public String header;
        public IoBuffer content;
    };
    
    public static List<MultipartFormData> parseMultipartFormData(String boundaryValue, IoBuffer content) throws UnsupportedEncodingException {
        String splitBoundaryValue = "--" + boundaryValue;
        byte [] boundaryBytes = splitBoundaryValue.getBytes();
        String dataBeginValue = "\r\n\r\n";
        byte [] dataBeginBytes = dataBeginValue.getBytes();
        byte [] contentByteArray = content.array();
        int iOffset = 0;
        int boundaryPartBegin = -1;
        int boundaryPartDataBegin = -1;
        List<MultipartFormData> multipartFormDatas = new ArrayList<MultipartFormData>();
        String multipartFormDataHeader = "";
        do {
            if (byteEqual(boundaryBytes, contentByteArray, iOffset, boundaryBytes.length)) {
                if (boundaryPartDataBegin >= 0) {
                    MultipartFormData multipartFormData = new MultipartFormData();
                    multipartFormData.header = multipartFormDataHeader;
                    // 末尾有个0x0D,0x0A需要去掉
                    multipartFormData.content = IoBuffer.wrap(contentByteArray, boundaryPartDataBegin, iOffset - boundaryPartDataBegin - 2);
                    multipartFormDatas.add(multipartFormData);
                }
                iOffset += boundaryBytes.length;
                boundaryPartBegin = iOffset;
            } else {
                if (boundaryPartBegin >= 0) {
                    if (byteEqual(dataBeginBytes, contentByteArray, iOffset, dataBeginBytes.length)) {
                        multipartFormDataHeader = new String(contentByteArray, boundaryPartBegin, iOffset + dataBeginBytes.length - boundaryPartBegin, "UTF-8");
                        iOffset += dataBeginBytes.length;
                        boundaryPartDataBegin = iOffset;
                        boundaryPartBegin = -1;
                    } else {
                        iOffset++;
                    }
                } else {
                    iOffset++;
                }
            }
            if (iOffset >= content.remaining()) {
                break;
            }
        } while (true);
        return multipartFormDatas;
    }
    
    public HashMap<String, Object> parseAllParameters(IoSession session, HttpRequest request) throws Exception {
        HashMap<String, Object> result = new HashMap<String, Object>();
        if (request.getMethod() == HttpMethod.POST) {
            IoBuffer content = (IoBuffer) session.getAttribute("Content");
            if (content != null) {            
                String boundaryValue = getMultipartBoundaryValue(request);
                if (boundaryValue.length() > 0) {
                    session.setAttribute(BODY_LOG, "[MULTIPART]" + boundaryValue);
                    List<MultipartFormData> multipartFormDatas = parseMultipartFormData(boundaryValue, content);
                    for (MultipartFormData formData : multipartFormDatas) {
                        HashMap<String, String> headers = Utils.parseURLParameters(formData.header, "\r\n", ":");
                        if (headers.containsKey("Content-Disposition")) {
                            HashMap<String, String> disposition = Utils.parseURLParameters(headers.get("Content-Disposition"), ";", "=", true);
                            String name = disposition.get("name");
                            name = name.replaceAll("\"", "");
                            if (disposition.containsKey("filename")) {
                                String orginName = disposition.get("filename").replaceAll("\"", "");
                                MultipartFile mf = new MultipartFile(orginName, headers.get("Content-Type").trim(), formData);
                                result.put(name, mf);
                            } else {
                                String contentData = new String(formData.content.array(), formData.content.position(), formData.content.remaining(), "UTF-8");
                                result.put(name, contentData);
                            }
                        }
                    }
                } else {
                    String postData = new String(content.array(), content.position(), content.remaining(), "UTF-8");
                    session.setAttribute(BODY_LOG, "[BODY]" + postData);
                    HashMap<String, List<String>> parameterMap = Utils.parseURLParametersEx(postData);
                    for (Entry<String, List<String>> entry : parameterMap.entrySet()) {
                        List<String> valueList = entry.getValue();
                        String key = entry.getKey();
                        if (key.endsWith("[]")) {
                            result.put(key.substring(0, key.length() - 2), valueList);
                        } else if (key.endsWith("%5B%5D")) {
                            result.put(key.substring(0, key.length() - 6), valueList);
                        } else {
                            result.put(entry.getKey(), valueList.get(0));
                        }
                    }
                }
            }
        } else {
            Map<String, List<String>> paramters = request.getParameters();
            for (Entry<String, List<String>> entry : paramters.entrySet()) {
                List<String> valueList = entry.getValue();
                String key = entry.getKey();
                if (key.endsWith("[]")) {
                    result.put(key.substring(0, key.length() - 2), valueList);
                } else {
                    result.put(entry.getKey(), valueList.get(0));
                }
            }
        }
        return result;
    }
    
    private static String reg = "(?:')|(?:--)|(?:#)|(/\\*(?:.|[\\n\\r])*?\\*/)|" + "(\\b(select|update|and|or|delete|insert|trancate|char|into|substr|ascii|declare|exec|count|master|into|drop|execute)\\b)";
    private static Pattern sqlPattern = Pattern.compile(reg, Pattern.CASE_INSENSITIVE);
    private static boolean isSQLDefendValid(String str) {
        if (sqlPattern.matcher(str).find()) {
            LOGGER.error("检测到潜在的攻击串:" + str);
            return false;
        }
        return true;
    }

    /**
     * 获取指定类指定方法的参数名
     *
     * @param clazz 要获取参数名的方法所属的类
     * @param method 要获取参数名的方法
     * @return 按参数顺序排列的参数名列表，如果没有参数，则返回null
     */
    public static String[] getMethodParameterNamesByAsm(Class<?> clazz, final Method method) {
        final Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes == null || parameterTypes.length == 0) {
            return null;
        }
        final Type[] types = new Type[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            types[i] = Type.getType(parameterTypes[i]);
        }
        final String[] parameterNames = new String[parameterTypes.length];

        String className = clazz.getName();
        int lastDotIndex = className.lastIndexOf(".");
        className = className.substring(lastDotIndex + 1) + ".class";
        InputStream is = clazz.getResourceAsStream(className);
        try {
            ClassReader classReader = new ClassReader(is);
            classReader.accept(new ClassVisitor(Opcodes.ASM5) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                                 String[] exceptions) {
                    // 只处理指定的方法  
                    Type[] argumentTypes = Type.getArgumentTypes(desc);
                    if (!method.getName().equals(name) || !Arrays.equals(argumentTypes, types)) {
                        return null;
                    }
                    return new MethodVisitor(Opcodes.ASM5) {
                        @Override
                        public void visitLocalVariable(String name, String desc, String signature, Label start,
                                                       Label end, int index) {
                            // 静态方法第一个参数就是方法的参数，如果是实例方法，第一个参数是this  
                            if (Modifier.isStatic(method.getModifiers())) {
                                parameterNames[index] = name;
                            } else if (index > 0 && index <= parameterNames.length) {
                                parameterNames[index - 1] = name;
                            }
                        }
                    };

                }
            }, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return parameterNames;
    }
    
    public static String[] getMethodParameterNames(Method method) {
    	if (!mParameterNamesCacheEnabled) {
    		return getMethodParameterNamesByAsm(method.getDeclaringClass(), method);
    	}
    	return mParameterNamesMap.get(method);
    }
    
    public List<Object> getMethodParameters(Class<?> clazz, Method method, IoSession session, HttpRequest request, HashMap<String, Object> allParameters, com.fenglinga.tinyspring.framework.Controller controller) throws Exception {
        StringBuilder sb = new StringBuilder();
        String[] paramNames = getMethodParameterNames(method);
        Parameter[] parameters = method.getParameters();
        List<Object> params = new ArrayList<Object>();
        for (int i = 0; i < parameters.length && i < paramNames.length; i++) {
            Parameter parameter = parameters[i];
            String paramName = paramNames[i];
            Class<?> type = parameter.getType();
            String className = type.getName();
            Object pv = null;
            RequestBody rb = parameter.getAnnotation(RequestBody.class);
            KeepOriginParameter kop = parameter.getAnnotation(KeepOriginParameter.class);
            if (rb != null) {
                IoBuffer content = (IoBuffer)session.getAttribute("Content");
                if (content != null) {
                    String postContent = new String(content.array(), content.position(), content.remaining(), "UTF-8");
                    if (!isSQLDefendValid(postContent)) {
                        throw new Exception("潜在的攻击请求");
                    }
                    if (className.equals(String.class.getName())) {
                        pv = postContent;
                    } else if (className.equals(JSONObject.class.getName())) {
                        pv = JSON.parseObject(postContent);
                    } else if (className.equals(JSONArray.class.getName())) {
                        pv = JSON.parseArray(postContent);
                    } else {
                        ResolveParameterTypeResult rpt = controller.resolveParameterType(type, paramName, allParameters.get(paramName));
                        if (rpt != null && rpt.resolved) {
                            pv = rpt.value;
                        } else {
                               throw new Exception(className + "类型字段" + paramName + "未处理");
                        }
                    }
                } else {
                    if (rb.required()) {
                        throw new Exception("必填@RequestBody " + className + " " + paramName + "不能为空");
                    }
                }
            } else {
                if (className.equals(VelocityContext.class.getName())) {
                    VelocityContext vc = new VelocityContext();
                    vc.put("application", Constants.Config);
                    vc.put("request", request);
                    vc.put("date", new Date());
                    pv = vc;
                } else if (className.equals(HttpServletResponse.class.getName())) {
                    pv = new HttpServletResponse();
                    session.setAttribute("Response", pv);
                } else if (className.equals(HttpRequest.class.getName())) {
                    pv = request;
                } else if (className.equals(HttpServerHandler.class.getName())) {
                    pv = this;
                } else if (className.equals(IoSession.class.getName())) {
                    pv = session;
                } else if (className.equals(MultipartFile.class.getName())) {
                    pv = allParameters.get(paramName);
                } else if (className.equals(IoBuffer.class.getName())) {
                    IoBuffer content = (IoBuffer)session.getAttribute("Content");
                    pv = content;
                } else if (className.equals(boolean.class.getName())) {
                    pv = allParameters.get(paramName);
                    if (pv == null) {
                        throw new Exception("必填" + className + "类型字段" + paramName + "不能为空");
                    }
                    pv = Boolean.valueOf(String.valueOf(pv));
                } else if (className.equals(byte.class.getName())) {
                    pv = allParameters.get(paramName);
                    if (pv == null) {
                        throw new Exception("必填" + className + "类型字段" + paramName + "不能为空");
                    }
                    pv = Byte.valueOf(String.valueOf(pv));
                } else if (className.equals(char.class.getName())) {
                    pv = allParameters.get(paramName);
                    if (pv == null) {
                        throw new Exception("必填" + className + "类型字段" + paramName + "不能为空");
                    }
                    pv = (char)(int)Integer.valueOf(String.valueOf(pv));
                } else if (className.equals(short.class.getName())) {
                    pv = allParameters.get(paramName);
                    if (pv == null) {
                        throw new Exception("必填" + className + "类型字段" + paramName + "不能为空");
                    }
                    pv = Short.valueOf(String.valueOf(pv));
                } else if (className.equals(int.class.getName())) {
                    pv = allParameters.get(paramName);
                    if (pv == null) {
                        throw new Exception("必填" + className + "类型字段" + paramName + "不能为空");
                    }
                    pv = Integer.valueOf(String.valueOf(pv));
                } else if (className.equals(long.class.getName())) {
                    pv = allParameters.get(paramName);
                    if (pv == null) {
                        throw new Exception("必填" + className + "类型字段" + paramName + "不能为空");
                    }
                    pv = Long.valueOf(String.valueOf(pv));
                } else if (className.equals(float.class.getName())) {
                    pv = allParameters.get(paramName);
                    if (pv == null) {
                        throw new Exception("必填" + className + "类型字段" + paramName + "不能为空");
                    }
                    pv = Float.valueOf(String.valueOf(pv));
                } else if (className.equals(double.class.getName())) {
                    pv = allParameters.get(paramName);
                    if (pv == null) {
                        throw new Exception("必填" + className + "类型字段" + paramName + "不能为空");
                    }
                    pv = Double.valueOf(String.valueOf(pv));
                } else if (className.equals(String.class.getName())) {
                    pv = allParameters.get(paramName);
                    if (pv != null) {
                        if (kop != null) {
                            pv = (String)pv;
                            if (!isSQLDefendValid((String)pv)) {
                                throw new Exception("潜在的攻击请求");
                            }
                        } else {
                            pv = Utils.decodeURLString((String)pv);
                            if (!isSQLDefendValid((String)pv)) {
                                throw new Exception("潜在的攻击请求");
                            }
                        }
                    }
                } else if (className.equals(Byte.class.getName())) {
                    String obj = (String)allParameters.get(paramName);
                    if (obj != null && obj.length() > 0) {
                        pv = Byte.valueOf(obj);
                    } else {
                        pv = null;
                    }
                } else if (className.equals(Character.class.getName())) {
                    String obj = (String)allParameters.get(paramName);
                    if (obj != null && obj.length() > 0) {
                        pv = new Character((char)Integer.valueOf(obj).intValue());
                    } else {
                        pv = null;
                    }
                } else if (className.equals(Boolean.class.getName())) {
                    String obj = (String)allParameters.get(paramName);
                    if (obj != null && obj.length() > 0) {
                        pv = Boolean.valueOf(obj);
                    } else {
                        pv = null;
                    }
                } else if (className.equals(Short.class.getName())) {
                    String obj = (String)allParameters.get(paramName);
                    if (obj != null && obj.length() > 0) {
                        pv = Short.valueOf(obj);
                    } else {
                        pv = null;
                    }
                } else if (className.equals(Integer.class.getName())) {
                    String obj = (String)allParameters.get(paramName);
                    if (obj != null && obj.length() > 0) {
                        pv = Integer.valueOf(obj);
                    } else {
                        pv = null;
                    }
                } else if (className.equals(Long.class.getName())) {
                    String obj = (String)allParameters.get(paramName);
                    if (obj != null && obj.length() > 0) {
                        pv = Long.valueOf(obj);
                    } else {
                        pv = null;
                    }
                } else if (className.equals(Double.class.getName())) {
                    String obj = (String)allParameters.get(paramName);
                    if (obj != null && obj.length() > 0) {
                        pv = Double.valueOf(obj);
                    } else {
                        pv = null;
                    }
                } else if (className.equals(String[].class.getName())) {
                    Object obj = allParameters.get(paramName);
                    if (obj != null && obj instanceof List<?>) {
                        List<?> list = (List<?>)obj;
                        String [] array = list.toArray(new String[0]);
                        for (int j = 0; j < array.length; j++) {
                            String item = array[j];
                            if (kop != null) {
                                if (!isSQLDefendValid(item)) {
                                    throw new Exception("潜在的攻击请求");
                                }
                            } else {
                                item = Utils.decodeURLString(item);
                                if (!isSQLDefendValid(item)) {
                                    throw new Exception("潜在的攻击请求");
                                }
                                array[j] = item;
                            }
                        }
                        pv = array;
                    }
                } else {
                    ResolveParameterTypeResult rpt = controller.resolveParameterType(type, paramName, allParameters.get(paramName));
                    if (rpt != null && rpt.resolved) {
                        pv = rpt.value;
                    } else {
                           throw new Exception(className + "类型字段" + paramName + "未处理");
                    }
                }
            }
            String paramValueLog = objectToString(pv);
            sb.append("[PARAM](").append(className).append(")").append(paramName).append("=>[");
            if (paramValueLog.length() > LOG_MAX_STRING_LENGTH) {
                sb.append(paramValueLog.substring(0, LOG_MAX_STRING_LENGTH)).append("...");
            } else {
                sb.append(paramValueLog);
            }
            sb.append("]\r\n");
            params.add(pv);
        }
        session.setAttribute(PARSE_PARAMETER_LOG, sb.toString().trim());
        return params;
    }
    
    private String objectToString(Object pv) {
        if (pv instanceof HttpRequest) {
            return "HttpRequest@" + Integer.toHexString(pv.hashCode());
        } else if (pv instanceof String[]) {
            return Utils.buildStringArray((String[])pv, ",");
        }
        return String.valueOf(pv);
    }
    
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getParameterAnnotation(Annotation[] anns, Class<A> annotationType) {
        for (Annotation ann : anns) {
            if (annotationType.isInstance(ann)) {
                return (A) ann;
            }
        }
        return null;
    }

    @Override
    public void messageReceived(final IoSession session, Object message) throws Exception {
        if (mShutdown) {
            session.close(true);
            return;
        }
        // LOGGER.debug("messageReceived: " + message);
        // Check that we can service the request context
        if (message instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) message;
            session.setAttribute("HttpRequest", request);
            session.setAttribute(REQUEST_ENTER_TIME, Utils.getMSTime());

            // LOGGER.info(request.toString());
            if (request.getMethod() == HttpMethod.GET) {
                handleRequest(session, request);
            } else if (request.getMethod() == HttpMethod.POST) {
            } else if (request.getMethod() == HttpMethod.OPTIONS) {
                writeResponse(session, request, "OK", "text/html;charset=utf-8", null);
            } else {
                session.close(true);
            }

        } else if (message instanceof IoBuffer) {
            IoBuffer ioBuffer = (IoBuffer) message;
            IoBuffer content = (IoBuffer) session.getAttribute("Content");
            if (content == null) {
                content = IoBuffer.allocate(ioBuffer.array().length);
                content.setAutoExpand(true);
                content.put(ioBuffer);
                session.setAttribute("Content", content);
            } else {
                content.put(ioBuffer);
            }
        } else if (message instanceof HttpEndOfContent) {
            //HttpEndOfContent endOfContent = (HttpEndOfContent) message;
            HttpRequest request = (HttpRequest) session.getAttribute("HttpRequest");
            if (request != null) {
                if (request.getMethod() == HttpMethod.POST) {
                    IoBuffer content = (IoBuffer) session.getAttribute("Content");
                    if (content != null) {
                        content.flip();
                    } else {
                        content = IoBuffer.allocate(0);
                    }
                    handleRequest(session, request);
                } else if (request.getMethod() == HttpMethod.GET) {
                } else {
                    session.close(true);
                }
            }
        }
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        //if (LOG_ENABLED) LOGGER.debug("CLOSED:" + this + "  Session: " + session);
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) {
        //if (LOG_ENABLED) LOGGER.debug("*** IDLE #" + session.getIdleCount(IdleStatus.BOTH_IDLE) + " ***");
        session.close(true);
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) {
        if (cause != null && cause.getStackTrace() != null) {
            cause.printStackTrace();
        }

        session.close(true);
    }
}
