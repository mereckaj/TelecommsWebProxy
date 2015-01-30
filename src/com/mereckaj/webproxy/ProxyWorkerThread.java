package com.mereckaj.webproxy;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.logging.Level;

import com.mereckaj.webproxy.utils.Utils;

/**
 * This {@link Thread} object gets created each time {@link Main} gets a request
 * from a user to connect to some host.
 */
public class ProxyWorkerThread extends Thread {

	/*
	 * User socket, passed in through the constructor
	 */
	Socket clientToProxySocket;

	/*
	 * Output stream that will be used to pass user the response from the host
	 * it tried to connect with
	 */
	DataOutputStream incomingOutputStream;

	/*
	 * Input stream that will be parsed and used to create new connection
	 * details by the proxy
	 */
	InputStream incomingInputStream;

	/*
	 * Reader used to make reading of input stream easier
	 */
	BufferedReader incomingBufferedReader;

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
	 * 
	 * This function will do all of the work for this thread.
	 * 
	 * It will take the data being passed by the client, parse it, create a
	 * connection to host, receive the reply, pass the reply back to the user.
	 */
	public void run() {
		byte[] byteBuffer;
		try {
			/*
			 * set up the input and output streams as well as the buffered
			 * reader from the input stream
			 */
			incomingOutputStream = new DataOutputStream(
					clientToProxySocket.getOutputStream());
			incomingInputStream = clientToProxySocket.getInputStream();
			incomingBufferedReader = new BufferedReader(new InputStreamReader(
					incomingInputStream));

			/*
			 * Get the data that was contained inside the packet being
			 * transmitted
			 */
			System.out.println("1");
			byteBuffer = getDataFromUserToRemoteHot();

			/*
			 * Extract the url from payload TODO: Replace with http header
			 * parser method
			 */
			URL url = new URL(Utils.getUrl(new String(byteBuffer)));

			/*
			 * Check if filtering is enabled, if so Check if the host is
			 * blocked,
			 * 
			 * If it is, log the attempted access and perform some action
			 * indicated by doActionIfBlocked
			 * 
			 * (return some status and close socket)
			 */
			filterHost(url.getHost());

			/*
			 * Host is not blocked so check the content for may violations it.
			 * 
			 * If yes, then log the violation and do some action as specified by
			 * doActionIfBlocked(); (Check the response from server afterwards)
			 */
			filterContent(url.getHost());

			/*
			 * Log connection request TODO: Better logging info is needed TODO:
			 * Replace with http header parser info
			 */
			ProxyDataLogger.getInstance().log(Level.INFO,
					"Connect: " + url.getHost());

			/*
			 * Create a new socket from the proxy to the host. TODO: Use http
			 * header parser to get host
			 */
			InetAddress serverAddress = InetAddress.getByName(url.getHost());
			proxyToServerSocket = new Socket(serverAddress, 80);

			/*
			 * Get the necessary streams from this new socket.
			 */
			outgoingOutputStream = proxyToServerSocket.getOutputStream();
			outgoingInputStream = proxyToServerSocket.getInputStream();

			/*
			 * Request from client is already read in. Pass it onto the host it
			 * is trying to reach.
			 */
			System.out.println("2");
			sendClientRequestToRemoteHost(byteBuffer);

			/*
			 * While the host has data to receive, keep passing it back to the
			 * user
			 */
			while (proxyToServerSocket.isConnected()&&clientToProxySocket.isConnected()) {

				/*
				 * Get response from server
				 * 
				 * getResponseFromRemoteHost returns null if there's nothing to
				 * return.
				 */
				byteBuffer = getResponseFromRemoteHost();
				System.out.println("3");
				if (byteBuffer == null) {
					System.out.println("4");
					/*
					 * Check if any new request for data transfer from client to
					 * host have been made.
					 * 
					 * getDataFromUserToRemoteHost returns null if there is no more
					 * data
					 */
					byteBuffer = getDataFromUserToRemoteHot();
					System.out.println("5");
					if (byteBuffer == null) {
						break;
					}
					
					/*
					 * Send the data on and loop back to the top to get the reply.
					 */
					sendClientRequestToRemoteHost(byteBuffer);
					System.out.println("6");
					returnResponseFromHostToClient(byteBuffer);
					System.out.println("7");
				}else{
					System.out.println("8");
					/*
					 * Return response to user
					 */
					returnResponseFromHostToClient(byteBuffer);
				}
				System.out.println("9");
			}
			
			/*
			 * Close connections as there will be no more communications between
			 * the client and host.
			 */
			closeConnection();

			/*
			 * Log that proxying has finished
			 */
			ProxyDataLogger.getInstance().log(Level.INFO,
					"Disonnect: " + url.getHost());

			/*
			 * Log usage statistics
			 */
			// TODO: Log usage statistics

		} catch (IOException e) {

			/*
			 * Something bad happened, log the problems occurrence and refuse
			 * connection
			 */
			ProxyLogger.getInstance().log(Level.SEVERE,
					e.getMessage() + " " + clientToProxySocket.hashCode());
			closeConnection();
			e.printStackTrace();
		}
	}

	private void returnResponseFromHostToClient(byte[] byteBuffer)
			throws IOException {
		incomingOutputStream.write(byteBuffer, 0, byteBuffer.length);
		incomingOutputStream.flush();
	}

	private void sendClientRequestToRemoteHost(byte[] byteBuffer)
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
	private void filterContent(String host) {
		if (filteringEnabled && containsBlockedContent()) {
			ProxyDataLogger.getInstance().log(
					Level.INFO,
					"Blocked :" + host + " from: "
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

	private boolean containsBlockedContent() {
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