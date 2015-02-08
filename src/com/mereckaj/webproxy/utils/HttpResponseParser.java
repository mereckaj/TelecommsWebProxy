package com.mereckaj.webproxy.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class HttpResponseParser {

	private String protocol;
	private String method;
	private Map<String, String> headerFields;

	public HttpResponseParser(byte[] data) {
		headerFields = new HashMap<String, String>();
		String temp = new String(data);
		String[] lines = temp.split("\r\n");
		protocol = lines[0].split(" ")[0];
		method = lines[0].split(" ")[1];
		parseMethod(lines[0].split(" ")[1]);
		for (int i = 1; i < lines.length; i++) {
			if (!lines[i].isEmpty()) {
				String[] keyValue = lines[i].split(":");
				if (keyValue.length == 2) {
					headerFields.put(keyValue[0], keyValue[1]);
				} else {
					try{
						int n = lines[i].indexOf(':');
						String key = lines[i].substring(0, n);
						String value = lines[i].substring(n + 1);
						headerFields.put(key, value);
					} catch(StringIndexOutOfBoundsException e){
						e.printStackTrace();
					}
				}
			} else {
				break;
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
	private void parseMethod(String method){
		
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
}
