package com.mereckaj.webproxy.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.mereckaj.webproxy.CacheInfoObject;
/**
 * This class parses a HTTP Response header.
 * 
 * It gets data like: PROTOCOL,METHOD and mainly: <b> Cache-Content </b> settings
 */
public class HttpResponseParser {

    private String protocol;
    private String method;
    private Map<String, String> headerFields;
    private byte[] headerData;
    String temp;

    public HttpResponseParser(byte[] data) {
	if (data != null) {
	    headerFields = new HashMap<String, String>();
	    temp = new String(data);
	    String[] lines = temp.split("\r\n");
	    protocol = lines[0].split(" ")[0];
	    method = lines[0].split(" ")[1];
	    parseMethod(lines[0].split(" ")[1]);
	    for (int i = 1; i < lines.length; i++) {
		if (!lines[i].isEmpty()) {
		    String[] keyValue = lines[i].split(":");
		    if (keyValue.length == 2) {
			headerFields.put(keyValue[0].trim(), keyValue[1]);
		    } else {
			try {
			    int n = lines[i].indexOf(':');
			    String key = lines[i].substring(0, n);
			    String value = lines[i].substring(n + 1);
			    headerFields.put(key, value);
			} catch (StringIndexOutOfBoundsException e) {
			    e.printStackTrace();
			}
		    }
		} else {
		    break;
		}
	    }
	    for (int i = 0; i < data.length; i++) {
		try {
		    if (data[i] == '\n' && data[i + 1] == '\r' && data[i + 2] == '\n') {
			headerData = new byte[data.length - (i + 3)];
			System.arraycopy(data, (i + 3), headerData, 0, data.length - (i + 3));
		    }
		} catch (ArrayIndexOutOfBoundsException e) {
		}
	    }
	}
    }

    public void printheaderFields() {
	Collection<String> col = headerFields.keySet();
	String[] keys = new String[col.size()];
	col.toArray(keys);
	for (int i = 0; i < keys.length; i++) {
	    System.out.println(keys[i] + "\n\t" + headerFields.get(keys[i]));
	}
    }

    private void parseMethod(String method) {

    }

    public String getContentLength() {
	if (headerFields.containsKey("Content-Length")) {
	    return headerFields.get("Content-Length");
	} else {
	    return null;
	}
    }

    /*
     * Returns null if key does not exist in the map.
     */
    public String getValue(String key) {
	if (headerFields.containsKey(key)) {
	    return headerFields.get(key);
	} else {
	    return null;
	}
    }

    public String getProtocol() {
	return protocol;
    }

    public void setProtocol(String protocol) {
	this.protocol = protocol;
    }

    public String getStatus() {
	return method;
    }

    public void setStatus(String status) {
	this.method = status;
    }

    public CacheInfoObject getCacheInfo() {
	if (!method.contains("200")) {
	    return null;
	}
	CacheInfoObject infoObject = new CacheInfoObject();
	infoObject.setMethod(method);
	String[] settings;
	String tmp = "";
	boolean cacheControl = (headerFields.containsKey("Cache-Control") || headerFields
		.containsKey("cache-control"));
	String cc = headerFields.get("Cache-Control");
	if(cc == null){
	    cc = headerFields.get("cache-control");
	}
	if (cacheControl) {
	    if (!cc.contains("no-cache")) {
		infoObject.setNoCache(false);
		settings = cc.split(",");
		for (int i = 0; i < settings.length; i++) {
		    tmp = settings[i];
		    if (tmp.contains("max-age")) {
			String s = tmp.substring(tmp.indexOf("=") + 1);
			infoObject.setMaxAge(Integer.parseInt(s));
		    } else if (tmp.contains("private")) {
			infoObject.setPrivate(true);
		    } else if (tmp.contains("public")) {
			infoObject.setPublic(true);
		    } else if (tmp.contains("no-transform")) {
			infoObject.setNoModify(true);
		    } else if (tmp.contains("must-revalidate")) {
			infoObject.setMustRevalidate(true);
		    } 
		}
	    } else {
		infoObject.setNoCache(true);
	    }
	}
	if (headerFields.containsKey("Date")) {
	    infoObject.setDate(parseDate(headerFields.get("Date").trim()));
	}
	infoObject.wholeheader = temp;
	return infoObject;
    }
    /**
     * This method parses the HTTP format date into a {@link Date} object.
     * This is used to enable comparison of dates by the cahce.
     * @param date the string format date to be parsed
     * @return <b>null</b> if a {@link ParseException} is caught<br>
     * {@link Date} if the parse is successful.
     */
    public Date parseDate(String date) {
	Date d = null;
	SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
	try {
	    d = format.parse(date);
	} catch (ParseException e) {
	}
	return d;
    }
}
