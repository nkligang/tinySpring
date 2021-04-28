package com.fenglinga.tinyspring.framework;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
import com.alibaba.fastjson.JSONObject;
import com.fenglinga.tinyspring.common.Constants;
import com.fenglinga.tinyspring.common.Utils;
import com.fenglinga.tinyspring.framework.annotation.AfterReturning;
import com.fenglinga.tinyspring.framework.annotation.Aspect;
import com.fenglinga.tinyspring.framework.annotation.Controller;
import com.fenglinga.tinyspring.framework.annotation.RequestBody;
import com.fenglinga.tinyspring.framework.annotation.RequestMapping;
import com.fenglinga.tinyspring.framework.annotation.ResponseBody;
import com.fenglinga.tinyspring.framework.annotation.RestController;
import com.fenglinga.tinyspring.framework.annotation.Transactional;
import com.fenglinga.tinyspring.mysql.Db;

public class HttpServerHandler extends IoHandlerAdapter {
    private final static Logger LOGGER = LoggerFactory.getLogger(HttpServerHandler.class);
    private static boolean mShutdown = false;
    protected HashMap<String, String> mContentTypeMap = new HashMap<String, String>();
    private VelocityEngine mVelocityEngine = null;
    private final ScheduledThreadPoolExecutor mExecutor = new ScheduledThreadPoolExecutor(10);
    protected HashMap<String, Method> mRestControllerMethodMap = new HashMap<String, Method>();
    protected HashMap<String, Method> mControllerMethodMap = new HashMap<String, Method>();
    protected List<Method> mAfterReturningMethodList = new ArrayList<Method>();
    /** 是否支持断点续传 */
    private static boolean mRangeSupport = false;

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
                        }
                        if (isController) {
                            mControllerMethodMap.put(v, method);
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
        LOGGER.debug("Found rest controller method:" + mRestControllerMethodMap.size());
        LOGGER.debug("Found controller method:" + mControllerMethodMap.size());
    }
    
    public HashMap<String, Method> getControllerMethodMaps(boolean isRest) {
        return isRest ? mRestControllerMethodMap : mControllerMethodMap;
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
        LOGGER.debug("OPENED:" + this + "  Session: " + session);
    }
    
    protected HttpResponse pageAccess(HttpRequest request, String requestPath) {
        return null;
    }
    
    protected HttpResponse pagePreAccess(HttpRequest request, String requestPath) {
        return null;
    }

    protected void pageNotFound(final IoSession session) {
        HashMap<String, String> headers = new HashMap<String, String>();
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.CLIENT_ERROR_NOT_FOUND, headers);
        session.write(response).addListener(IoFutureListener.CLOSE);
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
    
    public void redirect(IoSession session, String redirectUrl, String setCookie) {
        String result = "";
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "text/html; charset=utf-8");
        headers.put("Content-Length", String.valueOf(Utils.getContentLenth(result)));
        headers.put("Location", redirectUrl);
        if (setCookie != null && setCookie.length() > 0) {
            headers.put("Set-Cookie", setCookie);
        }
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.REDIRECTION_FOUND, headers);
        session.write(response).addListener(IoFutureListener.CLOSE);
    }

    private void writeResponse(IoSession session, HttpRequest request, String content, String contentType) throws Exception {
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

        HashMap<String, String> headers = new HashMap<String, String>();
        if (contentType.length() > 0) {
            headers.put("Content-Type", contentType);
        }
        if (gzipEnabled && buffer != null) {
            headers.put("Content-Encoding", "gzip");
            headers.put("Content-Length", String.valueOf(buffer.remaining()));
        } else {
            headers.put("Content-Length", String.valueOf(Utils.getContentLenth(content)));
        }
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SUCCESS_OK, headers);
        session.write(response);
        if (buffer != null) {
            session.write(buffer).addListener(IoFutureListener.CLOSE);
        }
    }
    
    private void writeResponse(IoSession session, HttpRequest request, HttpServletResponse servletReponse) throws Exception {
        IoBuffer buffer = servletReponse.getBuffer();
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.putAll(servletReponse.getHeaders());
        if (buffer != null) {
            headers.put("Content-Length", String.valueOf(buffer.remaining()));
        }
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SUCCESS_OK, headers);
        session.write(response);
        if (buffer != null) {
            session.write(buffer).addListener(IoFutureListener.CLOSE);
        }
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
        LOGGER.debug("Handle request: " + requestPath);
        
        Method foundRestMethod = mRestControllerMethodMap.get(requestPath);
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
                List<Object> params = getMethodParameters(foundRestMethod, session, request, allParameters);
                controller.setRequest(request);
                controller.setSession(session);
                controller.setParameters(allParameters);

                JSONObject reqObj = controller.onRequest(foundRestMethod, requestPath);
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
                String resultStr = resultObject.toJSONString();
                LOGGER.debug("result: " + resultStr);
                writeResponse(session, request, resultStr, "application/json;charset=utf-8");
                return;
            } else if (result instanceof String) {
                String resultStr = (String)result;
                LOGGER.debug("result: " + resultStr);
                writeResponse(session, request, resultStr, "text/html;charset=utf-8");
                return;
            }
        }
        
        Method foundMethod = mControllerMethodMap.get(requestPath);
        if (foundMethod != null) {
            RequestMapping rm = foundMethod.getAnnotation(RequestMapping.class);
            if (rm == null) {
                return;
            }
            HashMap<String, Object> allParameters = parseAllParameters(session, request);
            List<Object> params = getMethodParameters(foundMethod, session, request, allParameters);
            VelocityContext model = findMethodParameter(params, VelocityContext.class, true);
            
            Object result = null;
            
            Class<?> klass = foundMethod.getDeclaringClass();
            com.fenglinga.tinyspring.framework.Controller controller = (com.fenglinga.tinyspring.framework.Controller)klass.newInstance();
            controller.setRequest(request);
            controller.setSession(session);
            controller.setParameters(allParameters);
            
            JSONObject reqObj = controller.onRequest(foundMethod, requestPath);
            if (!reqObj.isEmpty()) {
                String login_url = Constants.Config.getString("server.login_url");
                redirect(session, login_url, null);
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
                        writeResponse(session, request, resultStr, "text/html;charset=utf-8");
                        return;
                    } else {
                        String redirctPrefix = "redirect:";
                        if (resultStr.startsWith(redirctPrefix)) {
                            String redirectURL = resultStr.substring(redirctPrefix.length());
                            redirect(session, redirectURL, null);
                            return;
                        } else {
                            String templateFile = resultStr;
                            String localFilePath = Constants.AppAssetsPath + "templates/" + templateFile;
                            File resFile = new File(localFilePath);
                            if (resFile.exists()) {
                                Template template = mVelocityEngine.getTemplate(templateFile, "UTF-8");
                                StringWriter writer = new StringWriter();
                                template.merge(model, writer);
                                String resultString = writer.toString();
                                writeResponse(session, request, resultString, "text/html;charset=utf-8");
                                return;
                            }
                        }
                    }
                }
            }
        }
        
        String localFilePath = Constants.AppAssetsPath + "static" + requestPath;
        final File resFile = new File(localFilePath);
        if (resFile.isFile() && resFile.exists()) {
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
            if (rangeStart != 0) {
                headers.put("Content-Length", String.valueOf(rangeEnd - rangeStart + 1));
            } else {
                headers.put("Content-Length", String.valueOf(resFile.length()));
            }
            final long rangeStartBytes = rangeStart;
            headers.put("Last-Modified", Utils.formatTimeMSString(resFile.lastModified(), "EEE, dd MMM yyyy HH:mm:ss 'GMT'"));
            headers.put("ETag", Utils.HashToMD5Hex(localFilePath) + ":0");

            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status, headers);
            session.write(response);

            // Use this for reading the data.
            session.setAttribute("BodyType", "byte[]");
            if (resFile.length() > 4 * 1024 * 1024) {
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
                byte[] buffer = new byte[(int) resFile.length()];
                InputStream inputStream = new FileInputStream(resFile);
                int nRead = 0;
                while ((nRead = inputStream.read(buffer)) != -1) {
                    if (nRead == 0) break;
                    ByteBuffer outbuffer = ByteBuffer.wrap(buffer, 0, nRead);
                    session.write(outbuffer);
                }
                inputStream.close();
                session.close(false);
            }
        } else {
            pageNotFound(session);
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
    
    public static List<MultipartFormData> parseMultipartFormData(String boundaryValue, IoBuffer content) {
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
                        multipartFormDataHeader = new String(contentByteArray, boundaryPartBegin, iOffset + dataBeginBytes.length - boundaryPartBegin);
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
                    HashMap<String, List<String>> parameterMap = Utils.parseURLParametersEx(postData);
                    for (Entry<String, List<String>> entry : parameterMap.entrySet()) {
                        List<String> valueList = entry.getValue();
                        if (valueList.size() == 1) {
                            result.put(entry.getKey(), valueList.get(0));
                        } else {
                            result.put(entry.getKey(), valueList);
                        }
                    }
                }
            }
        } else {
            Map<String, List<String>> paramters = request.getParameters();
            for (Entry<String, List<String>> entry : paramters.entrySet()) {
                List<String> valueList = entry.getValue();
                if (valueList.size() == 1) {
                    result.put(entry.getKey(), valueList.get(0));
                } else {
                    result.put(entry.getKey(), valueList);
                }
            }
        }
        return result;
    }
    
    public List<Object> getMethodParameters(Method method, IoSession session, HttpRequest request, HashMap<String, Object> allParameters) throws Exception {
        Parameter[] parameters = method.getParameters();
        List<Object> params = new ArrayList<Object>();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (parameter.getName().startsWith("arg")) {
                throw new Exception("Unsupport java version");
            }
            Class<?> type = parameter.getType();
            String simpleName = type.getSimpleName();
            String paramName = parameter.getName();
            Object pv = null;
            RequestBody rb = parameter.getAnnotation(RequestBody.class);
            if (rb != null) {
            	IoBuffer content = (IoBuffer)session.getAttribute("Content");
            	if (content != null) {
                    String postContent = new String(content.array(), content.position(), content.remaining(), "UTF-8");
                	if (simpleName.equals("String")) {
                		pv = postContent;
                	} else if (simpleName.equals("JSONObject")) {
                		pv = JSON.parseObject(postContent);
                	} else if (simpleName.equals("JSONArray")) {
                		pv = JSON.parseArray(postContent);
                	}
            	} else {
            		if (rb.required()) {
            			throw new Exception("必填@RequestBody " + simpleName + " " + paramName + "不能为空");
            		}
            	}
            } else {
	            if (simpleName.equals("VelocityContext")) {
	                pv = new VelocityContext();
	            } else if (simpleName.equals("HttpServletResponse")) {
	                pv = new HttpServletResponse();
	                session.setAttribute("Response", pv);
	            } else if (simpleName.equals("HttpRequest")) {
	                pv = request;
	            } else if (simpleName.equals("HttpServerHandler")) {
	                pv = this;
	            } else if (simpleName.equals("IoSession")) {
	                pv = session;
	            } else if (simpleName.equals("MultipartFile")) {
	                pv = allParameters.get(paramName);
	            } else if (simpleName.equals("IoBuffer")) {
	                IoBuffer content = (IoBuffer)session.getAttribute("Content");
	                pv = content;
	            } else if (simpleName.equals("bool")) {
	                pv = allParameters.get(paramName);
	                if (pv == null) {
	                    throw new Exception("必填" + simpleName + "类型字段" + paramName + "不能为空");
	                }
	                pv = Boolean.valueOf(String.valueOf(pv));
	            } else if (simpleName.equals("int")) {
	                pv = allParameters.get(paramName);
	                if (pv == null) {
	                    throw new Exception("必填" + simpleName + "类型字段" + paramName + "不能为空");
	                }
	                pv = Integer.valueOf(String.valueOf(pv));
	            } else if (simpleName.equals("float")) {
	                pv = allParameters.get(paramName);
	                if (pv == null) {
	                    throw new Exception("必填" + simpleName + "类型字段" + paramName + "不能为空");
	                }
	                pv = Float.valueOf(String.valueOf(pv));
	            } else if (simpleName.equals("double")) {
	                pv = allParameters.get(paramName);
	                if (pv == null) {
	                    throw new Exception("必填" + simpleName + "类型字段" + paramName + "不能为空");
	                }
	                pv = Double.valueOf(String.valueOf(pv));
	            } else if (simpleName.equals("String")) {
	                pv = allParameters.get(paramName);
	                if (pv != null) {
	                    pv = Utils.decodeURLString((String)pv);
	                }
	            } else {
	                throw new Exception(simpleName + "类型字段" + paramName + "未处理");
	            }
            }
            LOGGER.debug("type: " + simpleName + " name: " + paramName + " => " + String.valueOf(pv));
            params.add(pv);
        }
        return params;
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

            // LOGGER.info(request.toString());
            if (request.getMethod() == HttpMethod.GET) {
                handleRequest(session, request);
            } else if (request.getMethod() == HttpMethod.POST) {
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
            HttpEndOfContent endOfContent = (HttpEndOfContent) message;
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
            LOGGER.debug("Response End: {}", endOfContent);
        }
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        LOGGER.debug("CLOSED");
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) {
        LOGGER.debug("*** IDLE #" + session.getIdleCount(IdleStatus.BOTH_IDLE) + " ***");
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
