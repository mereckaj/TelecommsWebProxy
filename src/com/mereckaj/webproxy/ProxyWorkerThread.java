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

/**
 * This is the main class that the proxy uses. This class extends thread.
 * 
 * @author julius
 * 
 * In this class:<br>
 * User is the person requesting the proxy to connect it to someone
 * Host is the server the proxy will connect to for the user
 * 
 */
public class ProxyWorkerThread extends Thread {

    // Defauly HTTP port
    private static final int HTTP_PORT = 80;

    /*
     * Some stuff that this class will need
     */
    private Socket userToProxySocket;
    private Socket proxyToServerSocket;
    private OutputStream incomingOutputStream;
    private InputStream incomingInputStream;
    private OutputStream outgoingOutputStream;
    private InputStream outgoingInputStream;
    private HttpRequestParser httpRequestHeader;
    private HttpResponseParser httpResponseHeader;
    private CacheInfoObject cacheInfoObject;

    /*
     * Instances of all of the singleton objects used in this class
     */
    private ProxyDataLogger dataLogger = ProxyDataLogger.getInstance();
    private ProxyLogger logger = ProxyLogger.getInstance();
    private ProxyCacheManager proxyCacheManager = ProxyCacheManager.getInstance();
    private ProxyTrafficFilter trafficFilter = ProxyTrafficFilter.getInstance();
    private ProxySettings settings = ProxySettings.getInstance();

    private static boolean filteringEnabled = ProxySettings.getInstance().isFilteringEnabled();

    // Hack that is used to determine if this data needs to be stored
    private boolean cacheThisData;

    /*
     * Ints below are used for logging of usage statistics
     */
    private int dataReceived;
    private int dataSent;

    /*
     * Used to pass around data between the socket
     */
    byte[] userToHostData;
    byte[] hostToUserData;

    /**
     * Constructor for this class. Expects a socket that will feed it some data.
     * 
     * @param s
     */
    public ProxyWorkerThread(Socket s) {
	this.userToProxySocket = s;
    }

    /**
     * The work horse of this class. Sorry about the size
     */
    public void run() {
	try {

	    incomingInputStream = userToProxySocket.getInputStream();
	    incomingOutputStream = userToProxySocket.getOutputStream();
	    
	    /*
	     * Get data from the user
	     */
	    userToHostData = getDataFromUser();
	    if (userToHostData == null) {
		return;
	    }
	    dataSent += userToHostData.length;
	    
	    /*
	     * Parse user request for host and check if it is blocked or request contains blocked content
	     */
	    httpRequestHeader = new HttpRequestParser(userToHostData);
	    if (filterHost(httpRequestHeader.getHost()) || filterContent(userToHostData)) {
		return;
	    }
	    
	    /*
	     * Check if the website the user request is cached
	     */
	    if (proxyCacheManager.isCached(httpRequestHeader.getUrl())) {
		/*
		 * Website is cahced, get the data from the cache
		 */
		byte[] data = proxyCacheManager.getData(httpRequestHeader.getUrl()).getData();
		returnResponse(data);
		//Add info to the UI
		UICacheHit(httpRequestHeader.getUrl(), data.length);
	    } else {
		/*
		 * Page is not cached, create a socket to the host at port 80
		 */
		proxyToServerSocket = new Socket(httpRequestHeader.getHost(), HTTP_PORT);
		dataLogger.log(ProxyLogLevel.CONNECT, "Connected: " + httpRequestHeader.getHost()
			+ " For: " + httpRequestHeader.getUrl() + " Port: " + HTTP_PORT);
		//Add info the the UI
		ProxyGUI.addToInfoAread("CONNECT\t" + httpRequestHeader.getHost(),true);

		outgoingInputStream = proxyToServerSocket.getInputStream();
		outgoingOutputStream = proxyToServerSocket.getOutputStream();

		/*
		 * Send users request to the server
		 */
		sendUserRequest(userToHostData);
		
		/*
		 * Receive the response from the server
		 */
		hostToUserData = getResponse();
		dataReceived += hostToUserData.length;
		
		/*
		 * Parse the response from the server and cache it if needed
		 */
		httpResponseHeader = new HttpResponseParser(hostToUserData);
		cacheInfoObject = httpResponseHeader.getCacheInfo();
		if (cacheInfoObject!=null && cacheInfoObject.isCacheable()) {
		    cacheInfoObject.put(hostToUserData);
		    cacheThisData = true;
		} else {
		    cacheThisData = false;
		}
		
		/*
		 * return the response from the host to the user
		 */
		returnResponse(hostToUserData);
		
		
		/*
		 * This loop deals with any subsequent request/responses
		 * in other words if all of the data wasn't transfered in 1 request or 
		 * response the rest of the transfering is done here.
		 * 
		 * The reason for the previous request/response being outside is because they contain 
		 * request and response headers which are needed for socket creation and caching
		 */
		while (userToProxySocket.isConnected() && proxyToServerSocket.isConnected()) {
		    
		    /*
		     * If the host has data to return to user
		     */
		    if (outgoingInputStream.available() != 0) {
			hostToUserData = getResponse();
			dataReceived += hostToUserData.length;
			returnResponse(hostToUserData);
			if (cacheThisData) {
			    cacheInfoObject.put(hostToUserData);
			}
		    }
		    
		    /*
		     * If the user has data to send to host
		     */
		    if (incomingInputStream.available() != 0) {

			userToHostData = getDataFromUser();
			dataSent += userToHostData.length;

			if (filterContent(userToHostData)) {
			    break;
			}
			sendUserRequest(userToHostData);
		    }
		    
		    /*
		     * If the user and host have no more data to send to each other
		     * go to sleep for a 100ms. This is just in case the user/host are
		     * under a heavy load and weren't able to perform the request/response
		     */
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
		/*
		 * Cache this object if needed
		 */
		writeToCacheIfNeeded(cacheInfoObject, httpRequestHeader.getUrl());
	    }
	    dataLogger.log(ProxyLogLevel.DISCONNECT, "Disconnected:" + httpRequestHeader.getHost());
	    
	    //Print information to UI
	    ProxyGUI.addToInfoAread("DISCONNECT\t" + httpRequestHeader.getHost(),true);
	    ProxyGUI.addToInfoAread("USAGE\tSent:" + dataSent + " Received:" + dataReceived,true);
	    
	    /*
	     * Close all of the streams and connection
	     * Make java's trash collectors job easier
	     */
	    closeConnection();
	    closeDataStreams();

	} catch (IOException e) {
	    logger.log(ProxyLogLevel.EXCEPTION, httpRequestHeader.getHost() + ": " + e.getMessage());
	    e.printStackTrace();
	}
	try {
	    join();
	} catch (InterruptedException e) {
	    // Give up
	}
    }
    
    /*
     * Print the info of a cache hit to the UI
     */
    private void UICacheHit(String url, int length) {
	if (url.length() > 40) {
	    url = url.substring(0, 40) + "...";
	}
	ProxyGUI.addToInfoAread("CACHE HIT\t" + url + " " + length + " bytes",true);
    }
    
    /*
     * Checks if data needs to be cached (if the HTTP Response header contained no-cache or related info)
     * If needed this item will be cached.
     */
    private void writeToCacheIfNeeded(CacheInfoObject cacheInfoObject, String url) {
	boolean success = false;
	if (cacheInfoObject != null && cacheInfoObject.isCacheable()) {
	    cacheInfoObject.setKey(url);
	    if (!cacheInfoObject.isPrivate()) {
		success = ProxyCacheManager.getInstance().cacheIn(url, cacheInfoObject);
		if (!success) {
		    logger.log(ProxyLogLevel.EXCEPTION, "UNABLE TO CACHE ");
		}
	    }
	}
    }
    
    /*
     * Close the data streams. The only time this causes a null
     * pointer exception is if the stream is already closed or was not
     * Instantiated. Any other exception is thrown
     */
    private void closeDataStreams() throws IOException {
	try {
	    incomingInputStream.close();
	    incomingOutputStream.close();
	    outgoingInputStream.close();
	    outgoingOutputStream.close();
	} catch (NullPointerException e) {
	}
    }
    
    /*
     * Given the data from the host this method returns it to the user
     */
    private void returnResponse(byte[] byteBuffer) throws IOException {
	incomingOutputStream.write(byteBuffer, 0, byteBuffer.length);
	incomingOutputStream.flush();
    }
    
    /*
     * Given the user request this method sends it to the host
     */
    private void sendUserRequest(byte[] byteBuffer) throws IOException {
	outgoingOutputStream.write(byteBuffer, 0, byteBuffer.length);
    }
    
    /*
     * this method gets the response from the host
     */
    private byte[] getResponse() throws IOException {
	byte[] byteBuffer = null;
	byte[] byteTmp = new byte[settings.getMaxBuffer()];
	int size = outgoingInputStream.read(byteTmp, 0, byteTmp.length);

	if (size <= 0) {
	    return null;
	}
	byteBuffer = new byte[size];
	System.arraycopy(byteTmp, 0, byteBuffer, 0, size);
	return byteBuffer;
    }
    
    /*
     * this method gets the data from the user
     */
    private byte[] getDataFromUser() throws IOException {
	byte[] byteBuffer = null;
	byte[] byteTmp = new byte[settings.getMaxBuffer()];
	int size = incomingInputStream.read(byteTmp, 0, byteTmp.length);
	if (size <= 0) {
	    return null;
	}
	byteBuffer = new byte[size];
	System.arraycopy(byteTmp, 0, byteBuffer, 0, size);
	return byteBuffer;
    }
    
    /*
     * Given some data, returns true if the data contained filtered content
     */
    private boolean filterContent(byte[] data) {
	if (filteringEnabled && containsBlockedContent(data)) {
	    dataLogger.log(ProxyLogLevel.INFO,
		    "Blocked: from: " + userToProxySocket.getInetAddress() + " contenct violation");
	    ProxyGUI.addToInfoAread("BLOCKED\t" + userToProxySocket.getInetAddress(),true);
	    doActionIfBlocked();
	    return true;
	}
	return false;
    }
    
    /*
     * Given a hostname, this method checks if it is in the blocked list
     */
    private boolean filterHost(String host) {
	
	if (filteringEnabled && isBlockedHostOrIP(host)) {
	    dataLogger.log(ProxyLogLevel.INFO,
		    "Blocked :" + host + " from: " + userToProxySocket.getInetAddress()
			    + " blocked host");
	    ProxyGUI.addToInfoAread("BLOCKED\t" + host,true);
	    doActionIfBlocked();
	    return true;
	}
	return false;
    }
    
    /*
     * Closes socket connections.
     */
    private void closeConnection() {
	try {
	    userToProxySocket.close();
	    proxyToServerSocket.close();
	} catch (NullPointerException e) {
	} catch (IOException e) {
	}
    }
    
    /*
     * return true if data contains blocked phrases
     */
    private boolean containsBlockedContent(byte[] data) {
	if (trafficFilter.containsBlockedKeyword(data)) {
	    return true;
	} else {
	    return false;
	}
    }
    
    /*
     * Checks if the host is blocked
     */
    private boolean isBlockedHostOrIP(String host) {
	try {
	    String ip = InetAddress.getByName(host).getHostAddress();
	    boolean hostIsBlocked = trafficFilter.isBlockedHost(host);
	    boolean ipIsBlocked = trafficFilter.isBlockedIP(ip);
	    if (hostIsBlocked || ipIsBlocked) {
		return true;
	    } else {
		return false;
	    }
	} catch (UnknownHostException e) {
	    return trafficFilter.isBlockedHost(host);
	}
    }
    
    /*
     * If the host/ip/data is in some way refused a connection
     * This method will deal with it.
     * 
     * What it actually does is: get the content of the 
     * <b>proxy_refused_connection.html</b> file and return it to the user
     * afterwards close the connection and shut down this thread
     */
    private void doActionIfBlocked() {
	byte[] data = settings.getRefused();
	try {
	    returnResponse(data);
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