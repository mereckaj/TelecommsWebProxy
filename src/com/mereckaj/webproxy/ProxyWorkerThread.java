package com.mereckaj.webproxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import com.mereckaj.webproxy.gui.ProxyGUI;
import com.mereckaj.webproxy.utils.HttpRequestParser;
import com.mereckaj.webproxy.utils.HttpResponseParser;

public class ProxyWorkerThread extends Thread {

    private static final int HTTP_PORT = 80;

    private Socket userToProxySocket;

    private Socket proxyToServerSocket;

    private OutputStream incomingOutputStream;

    private InputStream incomingInputStream;

    private OutputStream outgoingOutputStream;

    private InputStream outgoingInputStream;

    private static boolean filteringEnabled;

    private boolean cacheThisData;

    private int dataReceived;

    private int dataSent;

    public ProxyWorkerThread(Socket s) {
	this.userToProxySocket = s;
	filteringEnabled = ProxySettings.getInstance().isFilteringEnabled();
    }

    public void run() {

	byte[] userToHostData;
	byte[] hostToUserData;

	HttpRequestParser httpRequestHeader = null;
	HttpResponseParser httpResponseHeader = null;
	ProxyDataLogger proxyDataLogger = ProxyDataLogger.getInstance();
	ProxyCacheManager proxyCacheManager = ProxyCacheManager.getInstance();
	CacheInfoObject cacheInfoObject = null;
	try {

	    incomingInputStream = userToProxySocket.getInputStream();
	    incomingOutputStream = userToProxySocket.getOutputStream();

	    userToHostData = getDataFromUserToRemoteHost();
	    if (userToHostData == null) {
		return;
	    }
	    dataSent += userToHostData.length;

	    httpRequestHeader = new HttpRequestParser(userToHostData);
	    if (filterHost(httpRequestHeader.getHost()) || filterContent(userToHostData)) {
		return;
	    }

	    if (proxyCacheManager.isCached(httpRequestHeader.getUrl())) {
		byte[] data = proxyCacheManager.getData(httpRequestHeader.getUrl()).getData();
		returnResponseFromHostToUser(data);
		UICacheHit(httpRequestHeader.getUrl(), data.length);
	    } else {
		proxyToServerSocket = new Socket(httpRequestHeader.getHost(), HTTP_PORT);
		proxyDataLogger.log(
			ProxyLogLevel.CONNECT,
			"Connected: " + httpRequestHeader.getHost() + " For: "
				+ httpRequestHeader.getUrl() + " Port: " + HTTP_PORT);
		ProxyGUI.addToInfoAread("CONNECT\t" + httpRequestHeader.getHost());

		outgoingInputStream = proxyToServerSocket.getInputStream();
		outgoingOutputStream = proxyToServerSocket.getOutputStream();

		sendUserRequestToRemoteHost(userToHostData);

		hostToUserData = getResponseFromRemoteHost();
		dataReceived += hostToUserData.length;

		httpResponseHeader = new HttpResponseParser(hostToUserData);
		cacheInfoObject = httpResponseHeader.getCacheInfo();
		if (cacheInfoObject.isCacheable()) {
		    cacheInfoObject.put(hostToUserData);
		    cacheThisData = true;
		} else {
		    cacheThisData = false;
		}

		returnResponseFromHostToUser(hostToUserData);

		while (userToProxySocket.isConnected() && proxyToServerSocket.isConnected()) {

		    if (outgoingInputStream.available() != 0) {
			hostToUserData = getResponseFromRemoteHost();
			dataReceived += hostToUserData.length;
			returnResponseFromHostToUser(hostToUserData);
			if (cacheThisData) {
			    cacheInfoObject.put(hostToUserData);
			}
		    }
		    if (incomingInputStream.available() != 0) {

			userToHostData = getDataFromUserToRemoteHost();
			dataSent += userToHostData.length;

			if (filterContent(userToHostData)) {
			    break;
			}

			sendUserRequestToRemoteHost(userToHostData);
		    }

		    if (incomingInputStream.available() == 0
			    && outgoingInputStream.available() == 0) {
			try {
			    Thread.sleep(100);
			} catch (InterruptedException e) {
			    e.printStackTrace();
			}

			if (incomingInputStream.available() == 0
				&& outgoingInputStream.available() == 0) {
			    break;
			}
		    }
		}
		writeToCacheIfNeeded(cacheInfoObject, httpRequestHeader.getUrl());
	    }
	    proxyDataLogger.log(ProxyLogLevel.DISCONNECT,
		    "Disconnected:" + httpRequestHeader.getHost());
	    ProxyGUI.addToInfoAread("DISCONNECT\t" + httpRequestHeader.getHost());
	    ProxyGUI.addToInfoAread("USAGE\tSent:" + dataSent + " Received:" + dataReceived);
	    closeConnection();
	    closeDataStreams();

	} catch (IOException e) {
	    ProxyLogger.getInstance().log(ProxyLogLevel.EXCEPTION,
		    httpRequestHeader.getHost() + ": " + e.getMessage());
	    e.printStackTrace();
	}
	try {
	    join();
	} catch (InterruptedException e) {
	}
    }

    private void UICacheHit(String url, int length) {
	if (url.length() > 80) {
	    url = url.substring(0, 40) + "...";
	}
	ProxyGUI.addToInfoAread("CACHE HIT\t" + url + " " + length+" bytes");
    }

    private void writeToCacheIfNeeded(CacheInfoObject cacheInfoObject, String url) {
	boolean success = false;
	if (cacheInfoObject != null && cacheInfoObject.isCacheable()) {
	    if (!cacheInfoObject.isPrivate()) {
		success = ProxyCacheManager.getInstance().cacheIn(url, cacheInfoObject);
		if (!success) {
		    ProxyLogger.getInstance().log(ProxyLogLevel.EXCEPTION, "UNABLE TO CACHE ");
		}
	    }
	}
    }

    private void closeDataStreams() throws IOException {
	try {
	    incomingInputStream.close();
	    incomingOutputStream.close();
	    outgoingInputStream.close();
	    outgoingOutputStream.close();
	} catch (NullPointerException e) {
	}
    }

    private void returnResponseFromHostToUser(byte[] byteBuffer) throws IOException {
	incomingOutputStream.write(byteBuffer, 0, byteBuffer.length);
	incomingOutputStream.flush();
    }

    private void sendUserRequestToRemoteHost(byte[] byteBuffer) throws IOException {
	outgoingOutputStream.write(byteBuffer, 0, byteBuffer.length);
    }

    private byte[] getResponseFromRemoteHost() throws IOException {
	byte[] byteBuffer = null;
	byte[] byteTmp = new byte[ProxySettings.getInstance().getMaxBuffer()];
	int size = outgoingInputStream.read(byteTmp, 0, byteTmp.length);

	if (size <= 0) {
	    return null;
	}
	byteBuffer = new byte[size];
	System.arraycopy(byteTmp, 0, byteBuffer, 0, size);
	return byteBuffer;
    }

    private byte[] getDataFromUserToRemoteHost() throws IOException {
	byte[] byteBuffer = null;
	byte[] byteTmp = new byte[ProxySettings.getInstance().getMaxBuffer()];
	int size = incomingInputStream.read(byteTmp, 0, byteTmp.length);
	if (size <= 0) {
	    return null;
	}
	byteBuffer = new byte[size];
	System.arraycopy(byteTmp, 0, byteBuffer, 0, size);
	return byteBuffer;
    }

    private boolean filterContent(byte[] data) {
	if (filteringEnabled && containsBlockedContent(data)) {
	    ProxyDataLogger.getInstance().log(ProxyLogLevel.INFO,
		    "Blocked: from: " + userToProxySocket.getInetAddress() + " contenct violation");
	    ProxyGUI.addToInfoAread("BLOCKED\t" + userToProxySocket.getInetAddress());
	    doActionIfBlocked();
	    return true;
	}
	return false;
    }

    private boolean filterHost(String host) {
	if (filteringEnabled && isBlockedHostOrIP(host)) {
	    ProxyDataLogger.getInstance().log(
		    ProxyLogLevel.INFO,
		    "Blocked :" + host + " from: " + userToProxySocket.getInetAddress()
			    + " blocked host");
	    ProxyGUI.addToInfoAread("BLOCKED\t" + host);
	    doActionIfBlocked();
	    return true;
	}
	return false;
    }

    private void closeConnection() {
	try {
	    userToProxySocket.close();
	    proxyToServerSocket.close();
	} catch (NullPointerException e) {
	} catch (IOException e) {
	}
    }

    private boolean containsBlockedContent(byte[] data) {
	if (ProxyTrafficFilter.getInstance().containsBlockedKeyword(data)) {
	    return true;
	} else {
	    return false;
	}
    }

    private boolean isBlockedHostOrIP(String host) {
	try {
	    String ip = InetAddress.getByName(host).getHostAddress();
	    boolean hostIsBlocked = ProxyTrafficFilter.getInstance().isBlockedHost(host);
	    boolean ipIsBlocked = ProxyTrafficFilter.getInstance().isBlockedIP(ip);
	    if (hostIsBlocked || ipIsBlocked) {
		return true;
	    } else {
		return false;
	    }
	} catch (UnknownHostException e) {
	    return ProxyTrafficFilter.getInstance().isBlockedHost(host);
	}
    }

    private void doActionIfBlocked() {
	byte[] data = ProxySettings.getInstance().getRefused();
	try {
	    returnResponseFromHostToUser(data);
	} catch (IOException e1) {
	    e1.printStackTrace();
	}
	closeConnection();
	try {
	    closeDataStreams();
	} catch (IOException e) {
	}
    }

}