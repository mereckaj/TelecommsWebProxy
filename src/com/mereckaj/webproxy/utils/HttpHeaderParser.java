package com.mereckaj.webproxy.utils;

public class HttpHeaderParser {
	public String url;
	public String host;

	public HttpHeaderParser(byte[] data) {
		String s = new String(data);
		String[] lines = s.split("\n");
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].contains("http://")) {
				int n = lines[i].indexOf("http://");
				int m = lines[i].indexOf(' ', n);
				url = lines[i].substring(n, m);
				break;
			}
		}
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].contains("Host:")) {
				host = lines[i].substring(6).trim();
				break;
			}
		}
	}
}
