package com.mereckaj.webproxy.utils;

public class HttpHeaderParser {
	public static enum METHOD {
		GET, HEAD, POST, PUT, DELETE, TRACE, OPTIONS, CONNECT, PATCH, ERROR
	};

	public static enum STATE {
		OK, ERROR
	}

	private String url;
	private String host;
	private METHOD method;
	private String protocol;
	private STATE state;
	private int numberOfLinesInHeader;

	public HttpHeaderParser(byte[] data) {
		String temp = new String(data);
		try {
			String[] lines = temp.split("\n");
			numberOfLinesInHeader = lines.length;
			parseFirstLine(lines[0]);
			parseLines(lines);
			if(method==METHOD.CONNECT){
				reparseHost();
			}
			state = STATE.OK;
		} catch (Exception e) {
			state = STATE.ERROR;
			e.printStackTrace();
		}
	}
	public void reparseHost(){
		host = host.substring(0, host.indexOf(':'));
	}

	private void parseFirstLine(String string) {
		String[] s = string.split(" ");
		if (s.length > 0) {
			parserMethod(s[0]);
			if (method == METHOD.CONNECT) {
				url = s[1];
				url.substring(0, url.indexOf(':'));
			} else {
				url = s[1];
			}
			protocol = s[2];
		} else {
			System.out.println("UNABLE TO PARSE FIRST LINE");
		}

	}

	private void parserMethod(String string) {
		switch (string.trim()) {
		case "GET":
			method = METHOD.GET;
			break;
		case "HEAD":
			method = METHOD.HEAD;
			break;
		case "POST":
			method = METHOD.POST;
			break;
		case "PUT":
			method = METHOD.PUT;
			break;
		case "DELETE":
			method = METHOD.DELETE;
			break;
		case "TRACE":
			method = METHOD.TRACE;
			break;
		case "OPTIONS":
			method = METHOD.OPTIONS;
			break;
		case "CONNECT":
			method = METHOD.CONNECT;
			break;
		case "PATCH":
			method = METHOD.PATCH;
			break;
		default:
			method = METHOD.ERROR;
		}
	}

	private void parseLines(String[] lines) {
		for (int i = 1; i < numberOfLinesInHeader; i++) {
			if (lines[i].contains("Host:")) {
				host = lines[i].substring(6).trim();
				if(host.contains("\n")){
					host = host.substring(0,(host.indexOf('\n')-1));
				}
			}
		}
	}

	public String getUrl() {
		return url;
	}

	public String getHost() {
		return host;
	}

	public METHOD getMethod() {
		return method;
	}
	public String getMethodName(){
		return method.name();
	}

	public String getProtocol() {
		return protocol;
	}

	public String getState() {
		return state.name();
	}

}
