package com.fenglinga.tinyspring.common;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.JarURLConnection;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.MalformedInputException;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.mina.util.Base64;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public final class Utils {
    public static String[] parseStringArray(String str, String split) {
        if (str == null || str.length() == 0)
            return new String[0];
        return str.split(split);
    }
    
    public static Set<String> parseStringSet(String str, String split) {
        Set<String> ret = new HashSet<String>();
        if (str == null || str.length() == 0)
            return ret;
        String[] result = str.split(split);
        int iCount = result.length;
        for (int i = 0; i < iCount; i++) {
            ret.add(result[i]);
        }
        return ret;
    }
    
    public static List<String> parseStringList(String str, String split) {
        List<String> ret = new ArrayList<String>();
        if (str == null || str.length() == 0)
            return ret;
        String[] result = str.split(split);
        int iCount = result.length;
        for (int i = 0; i < iCount; i++) {
            ret.add(result[i]);
        }
        return ret;
    }

    public static int[] parseIntArray(String str, String split) {
        if (str == null || str.length() == 0)
            return new int[0];
        String[] result = str.split(split);
        int iCount = result.length;
        int[] ret = new int[iCount];
        for (int i = 0; i < iCount; i++) {
            ret[i] = parseIntValue(result[i], 0);
        }
        return ret;
    }

    public static long[] parseLongArray(String str, String split) {
        if (str == null || str.length() == 0)
            return new long[0];
        String[] result = str.split(split);
        int iCount = result.length;
        long[] ret = new long[iCount];
        for (int i = 0; i < iCount; i++) {
            ret[i] = parseLongValue(result[i], 0);
        }
        return ret;
    }

    public static List<Long> parseLongArrayEx(String str, String split) {
        List<Long> ret = new ArrayList<Long>();
        if (str == null || str.length() == 0)
            return ret;
        String[] result = str.split(split);
        int iCount = result.length;
        for (int i = 0; i < iCount; i++) {
            ret.add(parseLongValue(result[i], 0));
        }
        return ret;
    }
    
    public static Set<Integer> parseIntSet(String str, String split) {
        Set<Integer> ret = new HashSet<Integer>();
        if (str == null || str.length() == 0)
            return ret;
        String[] result = str.split(split);
        int iCount = result.length;
        for (int i = 0; i < iCount; i++) {
            ret.add(parseIntValue(result[i], 0));
        }
        return ret;
    }

    public static String buildIntegerSet(Set<Integer> arr, String split) {
        StringBuilder result = new StringBuilder();
        int iEntryIndex = 0;
        for (Integer entry : arr) {
            if (iEntryIndex != 0)
                result.append(split);
            result.append(entry);
            iEntryIndex++;
        }
        return result.toString();
    }

    public static String buildStringSet(Set<String> arr, String split) {
        StringBuilder result = new StringBuilder();
        int iEntryIndex = 0;
        for (String entry : arr) {
            if (iEntryIndex != 0)
                result.append(split);
            result.append(entry);
            iEntryIndex++;
        }
        return result.toString();
    }

    public static String buildIntegerArray(List<Integer> arr, String split) {
        StringBuilder result = new StringBuilder();
        int iEntryIndex = 0;
        for (Integer entry : arr) {
            if (iEntryIndex != 0)
                result.append(split);
            result.append(entry);
            iEntryIndex++;
        }
        return result.toString();
    }

    public static String buildIntegerArray(int [] arr, String split) {
        StringBuilder result = new StringBuilder();
        int iEntryIndex = 0;
        for (Integer entry : arr) {
            if (iEntryIndex != 0)
                result.append(split);
            result.append(entry);
            iEntryIndex++;
        }
        return result.toString();
    }

    public static String buildLongArray(List<Long> arr, String split) {
        StringBuilder result = new StringBuilder();
        int iEntryIndex = 0;
        for (Long entry : arr) {
            if (iEntryIndex != 0)
                result.append(split);
            result.append(entry);
            iEntryIndex++;
        }
        return result.toString();
    }

    public static String buildStringArray(List<String> arr, String split) {
        StringBuilder result = new StringBuilder();
        int iEntryIndex = 0;
        for (String entry : arr) {
            if (iEntryIndex != 0)
                result.append(split);
            result.append(entry);
            iEntryIndex++;
        }
        return result.toString();
    }

    public static String buildStringArray(String [] arr, String split) {
        StringBuilder result = new StringBuilder();
        int iEntryIndex = 0;
        for (String entry : arr) {
            if (iEntryIndex != 0)
                result.append(split);
            result.append(entry);
            iEntryIndex++;
        }
        return result.toString();
    }
    
    public static Set<String> convertType(String [] arr) {
        Set<String> result = new HashSet<String>();
        if (arr != null && arr.length != 0) {
            for (String a : arr) {
                result.add(a);
            }
        }
        return result;
    }
    
    public static List<String> convertType(Set<String> arr) {
        List<String> result = new ArrayList<String>();
        for (String a : arr) {
            result.add(a);
        }
        return result;
    }

    public static boolean isValidInt(String value) {
        if (value == null || value.length() == 0) {
            return true;
        } else {
            try {
                Integer.valueOf(value);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    public static boolean isValidIntArray(String str, String split) {
        if (str == null || str.length() == 0)
            return true;
        String[] result = str.split(split);
        int iCount = result.length;
        for (int i = 0; i < iCount; i++) {
            if (!isValidInt(result[i]))
                return false;
        }
        return true;
    }

    public static float[] parseFloatArray(String str, String split) {
        if (str == null || str.length() == 0)
            return new float[0];
        String[] result = str.split(split);
        int iCount = result.length;
        float[] ret = new float[iCount];
        for (int i = 0; i < iCount; i++) {
            ret[i] = parseFloatValue(result[i], 0);
        }
        return ret;
    }

    public static double[] parseDoubleArray(String str, String split) {
        if (str == null || str.length() == 0)
            return new double[0];
        String[] result = str.split(split);
        int iCount = result.length;
        double[] ret = new double[iCount];
        for (int i = 0; i < iCount; i++) {
            ret[i] = parseDoubleValue(result[i], 0);
        }
        return ret;
    }

    public static String getIntArrayString(int[] ints, String split) {
        String res = "";
        int iIndex = 0;
        for (int i : ints) {
            if (iIndex == 0)
                res += i;
            else
                res += split + i;
            iIndex++;
        }
        return res;
    }
    
    public static Set<Integer> getIntCrossSet(String strNew, String strOld, String split) {
        Set<Integer> setNew = parseIntSet(strNew, split);
        Set<Integer> setOld = parseIntSet(strOld, split);
        Set<Integer> ret = new HashSet<Integer>();
        for (Integer v : setNew) {
            if (!setOld.contains(v)) {
                ret.add(v);
            }
        }
        return ret;
    }

    public static int[] distributionPropbability(float[] prop, Random mRandom) {
        int[] indices = new int[prop.length];
        int[] tokens = new int[prop.length];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = -1;
            tokens[i] = -1;
        }
        for (int i = 0; i < indices.length; i++) {
            while (true) {
                float fRandomValue = mRandom.nextFloat();
                int iIndex = -1;
                for (int j = 0; j < prop.length; j++) {
                    if (tokens[j] >= 0) {
                        fRandomValue -= prop[tokens[j]];
                        continue;
                    }
                    if (fRandomValue <= prop[j] || j == prop.length - 1) {
                        iIndex = j;
                        break;
                    } else {
                        fRandomValue -= prop[j];
                    }
                }
                if (iIndex >= 0) {
                    indices[i] = iIndex;
                    tokens[iIndex] = i;
                    break;
                }
            }
        }
        return indices;
    }

    private static Random mGetRandomValueRandom = null;
    public static Random random = new Random();
    public static int getRandom(int min, int max) {
        return min + Math.abs(random.nextInt() % (max - min + 1));
    }
    
    public static int getRandomInIntArray(int[] list){
        if(list.length == 1){
            return list[0];
        }        
        int index = getRandom(0,list.length-1);
        return list[index];
    }

    public static List<Integer> getRandomValues(int iCount) {
        if (mGetRandomValueRandom == null)
            mGetRandomValueRandom = new Random(System.currentTimeMillis());
        List<Integer> ret = new ArrayList<Integer>();
        for (int i = 0; i < iCount; i++) {
            ret.add(Math.abs(mGetRandomValueRandom.nextInt()));
        }
        return ret;
    }

    public static int getRandomValue() {
        if (mGetRandomValueRandom == null)
            mGetRandomValueRandom = new Random(System.currentTimeMillis());
        return Math.abs(mGetRandomValueRandom.nextInt());
    }

    public static byte [] getRandomBytes(int iCount) {
        if (mGetRandomValueRandom == null) {
            mGetRandomValueRandom = new Random(System.currentTimeMillis());
        }
        byte [] bytes = new byte[iCount];
        mGetRandomValueRandom.nextBytes(bytes);
        return bytes;
    }

    public static float getRandomFloatValue() {
        if (mGetRandomValueRandom == null)
            mGetRandomValueRandom = new Random(System.currentTimeMillis());
        return mGetRandomValueRandom.nextFloat();
    }
    
    public static int getRandomFloatsIndex(float [] probabilities) {
        int iLen = probabilities.length;
        if (iLen == 0) return -1;
        float fRandomValue = getRandomFloatValue();
        for (int i = 0; i < iLen; i++) {
            float f = probabilities[i];
            if (fRandomValue < f) {
                return i;
            } else {
                fRandomValue -= f;
            }
        }
        return iLen - 1;
    }
    
    public static int [] getRandomIndex(int iLen, int iExchangeTimes) {
        int [] result = new int[iLen];
        for (int i = 0; i < iLen; i++) {
            result[i] = i;
        }
        for (int i = 0; i < iExchangeTimes; i++) {
            int iRandomValue1 = getRandomValue() % iLen;
            int iRandomValue2 = getRandomValue() % iLen;
            if (iRandomValue1 == iRandomValue2) continue;
            int iTemp = result[iRandomValue1];
            result[iRandomValue1] = result[iRandomValue2];
            result[iRandomValue2] = iTemp;
        }
        return result;
    }

    public class InvertedSort implements Comparator<Integer> {
        public int compare(Integer o1, Integer o2) {
            Integer s1 = (Integer) o1;
            Integer s2 = (Integer) o2;
            return s1 - s2;
        }
    }

    public static int[] sortJointRank(int[] orderBys) {
        int iCount = orderBys.length;
        int iIndex = 0;
        Integer[] iSoredScores = new Integer[iCount];
        for (iIndex = 0; iIndex < iCount; iIndex++) {
            iSoredScores[iIndex] = orderBys[iIndex];
        }
        Arrays.sort(iSoredScores, Collections.reverseOrder());
        int[] iSortedRanks = new int[iCount];
        for (iIndex = 0; iIndex < iCount; iIndex++) {
            iSortedRanks[iIndex] = -1;
            // System.out.println(iSoredScores[iIndex]);
        }
        for (iIndex = 0; iIndex < iCount; iIndex++) {
            if (iSortedRanks[iIndex] >= 0)
                continue;
            iSortedRanks[iIndex] = iIndex + 1;
            int iScore = iSoredScores[iIndex];
            for (int j = iIndex + 1; j < iCount; j++) {
                if (iScore == iSoredScores[j]) {
                    iSortedRanks[j] = iIndex + 1;
                } else {
                    break;
                }
            }
        }
        int[] iRanks = new int[iCount];
        for (iIndex = 0; iIndex < iCount; iIndex++) {
            int iScore = orderBys[iIndex];
            for (int j = 0; j < iCount; j++) {
                if (iScore == iSoredScores[j]) {
                    iRanks[iIndex] = iSortedRanks[j];
                    break;
                }
            }
        }
        return iRanks;
    }

    public static HashMap<String, String> parseURLParameters(String param, String split, String subSplit) {
        return parseURLParameters(param, split, subSplit, false);
    }
    
    public static HashMap<String, String> parseURLParameters(String param, String split, String subSplit, boolean trimEnabled) {
        HashMap<String, String> ret = new HashMap<String, String>();
        if (param == null) return ret;
        String[] pairs = param.split(split);
        int iPairCount = pairs.length;
        for (int i = 0; i < iPairCount; i++) {
            String pair = pairs[i];
            int iPos = pair.indexOf(subSplit);
            if (iPos <= 0) continue;
            String key = pair.substring(0, iPos);
            String value = pair.substring(iPos+1);
            if (trimEnabled) {
                ret.put(key.trim(), value.trim());
            } else {
                ret.put(key, value);
            }
        }
        return ret;
    }

    public static HashMap<String, List<String>> parseURLParametersEx(String str) {
        return parseURLParametersEx(str, "&", "=");
    }
    
    public static HashMap<String, List<String>> parseURLParametersEx(String str, String split, String subSplit) {
        HashMap<String, List<String>> parameters = new HashMap<String, List<String>>();
        String[] params = str.split(split);
        for (int i = 0; i < params.length; i++) {
            String param = params[i];
            int idx = param.indexOf("=");
            if (idx <= 0) continue;
            String name = param.substring(0, idx);
            String value = param.substring(idx + 1);
            if (!parameters.containsKey(name)) {
                parameters.put(name, new ArrayList<String>());
            }
            parameters.get(name).add(value);
        }
        return parameters;
    }

    public static String buildURLParameters(HashMap<String, String> map, String split, String subSplit) {
        String result = "";
        int iEntryIndex = 0;
        for (Entry<String, String> entry : map.entrySet()) {
            if (iEntryIndex != 0)
                result += split;
            result += entry.getKey() + subSplit + entry.getValue();
            iEntryIndex++;
        }
        return result;
    }

    public static float parseFloatValue(String value, float fValueDef) {
        if (value == null || value.length() == 0) {
            return fValueDef;
        } else {
            try {
                fValueDef = Float.valueOf(value);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            return fValueDef;
        }
    }

    public static float parsePercentValue(String value, float fValueDef) {
        if (value == null || value.length() == 0) {
            return fValueDef;
        } else {
            try {
                value = value.replace("%", "");
                fValueDef = Float.valueOf(value) * 0.01f;
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            return fValueDef;
        }
    }

    public static double parseDoubleValue(String value, double fValueDef) {
        if (value == null || value.length() == 0) {
            return fValueDef;
        } else {
            try {
                fValueDef = Double.valueOf(value);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            return fValueDef;
        }
    }

    public static int parseIntValue(String value, int valueDef) {
        if (value == null || value.length() == 0) {
            return valueDef;
        } else {
            try {
                valueDef = Integer.valueOf(value);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            return valueDef;
        }
    }

    public static String parseStringValue(String value, String valueDef) {
        if (value == null || value.length() == 0) {
            return valueDef;
        }
        return value;
    }

    public static short parseShortValue(String value, short valueDef) {
        if (value == null || value.length() == 0) {
            return valueDef;
        } else {
            try {
                valueDef = Short.valueOf(value);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            return valueDef;
        }
    }

    public static boolean parseBooleanValue(String value, boolean valueDef) {
        if (value == null || value.length() == 0) {
            return valueDef;
        } else {
            try {
                valueDef = Boolean.valueOf(value);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            return valueDef;
        }
    }

    public static long parseLongValue(String value, long valueDef) {
        if (value == null || value.length() == 0) {
            return valueDef;
        } else {
            try {
                valueDef = Long.valueOf(value);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            return valueDef;
        }
    }
    
    public static boolean floatEquals(float f1, float f2) {
        return Math.abs(f1 - f2) < 0.0001f;
    }

    public static String getMacAddress() {
        try {
            // InetAddress ip = InetAddress.getLocalHost();
            // System.out.println("Current IP address : " +
            // ip.getHostAddress());

            Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
            while (networks.hasMoreElements()) {
                NetworkInterface network = networks.nextElement();
                byte[] mac = network.getHardwareAddress();

                if (mac != null) {
                    // System.out.print("Current MAC address : ");

                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                    }
                    if (sb.length() == 17)
                        return sb.toString();
                    // System.out.println(sb.toString());
                }
            }
            // } catch (UnknownHostException e) {
            // e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static boolean DownloadFile(String urlString, String localFile) {
        URL url = null;
        URLConnection con = null;
        int i;
        try {
            File targetFile = new File(localFile);
            if (!targetFile.getParentFile().exists()) {
                targetFile.getParentFile().mkdirs();
            }
            url = new URL(urlString);
            con = url.openConnection();
            File file = new File(localFile);
            String absPath = file.getAbsoluteFile().getAbsolutePath();
            BufferedInputStream bis = new BufferedInputStream(con.getInputStream());
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(absPath));
            while ((i = bis.read()) != -1) {
                bos.write(i);
            }
            bos.flush();
            bos.close();
            bis.close();
            System.out.println("save file: " + targetFile.getCanonicalPath());
            return true;
        } catch (MalformedInputException malformedInputException) {
            malformedInputException.printStackTrace();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        return false;
    }

    public static String DownloadString(String urlString) {
        URL url = null;
        URLConnection connection = null;
        try {
            url = new URL(urlString);
            connection = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));// 设置编码,否则中文乱码
            String line = "";
            StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return sb.toString();
        } catch (MalformedInputException malformedInputException) {
            malformedInputException.printStackTrace();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        return null;
    }

    /**
     * <pre>
     * 获取当前时间，以毫秒为单位
     * </pre>
     */
    public static long getMSTime() {
        return System.currentTimeMillis();
    }

    /**
     * <pre>
     * 获取当前时间，以秒为单位
     * </pre>
     */
    public static long getTimeNow() {
        return System.currentTimeMillis() / 1000;
    }

    public static String formatTimeString(long time) {
        return formatTimeString(time, "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * <pre>
     * 根据时间(以秒为单位)得到特定格式的时间字符串
     * 注意：这里处理了夏令时的问题
     * </pre>
     */
    public static String formatTimeString(long time, String format) {
        DateFormat fmt = new SimpleDateFormat(format);
        Date date = new Date();
        date.setTime(time * 1000);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int iDstOffset = cal.get(Calendar.DST_OFFSET);
        if (iDstOffset > 0) {
            date.setTime(time * 1000 - iDstOffset);
        }
        return fmt.format(date);
    }

    /**
     * <pre>
     * 根据时间格式串获取时间(以秒为单位)
     * 注意：这里处理了夏令时的问题
     * </pre>
     */
    public static long getTime(String timeString, String formatString) {
        try {
            DateFormat fmt = new SimpleDateFormat(formatString);
            Date date = fmt.parse(timeString);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int iDstOffset = cal.get(Calendar.DST_OFFSET);
            if (iDstOffset > 0) {
                return (date.getTime() + iDstOffset) / 1000;
            } else {
                return (date.getTime()) / 1000;
            }
        } catch (Exception e) {
            System.out.println("Call function error: getTime(" + timeString + ", " + formatString + ")");
            e.printStackTrace();
            return 0;
        }
    }

    /** 获取当前时间的当天的凌晨0点的时间(毫秒) */
    public static long getDayBeginTime() {
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date newDate = cal.getTime();
        return newDate.getTime();
    }

    /** 获取某个时间的当天的凌晨0点的时间(毫秒) */
    public static long getDayBeginTime(long curTimeMS) {
        Date date = new Date(curTimeMS);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date newDate = cal.getTime();
        return newDate.getTime();
    }

    /** 获取当前时间的所在的周的第一天的凌晨0点的时间(毫秒) */
    public static long getWeekBeginTime() {
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        cal.add(Calendar.DAY_OF_YEAR, -dayOfWeek + 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date newDate = cal.getTime();
        return newDate.getTime();
    }

    /** 获取某个时间的所在的月的第一天的凌晨0点的时间(毫秒) */
    public static long getMonthBeginTime(long curTimeMS) {
        Date date = new Date(curTimeMS);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        cal.add(Calendar.DAY_OF_YEAR, -dayOfMonth+1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date newDate = cal.getTime();
        return newDate.getTime();
    }

    /** 获取当前时间的所在的月的第一天的凌晨0点的时间(毫秒) */
    public static long getMonthBeginTime() {
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        cal.add(Calendar.DAY_OF_YEAR, -dayOfMonth+1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date newDate = cal.getTime();
        return newDate.getTime();
    }

    /** 获取给定时间(毫秒)的日数 */
    public static int getDayOfYear(long t) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(t);
        return cal.get(Calendar.DAY_OF_YEAR);
    }

    /** 获取给定时间(毫秒)的星期数 */
    public static int getWeekOfYear(long t) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(t);
        return cal.get(Calendar.WEEK_OF_YEAR);
    }

    /** 获取给定时间(毫秒)的星期数 */
    public static int getDayOfMonth(long t) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(t);
        return cal.get(Calendar.DAY_OF_MONTH);
    }

    public static long getTime(Date date) {
        return date.getTime() / 1000;
    }
    
    /** 获取今天星期几,为了和客户端对上，稍作修改 */
    public static int getDayOfWeek() {
        Calendar cal = Calendar.getInstance();    
        int day = cal.get(Calendar.DAY_OF_WEEK);
        day -= 2;
        if(day < 0){
            day += 7;
        }
        return day;
    }
    
    /**
     * 根据毫秒数得到年月日
     * @param ms
     * @return
     */
    public static String getYearMonthDay(long ms){
        return formatTimeString(ms/1000, "yyyy-MM-dd");
    }

    private static Random mgenerateKeyStringRandom = null;

    public static String generateKeyString() {
        String ret = "";
        Long now = Utils.getMSTime();
        ret += Long.toHexString(now);
        if (mgenerateKeyStringRandom == null)
            mgenerateKeyStringRandom = new Random(now);
        ret += Long.toHexString(mgenerateKeyStringRandom.nextLong());
        return ret;
    }
    
    public static String generateCharacter(int length, String alphabet) {
        StringBuilder sb = new StringBuilder();
        if (mgenerateKeyStringRandom == null)
            mgenerateKeyStringRandom = new Random(Utils.getMSTime());
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(Math.abs(mgenerateKeyStringRandom.nextInt()) % alphabet.length()));
        }
        return sb.toString();
    }

    public static long convertToLong(float fValue) {
        return (long) fValue;
    }

    public static int convertToInteger(float fValue) {
        return (int) fValue;
    }

    public static int convertToInteger(double fValue) {
        return (int) fValue;
    }
    
    public static void AddZipFileRecurse(ZipOutputStream zos, String root, String relRoot, String file, byte data[], int dataSize, String separator) throws Exception
    {
        String relFile = relRoot.length() == 0 ? file : relRoot + separator + file;
        String absFile = relFile.length() == 0 ? root : root + separator + relFile;
        File f = new File(absFile);
        if(f.isDirectory())
        {
            relRoot += relRoot.length() == 0 ? file : separator + file;
            String files[] = f.list();
            for (int i = 0; i < files.length; i++) {
                AddZipFileRecurse(zos, root, relRoot, files[i], data, dataSize, separator);
            }
        }
        else //it is just a file
        {
            System.out.println("Adding: " + relFile);
            FileInputStream fi = new FileInputStream(f);
            BufferedInputStream origin = new BufferedInputStream(fi, dataSize);
            ZipEntry entry = new ZipEntry(relFile);
            zos.putNextEntry(entry);
            int count;
            while ((count = origin.read(data, 0, dataSize)) != -1) {
                zos.write(data, 0, count);
                zos.flush();
            }
            zos.closeEntry();
            origin.close();
        }
    }
    
    public static void AddZipFileData(ZipOutputStream zos, String root, String relRoot, String file, byte data[], int dataSize, String separator) throws Exception
    {
        String relFile = relRoot.length() == 0 ? file : relRoot + separator + file;
        ZipEntry entry = new ZipEntry(relFile);
        zos.putNextEntry(entry);
        zos.write(data, 0, dataSize);
        zos.flush();
        zos.closeEntry();
    }

    public static int getContentLenth(StringBuilder content) {
        String str = content.toString();
        String strASCII = null;
        try {
            strASCII = new String(str.getBytes("UTF-8"), "US-ASCII");
            return strASCII.length();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int getContentLenth(String content) {
        String strASCII = null;
        try {
            strASCII = new String(content.getBytes("UTF-8"), "US-ASCII");
            return strASCII.length();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static String encodeURLString(String str) {
        if (str == null)
            return "";
        try {
            return java.net.URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return str;
        }
    }

    public static String decodeURLString(String str) {
        if (str == null)
            return "";
        try {
            return java.net.URLDecoder.decode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return str;
        }
    }

    /**
     * UTF8编码字符串计算MD5值(十六进制编码字符串)
     * 
     * @return result
     */
    public static String HashToMD5Hex(String sourceStr) {
        String signStr = "";
        try {
            byte[] bytes = sourceStr.getBytes("utf-8");
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(bytes);
            byte[] md5Byte = md5.digest();

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < md5Byte.length; i++) {
                //循环每个字符 将计算结果转化为正整数;
                int digestInt = md5Byte[i] & 0xff;
                //将10进制转化为较短的16进制
                String hexString = Integer.toHexString(digestInt);
                //转化结果如果是个位数会省略0,因此判断并补0
                if (hexString.length() < 2) {
                    sb.append(0);
                }
                //将循环结果添加到缓冲区
                sb.append(hexString);
            }
            //返回整个结果
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return signStr.toLowerCase();
    }

    /**
     * UTF8编码字符串计算SHA1值(十六进制编码字符串)
     * 
     * @return result
     */
    public static String HashToSHA1Hex(String sourceStr) {
        String signStr = "";
        try {
            byte[] bytes = sourceStr.getBytes("utf-8");
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(bytes);
            byte[] mdByte = md.digest();
            
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mdByte.length; i++) {
                //循环每个字符 将计算结果转化为正整数;
                int digestInt = mdByte[i] & 0xff;
                //将10进制转化为较短的16进制
                String hexString = Integer.toHexString(digestInt);
                //转化结果如果是个位数会省略0,因此判断并补0
                if (hexString.length() < 2) {
                    sb.append(0);
                }
                //将循环结果添加到缓冲区
                sb.append(hexString);
            }
            //返回整个结果
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return signStr.toLowerCase();
    }

    private static ScriptEngine javaScriptEngine = null;

    public synchronized static double CalculateExpression(String expr) {
        if (expr == null || expr.length() == 0) {
            return 0;
        }
        if (javaScriptEngine == null) {
            ScriptEngineManager factory = new ScriptEngineManager();
            javaScriptEngine = factory.getEngineByName("JavaScript");
        }
        try {
            return Double.valueOf(String.valueOf(javaScriptEngine.eval(expr)));
        } catch (Exception e) {
        }
        return 0;
    }

    private static String reg = "(?:')|(?:--)|(/\\*(?:.|[\\n\\r])*?\\*/)|" + "(\\b(select|update|and|or|delete|insert|trancate|char|into|substr|ascii|declare|exec|count|master|into|drop|execute)\\b)";
    private static Pattern sqlPattern = Pattern.compile(reg, Pattern.CASE_INSENSITIVE);

    public static boolean isSQLDefendValid(String str) {
        if (sqlPattern.matcher(str).find()) {
            return false;
        }
        return true;
    }

    public static String GetWorkDir() {
        String contextPath = Thread.currentThread().getContextClassLoader().getResource(".").getPath();
        File contextFile = new File(contextPath);
        contextPath = contextFile.getAbsolutePath() + "/";
        System.out.println("Context path: " + contextPath);
        return contextPath;
    }

    public static String LoadStringFromFile(String file) {
        InputStream inputStream = null;
        try {
            File resFile = new File(file);
            if (resFile.exists()) {
                inputStream = new FileInputStream(resFile);
            }
            if (inputStream == null)
                return null;
            InputStreamReader reader = null;
            BufferedReader br = null;

            reader = new InputStreamReader(inputStream, "UTF-8");
            br = new BufferedReader(reader);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\r\n");
            }
            System.out.println("load file: " + resFile.getCanonicalPath());
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    public static String LoadStringFromFile(URL configURL) {
        InputStream inputStream = null;
        try {
            inputStream = configURL.openStream();
            if (inputStream == null)
                return null;
            InputStreamReader reader = null;
            BufferedReader br = null;

            reader = new InputStreamReader(inputStream, "UTF-8");
            br = new BufferedReader(reader);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\r\n");
            }
            System.out.println("load file: " + configURL);
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    public static boolean SaveStringToFile(String file, String content) {
        File targetFile = new File(file);
        if (!targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }
        Writer out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetFile), "UTF-8"));
            out.write(content);
            System.out.println("save file: " + targetFile.getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    private static char SSEncDec_GetKeyFromChar(char nChar) {
        if (nChar == '-') {
            return 63;
        } else if (nChar == '_') {
            return 62;
        } else if (nChar < 58) {// 48-57 is '0'-'9', index is 52-61
            return (char) (nChar + 4);
        } else if (nChar < 91) {// 65-91 is 'A'-'Z', index is 26-51
            return (char) (nChar - 39);
        } else {// 97-122 is 'a'-'z', index is 0-25
            return (char) (nChar - 97);
        }
    }

    public static String SSEncDec_Blob2ByteArray(String blob) {
        int nBlobPos = 0; // current index of blob
        int nSPos = 0; // current index of string
        int nBitsNotSet = 8; // how many bits not set in the current s[nSPos]
        char nKeyIndex; // the index of key calculated using

        int blobLen = blob.length();
        // Calculate string length and allocate memory
        // It's ok to cut off the remainder, and we need space for the
        // termination character.
        int nStrLength = (blobLen * 6 / 8);

        byte[] s = new byte[nStrLength];
        // Initialize the string.
        for (int k = 0; k < nStrLength; ++k) {
            s[k] = 0;
        }

        for (nBlobPos = 0; nBlobPos < blobLen; nBlobPos++) {
            // Each character in the blob represents 6 bits in the decoded
            // string.
            nKeyIndex = SSEncDec_GetKeyFromChar(blob.charAt(nBlobPos));

            // If "a" represents the bits of the key from the first character of
            // the blob, "b" the bits of the key from the second character in
            // the blob, "b" the bits of the key from the third character in the
            // blob, etc.
            // Then the decoded string would be comprised as follows:
            // s[0] = bbaaaaaa;
            // s[1] = ccccbbbb;
            // s[2] = ddddddcc;
            // s[3] = ffeeeeee;
            // .
            // .
            // .

            // Reconstruct the string's character value. First fill in it's low
            // order bits first. Then shift to the high order bits that haven't
            // been set yet.
            s[nSPos] |= nKeyIndex << (8 - nBitsNotSet);

            if (nBitsNotSet > 6) {
                // The low order 6 bits of this string character have been set.
                // Only the top 2 bits are left.
                nBitsNotSet -= 6;
            } else {
                if (nSPos < (nStrLength - 1)) {
                    // There may be bits in the key that haven't been used. Go
                    // to
                    // the string's next character and place the remaining bits
                    // from the key into the character's low order bits. When
                    // nBitsNotSet is 6 all of the bits of the key have been
                    // used
                    // and this code will not set anything in the next
                    // character.
                    s[++nSPos] |= nKeyIndex >> nBitsNotSet;

                    // (6 - nBitsNotSet) low order bits have been set in the
                    // string's current character.

                    // Increment in order to lineup with the high order bits of
                    // the
                    // string's character that haven't been set yet.
                    nBitsNotSet += 2;
                }
                // else {}
                // we filled all char in s, so what's left is just padding bits,
                // ignore them
            }
        } // End of for loop.
        return new String(s);
    }

    private static char SSEncDec_GetCharFromKeyByIndex(int nKeyIndex) {
        if (nKeyIndex < 26) {// key index 0-25 is a-z
            return (char) (nKeyIndex + 97); // convert 0...25 to a...z
        } else if (nKeyIndex < 52) {// key index 26-51 is A-Z
            return (char) (nKeyIndex + 39); // convert 26...51 to A...Z
        } else if (nKeyIndex < 62) {// key index 52-61 is 0-9
            return (char) (nKeyIndex - 4); // convert 52...61 to 0...9
        } else if (nKeyIndex == 62) {// key index 62 is '_'
            return '_';
        } else {// key index 63 is '-'
            return '-';
        }
    }

    public static String SSEncDec_ByteArray2Blob(String s) {
        int nBlobPos = 0; // current index of blob
        int nSPos = 0; // current index of string
        int nBitsNotUsed = 8; // how many bits not used in the current s[nSPos]
        int nKeyIndex = 0; // the index of key calcualted using s

        // calculate blob length and allocate memory
        int len = s.length();
        int nBlobLength = len * 8 / 6;
        if (nBlobLength != 0) {// has remainder, need one more char to hold it
            nBlobLength += 1; // remainder
        } else {
            return "";
        }

        byte[] sBlob = new byte[nBlobLength];
        for (int k = 0; k < nBlobLength; ++k) {
            sBlob[k] = 0;
        }

        // -> go though each char in s, calculate a key index using 6 of its
        // bits at a time,
        // use that index to get a char from key, these chars will form the blob
        // -> go though s from left to right, but get bits from each char in s
        // from right to left

        while (nSPos < len) {
            // get next 6 not used bits from s

            // first shift not used bits in current pos all the way to right
            nKeyIndex = s.charAt(nSPos) >> (8 - nBitsNotUsed);

            if (nBitsNotUsed < 6) {// not enough bits in current pos
                if (++nSPos < len) {// there's still char in the next pos, so
                                    // use its bits (nSPos points to next pos
                                    // now)
                                    // get 6-nBitsNotUsed number of bits from
                                    // next char
                                    // to do so, we first shift the char in next
                                    // pos to left nBitsNotUsed times
                                    // then | with leftover bits to form a 6 bit
                                    // number
                    nKeyIndex |= (s.charAt(nSPos) << nBitsNotUsed);
                    nBitsNotUsed += 2; // since 6-nBitsNotUsed bits is used from
                                        // next char, 8-(6-nBitsNotUsed) will be
                                        // left there
                }
                // else
                // there's no next char, just use what we have left in the
                // current pos (nSPos is now pointing pass the end of s)
            } else {// enough bits not used in current pos
                nBitsNotUsed -= 6; // we used 6 more bits from current pos

                if (nBitsNotUsed == 0) {// used up all the bits in current pos,
                                        // so go to next
                    nBitsNotUsed = 8;
                    nSPos++;
                }
            }

            // use right most 6 bits as key index
            nKeyIndex &= 63; // & with 00111111(63) to take only the right most
                                // 6 bits

            // then use index to get a char from key to fill the blob
            sBlob[nBlobPos++] = (byte) SSEncDec_GetCharFromKeyByIndex(nKeyIndex);
        }
        if (nBitsNotUsed == 8 && nBlobLength != 1)// add by heyi
            sBlob[nBlobPos] = (byte) SSEncDec_GetCharFromKeyByIndex(0);
        return new String(sBlob);
    }
    
    public static String EncodeBase64(String src) {
        final Base64 base64 = new Base64();
        try {
            final byte[] textByte = src.getBytes("UTF-8");
            return new String(base64.encode(textByte), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }
    }
    
    public static String EncodeBase64(byte[] textByte) {
        final Base64 base64 = new Base64();
        try {
            return new String(base64.encode(textByte), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }
    }
    
    public static String DecodeBase64(String src) {
        final Base64 base64 = new Base64();
        try {
            final byte[] textByte = src.getBytes("UTF-8");
            return new String(base64.decode(textByte), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static boolean IsVersionValid(String request, String current) {
        boolean bVersionValid = true;
        int[] appVersion = Utils.parseIntArray(request, "\\.");
        int[] curVersion = Utils.parseIntArray(current, "\\.");
        int appVersionLen = appVersion.length;
        int curVersionLen = curVersion.length;
        if (appVersionLen != curVersionLen) {
            // 版本格式不匹配
            bVersionValid = false;
        } else {
            for (int i = 0; i < appVersionLen; i++) {
                if (appVersion[i] < curVersion[i]) {
                    bVersionValid = false;
                    break;
                } else if (appVersion[i] > curVersion[i]) {
                    break;
                }
            }
        }
        return bVersionValid;
    }

    public static boolean isNeedUpgrade(String request, String current) {
        boolean bNeedUpgrade = false;
        int[] appVersion = Utils.parseIntArray(request, "\\.");
        int[] curVersion = Utils.parseIntArray(current, "\\.");
        int appVersionLen = appVersion.length;
        int curVersionLen = curVersion.length;
        if (appVersionLen != curVersionLen) {
            // 版本格式不匹配
            bNeedUpgrade = true;
        } else {
            for (int i = 0; i < appVersionLen; i++) {
                if (appVersion[i] < curVersion[i]) {
                    bNeedUpgrade = true;
                    break;
                } else if (appVersion[i] > curVersion[i]) {
                    break;
                }
            }
        }
        return bNeedUpgrade;
    }

    public static String getNetworkInfo() {
        StringBuilder sb = new StringBuilder();
        try {
            Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
            while (nifs.hasMoreElements()) {
                NetworkInterface nif = nifs.nextElement();
                List<InterfaceAddress> ias = nif.getInterfaceAddresses();
                if (ias.size() == 0)
                    continue;
                for (InterfaceAddress ia : ias) {
                    InetAddress iad = ia.getAddress();
                    byte[] addr = iad.getAddress();
                    if (addr.length != 4)
                        continue;
                    if (addr[0] == 127 && addr[1] == 0 && addr[2] == 0 && addr[3] == 1)
                        continue;
                    sb.append(iad.toString()).append("\n");
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
    
    public static String formatString(String orig, int iLen) {
        int len = orig.length();
        if (len < iLen) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < (iLen - len); i++)
                sb.append(" ");
            sb.append(orig);
            return sb.toString();
        } else {
            return orig;
        }
    }
    
    public static String formatTimeMSString(long timeMS, String format) {
        Date date = new Date();
        date.setTime(timeMS);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT")); // 设置时区为GMT
        return sdf.format(cal.getTime());
    }
    

    public static boolean saveBytesToFile(String folder, String fileName, byte[] body, int off, int len) {
        try {
            File dir = new File(folder);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(folder + File.separator + fileName);
            String absPath = file.getAbsoluteFile().getAbsolutePath();
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(absPath));
            bos.write(body, off, len);
            bos.flush();
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public static boolean moveFileToDir(String srcFile, String destPath) {
        // File (or directory) to be moved
        File file = new File(srcFile);
        // Destination directory
        File dir = new File(destPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        // Move file to new directory
        return file.renameTo(new File(dir, file.getName()));
    }

    public static boolean moveFile(String srcFile, String destFile) {
        // File (or directory) to be moved
        File file = new File(srcFile);
        // Destination directory
        File dest = new File(destFile);
        if (dest.exists()) {
            if (!dest.delete()) {
                return false;
            }
        }
        File dir = dest.getParentFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        // Move file to new directory
        return file.renameTo(dest);
    }
        
    public static String getFileExt(String file) {
        int dotIndex = file.lastIndexOf(".");
        if (dotIndex >= 0) {
            return file.substring(dotIndex + 1);
        }
        return "";
    }
    
    public static long getFileLength(String path) {
        File file = new File(path);
        if (file.exists() && file.isFile()) {
            return file.length();
        }
        return 0;
    }
    
    public static boolean isFileExist(String path) {
        File file = new File(path);
        return file.exists();
    }
    
    public static String getFileLengthInShort(long length) {
        if (length >= (1024*1024*1024)) {
            return String.format("%.2f GB", (float)length/1073741824);
        } else if (length >= (1024*1024) && length < (1024*1024*1024)) {
            return String.format("%.2f MB", (float)length/1048576);
        } else if (length >= (1024) && length < (1024*1024)) {
            return String.format("%.2f KB", (float)length/1024);
        }
        return String.format("%d Bytes", (int)length);
    }
    
    public static class CommandInfo {
        public int exitValue;
        public String inputString = "";
        public String errorString = "";
    };
    public static CommandInfo executeCommand(String command)
    {
        try
        {
            CommandInfo info = new CommandInfo();
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec(command);
            System.out.println(command);
            {
                InputStream stdin = proc.getInputStream();
                InputStreamReader isr = new InputStreamReader(stdin);
                BufferedReader br = new BufferedReader(isr);
                String line = null;
                while ( (line = br.readLine()) != null) {
                    info.inputString += line + "\n";
                }
            }
            {
                InputStream stderr = proc.getErrorStream();
                InputStreamReader isr = new InputStreamReader(stderr);
                BufferedReader br = new BufferedReader(isr);
                String line = null;
                while ( (line = br.readLine()) != null) {
                    info.errorString += line + "\n";
                }
            }
            if (info.inputString.length() > 0)
                System.out.println(info.inputString);
            if (info.errorString.length() > 0)
                System.out.println(info.errorString);
            info.exitValue = proc.waitFor();
            System.out.println("Process exitValue: " + info.exitValue);
            return info;
        } catch (Throwable t){
            t.printStackTrace();
        }
        return null;
    }
    
    public static boolean setSystemTime(long currentTime) {
        //Operating system name
        String osName = System.getProperty("os.name");
        String cmd = "";
        try {
            if (osName.matches("^(?i)Windows.*$")) {// Window 系统
                // 格式 HH:mm:ss
                cmd = " cmd /c time " + Utils.formatTimeString(currentTime, "HH:mm:ss");
                Runtime.getRuntime().exec(cmd);
                System.out.println("exec:" + cmd);
                // 格式：yyyy-MM-dd
                cmd = " cmd /c date " + Utils.formatTimeString(currentTime, "yyyy-MM-dd");
                Runtime.getRuntime().exec(cmd);
                System.out.println("exec:" + cmd);
            } else {// Linux 系统
                // 格式：yyyyMMdd
                cmd = " date -s " + Utils.formatTimeString(currentTime, "yyyyMMdd");
                Runtime.getRuntime().exec(cmd);
                System.out.println("exec:" + cmd);
                // 格式 HH:mm:ss
                cmd = " date -s " + Utils.formatTimeString(currentTime, "HH:mm:ss");
                Runtime.getRuntime().exec(cmd);
                System.out.println("exec:" + cmd);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public static boolean adjustServerTime() {
        String timeURL = String.format("http://api.k780.com/?app=life.time&appkey=%s&sign=%s&format=json", "27407", "86ad62b814dde935c9a50fbcb6ee645d");
        String timeResult = Utils.DownloadString(timeURL);
        if (timeResult == null || timeResult.length() == 0) {
            return false;
        }
        JSONObject timeObject = JSON.parseObject(timeResult);
        String success = timeObject.getString("success");
        if (!success.equals("1")) {
            return false;
        }
        JSONObject resultObject = timeObject.getJSONObject("result");
        long timestamp = resultObject.getLongValue("timestamp");
        return setSystemTime(timestamp);
    }
    
    public static boolean SaveBytesToFile(String file, byte[] body, int off, int len) {
        try {
            File targetFile = new File(file);
            if (!targetFile.getParentFile().exists()) {
                targetFile.getParentFile().mkdirs();
            }
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            bos.write(body, off, len);
            bos.flush();
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
    
    public static byte[] createChecksum(String filename) throws Exception {
        InputStream fis =  new FileInputStream(filename);
        
        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;
        
        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);
        
        fis.close();
        return complete.digest();
    }
    
    // see this How-to for a faster way to convert
    // a byte array to a HEX string
    public static String getMD5Checksum(String filename) throws Exception {
        byte[] b = createChecksum(filename);
        String result = "";
        
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
        }
        return result;
    }
    
    public static void copyFileUsingFileStreams(File source, File dest) throws IOException {    
        InputStream input = null;    
        OutputStream output = null;    
        try {
            input = new FileInputStream(source);
            output = new FileOutputStream(dest);        
            byte[] buf = new byte[1024];        
            int bytesRead;        
            while ((bytesRead = input.read(buf)) != -1) {
                output.write(buf, 0, bytesRead);
            }
        } finally {
            input.close();
            output.close();
        }
    }
    
    /**
     * 从包package中获取所有的Class
     *
     * @param pack
     * @return
     */
    public static Set<Class<?>> getClasses(String pack) {
        // 第一个class类的集合
        Set<Class<?>> classes = new LinkedHashSet<Class<?>>();
        // 是否循环迭代
        boolean recursive = true;
        // 获取包的名字 并进行替换
        String packageName = pack;
        String packageDirName = packageName.replace('.', '/');
        // 定义一个枚举的集合 并进行循环来处理这个目录下的things
        Enumeration<URL> dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
            // 循环迭代下去
            while (dirs.hasMoreElements()) {
                // 获取下一个元素
                URL url = dirs.nextElement();
                // 得到协议的名称
                String protocol = url.getProtocol();
                // 如果是以文件的形式保存在服务器上
                if ("file".equals(protocol)) {
                    // 获取包的物理路径
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    // 以文件的方式扫描整个包下的文件 并添加到集合中
                    findAndAddClassesInPackageByFile(packageName, filePath, recursive, classes);
                } else if ("jar".equals(protocol)) {
                    // 如果是jar包文件
                    // 定义一个JarFile
                    // System.err.println("jar类型的扫描");
                    JarFile jar;
                    try {
                        // 获取jar
                        jar = ((JarURLConnection) url.openConnection()).getJarFile();
                        // 从此jar包 得到一个枚举类
                        Enumeration<JarEntry> entries = jar.entries();
                        // 同样的进行循环迭代
                        while (entries.hasMoreElements()) {
                            // 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
                            JarEntry entry = entries.nextElement();
                            String name = entry.getName();
                            // 如果是以/开头的
                            if (name.charAt(0) == '/') {
                                // 获取后面的字符串
                                name = name.substring(1);
                            }
                            // 如果前半部分和定义的包名相同
                            if (name.startsWith(packageDirName)) {
                                int idx = name.lastIndexOf('/');
                                // 如果以"/"结尾 是一个包
                                if (idx != -1) {
                                    // 获取包名 把"/"替换成"."
                                    packageName = name.substring(0, idx).replace('/', '.');
                                }
                                // 如果可以迭代下去 并且是一个包
                                if ((idx != -1) || recursive) {
                                    // 如果是一个.class文件 而且不是目录
                                    if (name.endsWith(".class") && !entry.isDirectory()) {
                                        // 去掉后面的".class" 获取真正的类名
                                        String className = name.substring(packageName.length() + 1, name.length() - 6);
                                        try {
                                            // 添加到classes
                                            classes.add(Class.forName(packageName + '.' + className));
                                        } catch (ClassNotFoundException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        // log.error("在扫描用户定义视图时从jar包获取文件出错");
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("不支持的协议：" + protocol);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return classes;
    }
    
    /**
     * 以文件的形式来获取包下的所有Class
     *
     * @param packageName
     * @param packagePath
     * @param recursive
     * @param classes
     */
    public static void findAndAddClassesInPackageByFile(String packageName, String packagePath, final boolean recursive,
            Set<Class<?>> classes) {
        // 获取此包的目录 建立一个File
        File dir = new File(packagePath);
        // 如果不存在或者 也不是目录就直接返回
        if (!dir.exists() || !dir.isDirectory()) {
            // log.warn("用户定义包名 " + packageName + " 下没有任何文件");
            return;
        }
        // 如果存在 就获取包下的所有文件 包括目录
        File[] dirfiles = dir.listFiles(new FileFilter() {
            // 自定义过滤规则 如果可以循环(包含子目录) 或则是以.class结尾的文件(编译好的java类文件)
            public boolean accept(File file) {
                return (recursive && file.isDirectory()) || (file.getName().endsWith(".class"));
            }
        });
        // 循环所有文件
        for (File file : dirfiles) {
            // 如果是目录 则继续扫描
            if (file.isDirectory()) {
                findAndAddClassesInPackageByFile(packageName + "." + file.getName(), file.getAbsolutePath(), recursive,
                        classes);
            } else {
                // 如果是java类文件 去掉后面的.class 只留下类名
                String className = file.getName().substring(0, file.getName().length() - 6);
                try {
                    // 添加到集合中去
                    // classes.add(Class.forName(packageName + '.' + className));
                    // 经过回复同学的提醒，这里用forName有一些不好，会触发static方法，没有使用classLoader的load干净
                    classes.add(
                            Thread.currentThread().getContextClassLoader().loadClass(packageName + '.' + className));
                } catch (ClassNotFoundException e) {
                    // log.error("添加用户自定义视图类错误 找不到此类的.class文件");
                    e.printStackTrace();
                }
            }
        }
    }
}
