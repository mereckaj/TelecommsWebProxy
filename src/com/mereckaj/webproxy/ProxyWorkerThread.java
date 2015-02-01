package com.mereckaj.webproxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import com.mereckaj.webproxy.utils.HttpHeaderParser;

/**
 * This {@link Thread} object gets created each time {@link Main} gets a request
 * from a user to connect to some host.
 * 
 * Some info about the naming in the comments:
 * 
 * User is the program that request the proxy for a connection to some server on
 * the Internet. (Sometimes may be referred to as the client)
 * 
 * Host is the server on the Internet to which the proxy will create a
 * connection on the behalf of the user.
 */
public class ProxyWorkerThread extends Thread {

	/*
	 * HTTP port for the host server.
	 */
	private static final int HTTP_PORT = 80;

	/*
	 * Variables used to count the total number of transfered bytes by this
	 * worker thread.
	 */
	private int bytesReceivedFromHost;
	private int bytesReceivedFromUser;

	/*
	 * User socket, passed in through the constructor.
	 */
	private Socket userToProxySocket;

	/*
	 * Host socket, created by the proxy and used to connect to some host on the
	 * behalf of the user.
	 */
	private Socket proxyToServerSocket;

	/*
	 * Output stream from the proxy to the user.
	 */
	private OutputStream incomingOutputStream;

	/*
	 * Input stream from the user to the proxy.
	 */
	private InputStream incomingInputStream;

	/*
	 * OutputStream from the proxy to the host.
	 */
	private OutputStream outgoingOutputStream;

	/*
	 * InputStream from the host to proxy.
	 */
	private InputStream outgoingInputStream;

	/*
	 * Boolean value used to indicated if filtering of content is enabled.
	 * 
	 * Will be set in the constructor according to the value sepcified by the
	 * proxy config.
	 */
	private static boolean filteringEnabled;

	/**
	 * Constructor for this class.
	 * 
	 * @param s
	 *            = {@link Socket} socket which is received from {@link Main}
	 */
	public ProxyWorkerThread(Socket s) {
		this.userToProxySocket = s;
		filteringEnabled = ProxySettings.getInstance().isFilteringEnabled();
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 * 
	 *      "God" method.
	 * 
	 */
	public void run() {

		/*
		 * byte arrays used to store the data while is being passed between the
		 * user and the host.
		 */
		byte[] userToHostData;
		byte[] hostToUserData;

		/*
		 * header will contain the info about what type of connection and where
		 * to the user wants to connect to.
		 */
		HttpHeaderParser header = null;

		try {

			/*
			 * Set up incoming streams.
			 */
			incomingInputStream = userToProxySocket.getInputStream();
			incomingOutputStream = userToProxySocket.getOutputStream();

			/*
			 * Read data passed to this proxy from a user.
			 */
			userToHostData = getDataFromUserToRemoteHot();
			if (userToHostData == null) {
				return;
			}

			/*
			 * Parse data give by the user into a header.
			 */
			header = new HttpHeaderParser(userToHostData);
			bytesReceivedFromUser += userToHostData.length;

			/*
			 * Filter hosts.
			 */
			if(filterHost(header.getHost())){
				return;
			}

			/*
			 * Filter content.
			 */
			if(filterContent(userToHostData)){
				return;
			}

			/*
			 * Create a new socket from proxy to host.
			 * 
			 * If the header method is CONNECT, program will not work.
			 */
			proxyToServerSocket = new Socket(header.getHost(), HTTP_PORT);
			ProxyDataLogger.getInstance().log(
					ProxyLogLevel.CONNECT,
					"Connected: " + header.getHost() + " For: "
							+ header.getUrl() + " Port: " + HTTP_PORT);

			/*
			 * Set up outgoing streams.
			 */
			outgoingInputStream = proxyToServerSocket.getInputStream();
			outgoingOutputStream = proxyToServerSocket.getOutputStream();

			/*
			 * Send initial request, from the user, to the host.
			 */
			sendUserRequestToRemoteHost(userToHostData);

			/*
			 * Get initial reply from the host.
			 */
			hostToUserData = getResponseFromRemoteHost();
			bytesReceivedFromHost += hostToUserData.length;

			/*
			 * Pass initial return, from the host, to the user.
			 */
			returnResponseFromHostToUser(hostToUserData);

			while (userToProxySocket.isConnected()
					&& proxyToServerSocket.isConnected()) {

				if (outgoingInputStream.available() != 0) {

					/*
					 * If a reply is available, read it.
					 */
					hostToUserData = getResponseFromRemoteHost();
					bytesReceivedFromHost += hostToUserData.length;

					/*
					 * Pass the reply to the user.
					 */
					returnResponseFromHostToUser(hostToUserData);
				}
				if (incomingInputStream.available() != 0) {

					/*
					 * If there is data passed from the client, read it in.
					 */
					userToHostData = getDataFromUserToRemoteHot();
					bytesReceivedFromUser += userToHostData.length;
					
					if(filterContent(userToHostData)){
						return;
					}

					/*
					 * Send the data to the host.
					 */
					sendUserRequestToRemoteHost(userToHostData);
				}

				/*
				 * If there is no more data to send from the user to host, or
				 * from host to user, put this thread to sleep for 100ms.
				 */
				if (incomingInputStream.available() == 0
						&& outgoingInputStream.available() == 0) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					/*
					 * Check again for traffic. If there is nothing then Break
					 * out of the while loop and close connections and streams.
					 */
					if (incomingInputStream.available() == 0
							&& outgoingInputStream.available() == 0) {
						break;
					}
				}
			}

			/*
			 * Log the disconnect from the host
			 */
			ProxyDataLogger.getInstance().log(
					ProxyLogLevel.DISCONNECT,
					"Disconnected:" + header.getHost() + " For: "
							+ header.getUrl());

			/*
			 * Log the usage statistics
			 */
			ProxyDataLogger.getInstance().log(
					ProxyLogLevel.USAGE,
					"U2P:" + bytesReceivedFromUser + " " + "H2P:"
							+ bytesReceivedFromHost);

			/*
			 * Close both of the sockets.
			 */
			closeConnection();

			/*
			 * Close all of the data streams
			 */
			closeDataStreams();

		} catch (IOException e) {
			ProxyLogger.getInstance().log(ProxyLogLevel.EXCEPTION,
					header.getHost() + ": " + e.getMessage());
			e.printStackTrace();
		}
	}

	/*
	 * This method closes all of the output streams that were created for
	 * transferring data between the two sockets
	 */
	private void closeDataStreams() throws IOException {
		try{
			incomingInputStream.close();
			incomingOutputStream.close();
			outgoingInputStream.close();
			outgoingOutputStream.close();	
		} catch (NullPointerException e ){
		}
	}

	/*
	 * This methods returns the reply from the host to the user.
	 */
	private void returnResponseFromHostToUser(byte[] byteBuffer)
			throws IOException {
		incomingOutputStream.write(byteBuffer, 0, byteBuffer.length);
		incomingOutputStream.flush();
	}

	/*
	 * This method sends the request from the user to the remote host
	 */
	private void sendUserRequestToRemoteHost(byte[] byteBuffer)
			throws IOException {
		outgoingOutputStream.write(byteBuffer, 0, byteBuffer.length);
	}

	/*
	 * This method retrieves the response from the remote host.
	 */
	private byte[] getResponseFromRemoteHost() throws IOException {
		byte[] byteBuffer = null;
		byte[] byteTmp = new byte[ProxySettings.getInstance().getMaxBuffer()];
		int size = outgoingInputStream.read(byteTmp, 0, byteTmp.length);

		/*
		 * If size is <= 0 then there is no more data to pass back. so return to
		 * normal flow.
		 */
		if (size <= 0) {
			return null;
		}
		byteBuffer = new byte[size];
		System.arraycopy(byteTmp, 0, byteBuffer, 0, size);
		return byteBuffer;
	}

	/*
	 * This method takes the data from the user
	 */
	private byte[] getDataFromUserToRemoteHot() throws IOException {
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

	/*
	 * Method called to filter for blocked content;
	 */
	private boolean filterContent(byte[] data) {
		if (filteringEnabled && containsBlockedContent(data)) {
			ProxyDataLogger.getInstance().log(
					ProxyLogLevel.INFO,
					"Blocked: from: "
							+ userToProxySocket.getInetAddress()
							+ " contenct violation");
			doActionIfBlocked();
			return true;
		}
		return false;
	}

	/*
	 * Method called to filter hosts
	 */
	private boolean filterHost(String host) {
		if (filteringEnabled && isBlockedHostOrIP(host)) {
			ProxyDataLogger.getInstance().log(
					ProxyLogLevel.INFO,
					"Blocked :" + host + " from: "
							+ userToProxySocket.getInetAddress()
							+ " blocked host");
			doActionIfBlocked();
			return true;
		}
		return false;
	}

	/*
	 * Method used to close the connection. Should be called if an error
	 * occurred or content/host contain disallowed items.
	 */
	private void closeConnection() {
		try {
			userToProxySocket.close();
			proxyToServerSocket.close();
		} catch (NullPointerException e) {
		} catch (IOException e) {
		}
	}

	private boolean containsBlockedContent(byte[] data) {
		if(ProxyTrafficFilter.getInstance().containsBlockedKeyword(data)){
			return true;
		}else{
			return false;
		}
	}

	/*
	 * This method checks is the host is in the blocked list.
	 * 
	 * File containing blocked hosts is set in the .proxy_config file
	 */
	private boolean isBlockedHostOrIP(String host) {
		try {
			String ip = InetAddress.getByName(host).getHostAddress();
			boolean hostIsBlocked = ProxyTrafficFilter.getInstance().isBlockedHost(host);
			boolean ipIsBlocked = ProxyTrafficFilter.getInstance().isBlockedIP(ip);
			if(hostIsBlocked || ipIsBlocked){
				return true;
			}else{
				return false;
			}
		} catch (UnknownHostException e) {
			return ProxyTrafficFilter.getInstance().isBlockedHost(host);
		}
	}

	/*
	 * This method will only be called if the host is blocked,
	 * 
	 * This will return some data to the user to indicate that the host it tried
	 * to reach is not allowed
	 */
	private void doActionIfBlocked() {
		closeConnection();
		try {
			closeDataStreams();
		} catch (IOException e) {
		}
	}

}