package com.mereckaj.webproxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;

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
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		byte[] userToHostData;
		byte[] hostToUserData;
		HttpHeaderParser header;
		try {
//			System.out.println("------------------------------------------");
//			System.out.println("DEBUG START:");
			// Set up incoming streams
			incomingInputStream = clientToProxySocket.getInputStream();
			incomingOutputStream = clientToProxySocket.getOutputStream();

			// Read data passed to this proxy from a client
			userToHostData = getDataFromUserToRemoteHot();
//			System.out.println("\tRead initial request");

			// Parse this data into a header
			header = new HttpHeaderParser(userToHostData);

			//Filter hosts
			filterHost(header.host);
			
			//Filter content
			filterContent(userToHostData);
			
			// Create a new socket from proxy to host
			proxyToServerSocket = new Socket(header.host, HTTP_PORT);
//			System.out.println("\tConnecting to: " + header.host + "\n\t\t"
//					+ header.url);
			
			// Set up outgoing streams
			outgoingInputStream = proxyToServerSocket.getInputStream();
			outgoingOutputStream = proxyToServerSocket.getOutputStream();

			// Send initial request
			sendUserRequestToRemoteHost(userToHostData);
//			System.out.println("\tSend initial request");

			// Get initial reply
			hostToUserData = getResponseFromRemoteHost();
//			System.out.println("\tReceived intial reply");

			// Pass initial return to user
			returnResponseFromHostToUser(hostToUserData);
//			System.out.println("\tReturned initial reply");

			while (clientToProxySocket.isConnected()
					&& proxyToServerSocket.isConnected()) {

				if (outgoingInputStream.available() != 0) {
					// Get reply
					hostToUserData = getResponseFromRemoteHost();
//					System.out.println("\tReceived reply");

					// Pass return to user
					returnResponseFromHostToUser(hostToUserData);
//					System.out.println("\tReturned reply");
				}
				if (incomingInputStream.available() != 0) {

					// Read data passed from a client
					userToHostData = getDataFromUserToRemoteHot();
//					System.out.println("\tRead request");

					// Send initial request
					sendUserRequestToRemoteHost(userToHostData);
//					System.out.println("\tSent request");
				} else {
					System.out.println("No more data to pass to user");
				}
				if (incomingInputStream.available() == 0
						&& outgoingInputStream.available() == 0) {
//					System.out.println("Neither have data to return");
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (incomingInputStream.available() == 0
							&& outgoingInputStream.available() == 0) {
//						System.out.println("Wait finished, still nothing.");
						break;
					}
				}
			}
//			System.out.println("Closing streams");
			closeConnection();
//			System.out.println("DEBUG FINISH");
			return;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void returnResponseFromHostToUser(byte[] byteBuffer)
			throws IOException {
		incomingOutputStream.write(byteBuffer, 0, byteBuffer.length);
		incomingOutputStream.flush();
	}

	private void sendUserRequestToRemoteHost(byte[] byteBuffer)
			throws IOException {
		outgoingOutputStream.write(byteBuffer, 0, byteBuffer.length);
		outgoingOutputStream.flush();
	}

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
					Level.INFO,
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
					Level.INFO,
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

}