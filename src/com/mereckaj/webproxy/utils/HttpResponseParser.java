package com.mereckaj.webproxy.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.mereckaj.webproxy.CacheInfoObject;

public class HttpResponseParser {

	private String protocol;
	private String method;
	private Map<String, String> headerFields;
	private byte[] headerData;

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
				if (data[i] == '\n' && data[i + 1] == '\r'
						&& data[i + 2] == '\n') {
					headerData = new byte[data.length - (i + 3)];
					System.arraycopy(data, (i + 3), headerData, 0, data.length
							- (i + 3));
				}
			} catch (ArrayIndexOutOfBoundsException e) {
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
		boolean cacheControl = headerFields.containsKey("Cache-Control");
		String cc = headerFields.get("Cache-Control");
		int maxAge = -1;
		boolean isPrivate = false;
		boolean isPublic = false;
		boolean noTransform = false;
		if (cacheControl) {
			if (!cc.contains("no-cache")) {
				String[] settings = cc.split(",");
				for (int i = 0; i < settings.length; i++) {
					if (settings[i].contains("max-age")) {
						maxAge = Integer.parseInt(settings[i]
								.substring(settings[i].indexOf("=")+1));
					} else if (settings[i].contains("private")) {
						isPrivate = true;
					} else if (settings[i].contains("public")) {
						isPublic = true;
					} else if (settings[i].contains("no-transform")) {
						noTransform = true;
					} else {
						System.out.println("\t\t" + settings[i]);
					}
				}
			}
		}
		return null;
	}
}
