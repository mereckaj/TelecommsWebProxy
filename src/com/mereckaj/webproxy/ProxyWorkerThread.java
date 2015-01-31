package com.mereckaj.webproxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import com.mereckaj.webproxy.utils.HttpHeaderParser;

/**
 * This {@link Thread} object gets created each time {@link Main} gets a request
 * from a user to connect to some host.
 */
public class ProxyWorkerThread extends Thread {

	/*
	 * HTTP port for the host server
	 */
	private static final int HTTP_PORT = 80;
	private static final int HTTPS_PORT = 443;

	/*
	 * Variables used to count the total number of transfered bytes by this
	 * current worker thread
	 */
	private int bytesReceivedFromHost;
	private int bytesReceivedFromUser;
	/*
	 * User socket, passed in through the constructor
	 */
	Socket clientToProxySocket;

	/*
	 * Output stream that will be used to pass user the response from the host
	 * it tried to connect with
	 */
	OutputStream incomingOutputStream;

	/*
	 * Input stream that will be parsed and used to create new connection
	 * details by the proxy
	 */
	InputStream incomingInputStream;

	/*
	 * Boolean value used to indicated if filtering of content is enabled
	 */
	private static boolean filteringEnabled;
	/*
	 * 
	 */
	OutputStream outgoingOutputStream;

	/*
	 * 
	 */
	InputStream outgoingInputStream;

	/*
	 * 
	 */
	Socket proxyToServerSocket;

	/*
	 * Temporary byte buffer, used for passing data between sockets.
	 */
	byte[] byteTmp;

	/**
	 * Constructor for this class.
	 * 
	 * @param s
	 *            = {@link Socket} socket which is received from {@link Main}
	 */
	public ProxyWorkerThread(Socket s) {
		this.clientToProxySocket = s;
		filteringEnabled = ProxySettings.getInstance().isFilteringEnabled();
		bytesReceivedFromHost = 0;
	}

	/*
	 * (non-Javadoc)
	 * 				
	 * @see java.lang.Thread#run()
	 */		
	public void run() {
		byte[] userToHostData;
		byte[] hostToUserData;
		HttpHeaderParser header = null;
		try {
			/*
			 * Set up incoming streams
			 */
			incomingInputStream = clientToProxySocket.getInputStream();
			incomingOutputStream = clientToProxySocket.getOutputStream();

			/*
			 * Read data passed to this proxy from a client
			 */
			userToHostData = getDataFromUserToRemoteHot();
			if (userToHostData == null) {
				return;
			}

			/*
			 * Parse this data into a header
			 */
			header = new HttpHeaderParser(userToHostData);
			bytesReceivedFromUser += userToHostData.length;
			/*
			 * Filter hosts
			 */
			filterHost(header.getHost());

			/*
			 * Filter content
			 */
			filterContent(userToHostData);

			/*
			 * Create a new socket from proxy to host
			 * 
			 * If the header contains method CONNECT, create a socket to port
			 * 443 Which handles HTTPS traffic. Otherwise socket to port 80
			 * Which handles HTTP traffic
			 */
			if (header.getMethod() == HttpHeaderParser.METHOD.CONNECT) {
				System.out.println("HTTPS REQUEST");
				return;
				// proxyToServerSocket = new Socket(header.getHost(),
				// HTTPS_PORT);
				// ProxyDataLogger.getInstance().log(
				// ProxyLogLevel.CONNECT,
				// "Connected: " + header.getHost() + " For: "
				// + header.getUrl() + " Port: " + HTTPS_PORT);
				// while(proxyToServerSocket.isConnected()){
				// System.out.println("STILL CONNECTED");
				// try {
				// Thread.sleep(100);
				// } catch (InterruptedException e) {
				// e.printStackTrace();
				// }
				// }
				// System.out.println("NO MORE");

			} else {
				proxyToServerSocket = new Socket(header.getHost(), HTTP_PORT);
				ProxyDataLogger.getInstance().log(
						ProxyLogLevel.CONNECT,
						"Connected: " + header.getHost() + " For: "
								+ header.getUrl() + " Port: " + HTTP_PORT);
			}

			/*
			 * Set up outgoing streams
			 */
			outgoingInputStream = proxyToServerSocket.getInputStream();
			outgoingOutputStream = proxyToServerSocket.getOutputStream();

			/*
			 * Send initial request
			 */
			sendUserRequestToRemoteHost(userToHostData);

			/*
			 * Get initial reply
			 */
			hostToUserData = getResponseFromRemoteHost();
			bytesReceivedFromHost += hostToUserData.length;
			/*
			 * Pass initial return to user
			 */
			returnResponseFromHostToUser(hostToUserData);

			while (clientToProxySocket.isConnected()
					&& proxyToServerSocket.isConnected()) {

				if (outgoingInputStream.available() != 0) {
					/*
					 * Get reply
					 */
					hostToUserData = getResponseFromRemoteHost();
					bytesReceivedFromHost += hostToUserData.length;

					/*
					 * Pass return to user
					 */
					returnResponseFromHostToUser(hostToUserData);
				}
				if (incomingInputStream.available() != 0) {

					/*
					 * Read data passed from a client
					 */
					userToHostData = getDataFromUserToRemoteHot();
					bytesReceivedFromUser += userToHostData.length;

					/*
					 * Send initial request
					 */
					sendUserRequestToRemoteHost(userToHostData);
				}

				/*
				 * If there is no more data to send from the user to host or
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
					 * Check again for traffic, if there is nothing left Break
					 * out of the while loop.
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
	 * Method called after initial send of user request to host:443 Check if
	 * client has data to return Check if user has data to send
	 */
	private void debugCONNECT() throws IOException {
		System.out.println("IN_IN_STREAM_NULL: "
				+ (incomingInputStream == null));
		System.out.println("IN_OUT_STREAM_NULL: "
				+ (incomingOutputStream == null));
		System.out.println("OUT_IN_STREAM_NULL: "
				+ (outgoingInputStream == null));
		System.out.println("OUT_OUT_STREAM_NULL: "
				+ (outgoingOutputStream == null));
		System.out.println("U2P_SOCKET_CLOSED: "
				+ clientToProxySocket.isClosed());
		System.out.println("U2P_SOCKET_CONNECTED: "
				+ clientToProxySocket.isConnected());
		System.out.println("P2H_SOCKET_CLOSED: "
				+ proxyToServerSocket.isClosed());
		System.out.println("P2H_SOCKET_CONNECTED: "
				+ proxyToServerSocket.isConnected());
		System.out.println("CLIENT_DATA_READY: "
				+ incomingInputStream.available());
		System.out.println("HOST_DATA_READY: "
				+ incomingInputStream.available());
	}

	/*
	 * This method closes all of the output streams that were created for
	 * transferring data between the two sockets
	 */
	private void closeDataStreams() throws IOException {
		incomingInputStream.close();
		incomingOutputStream.close();
		outgoingInputStream.close();
		outgoingOutputStream.close();
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
	private void filterContent(byte[] data) {
		if (filteringEnabled && containsBlockedContent(data)) {
			ProxyDataLogger.getInstance().log(
					ProxyLogLevel.INFO,
					"Blocked :" + " from: "
							+ clientToProxySocket.getInetAddress()
							+ " contenct violation");
			doActionIfBlocked();
		}
	}

	/*
	 * Method called to filter hosts
	 */
	private void filterHost(String host) {
		if (filteringEnabled && isBlockedHost(host)) {
			ProxyDataLogger.getInstance().log(
					ProxyLogLevel.INFO,
					"Blocked :" + host + " from: "
							+ clientToProxySocket.getInetAddress()
							+ " blocked host");
			doActionIfBlocked();
		}
	}

	/*
	 * Method used to close the connection. Should be called if an error
	 * occurred or content/host contain disallowed items.
	 */
	private void closeConnection() {
		try {
			clientToProxySocket.close();
			proxyToServerSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean containsBlockedContent(byte[] data) {
		// TODO check content
		return false;
	}

	/*
	 * This method checks is the host is in the blocked list.
	 * 
	 * File containing blocked hosts is set in the .proxy_config file
	 */
	private boolean isBlockedHost(String host) {
		// TODO: scan hosts
		return false;
	}

	/*
	 * This method will only be called if the host is blocked,
	 * 
	 * This will return some data to the user to indicate that the host it tried
	 * to reach is not allowed
	 */
	private void doActionIfBlocked() {
		// TODO: Action
	}

	public static Map<String, String> parseHTTPHeaders(InputStream inputStream)
			throws IOException {
		int charRead;
		StringBuffer sb = new StringBuffer();
		int count = 0;
		while (true) {
			count++;
			if (count == 30) {
				break;
			}
			sb.append((char) (charRead = inputStream.read()));
			if ((char) charRead == '\r') { // if we've got a '\r'
				sb.append((char) inputStream.read()); // then write '\n'
				charRead = inputStream.read(); // read the next char;
				if (charRead == '\r') { // if it's another '\r'
					sb.append((char) inputStream.read());// write the '\n'
					break;
				} else {
					sb.append((char) charRead);
				}
			}
		}

		String[] headersArray = sb.toString().split("\r\n");
		Map<String, String> headers = new HashMap<>();
		for (int i = 1; i < headersArray.length - 1; i++) {
			headers.put(headersArray[i].split(": ")[0],
					headersArray[i].split(": ")[1]);
		}

		return headers;
	}
}