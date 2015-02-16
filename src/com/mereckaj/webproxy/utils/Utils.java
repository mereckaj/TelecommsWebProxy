package com.mereckaj.webproxy.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.logging.Level;

import org.json.simple.JSONObject;

import com.mereckaj.webproxy.CacheInfoObject;
import com.mereckaj.webproxy.ProxyLogLevel;
import com.mereckaj.webproxy.ProxyLogger;

/**
 * This class is a general utility class that holds all of the methods that were
 * not suited for other classes
 * 
 * This method contains file opening, writing and date getting methods Nothing
 * too exciting
 * 
 * @author julius
 * 
 */
public class Utils {
    public static void appendTolFile(File f,String data){
	try {
	    FileWriter fw = new FileWriter(f);
	    fw.append(data);
	    fw.flush();
	    fw.close();
	} catch (IOException e) {
	    ProxyLogger.getInstance().log(ProxyLogLevel.EXCEPTION, "Couldn't write cached item to file");
	}
    }

    @SuppressWarnings("unchecked")
    public static JSONObject convertToJSON(CacheInfoObject info) {
	JSONObject obj = new JSONObject();
	obj.put("nocache", (!info.isCacheable()));
	obj.put("private", info.isPrivate());
	obj.put("public", info.isPublic());
	obj.put("nomodify", info.isNoModify());
	obj.put("maxage", info.getMaxAge());
	obj.put("date", info.getDate().toString());
	obj.put("data", "test");
	obj.put("data", new String(info.getData()));
	obj.put("url", info.getKey());
	obj.put("method", info.getMethod());
	return obj;
    }

    public static CacheInfoObject convertToCacheInfoObject(JSONObject obj) {
	CacheInfoObject info = new CacheInfoObject();
	@SuppressWarnings("unchecked")
	Set<String> k = obj.keySet();
	String[] keys = new String[k.size()];
	k.toArray(keys);
	boolean v;
	int n;
	Date d;
	String s;
	for (int i = 0; i < keys.length; i++) {
	    switch (keys[i]) {
	    case "nocache":
		v = Boolean.parseBoolean((String) obj.get(keys[i]));
		info.setNoCache(v);
		break;
	    case "private":
		v = Boolean.parseBoolean((String) obj.get(keys[i]));
		info.setPrivate(v);
		break;
	    case "public":
		v = Boolean.parseBoolean((String) obj.get(keys[i]));
		info.setPublic(v);
		break;
	    case "nomodify":
		v = Boolean.parseBoolean((String) obj.get(keys[i]));
		info.setNoModify(v);
		break;
	    case "maxage":
		n = Integer.parseInt((String) obj.get(keys[i]));
		info.setMaxAge(n);
		break;
	    case "date":
		try {
		    d = DateFormat.getInstance().parse((String) obj.get(keys[i]));
		    info.setDate(d);
		} catch (ParseException e) {
		    ProxyLogger.getInstance().log(ProxyLogLevel.EXCEPTION,
			    "Unable to parse Date " + obj.get("url"));
		}
		break;
	    case "data":
		s = (String) obj.get(keys[i]);
		info.setData(s.getBytes());
		break;
	    case "url":
		info.setKey((String) obj.get(keys[i]));
		break;
	    case "method":
		info.setMethod((String) obj.get(keys[i]));
		break;
	    default:
		System.out.println("CAN NOT PARSE: " + keys[i]);
		break;
	    }
	}
	return info;
    }

    public static BufferedReader openFile(String path) {

	File f;
	BufferedReader bufReader = null;
	FileReader reader;

	try {

	    f = getFile(path);
	    reader = new FileReader(f);
	    bufReader = new BufferedReader(reader);
	} catch (IOException e) {

	    ProxyLogger.getInstance().log(Level.WARNING, "Could not open file: " + path);
	}
	return bufReader;
    }

    public static BufferedReader openOrCreateFile(String path) {

	BufferedReader br = openFile(path);
	File f = getFile(path);

	if (br == null) {
	    try {

		f.createNewFile();

		br = new BufferedReader(new FileReader(f));
	    } catch (IOException e) {

		ProxyLogger.getInstance()
			.log(Level.SEVERE, "Couldn't open or create file: " + path);
	    }
	}
	return br;
    }

    public static File openOrCreateFolder(String path) {
	File f = new File(path);
	if (!f.exists()) {
	    if (!f.isDirectory()) {
		f.mkdir();
	    }
	}
	return f;
    }

    public static File[] getAllFilesInFolder(String path) {
	File f = new File(path);
	return f.listFiles();
    }

    public static File getFile(String path) {
	return new File(path);
    }

    public static String getCurrentDate() {
	DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
	Calendar cal = Calendar.getInstance();
	return dateFormat.format(cal.getTime());
    }

    public static String getUrl(String s) {
	int i = s.indexOf(' ');
	int n = s.indexOf('\n');
	return s.substring(i, n).trim();
    }
}
