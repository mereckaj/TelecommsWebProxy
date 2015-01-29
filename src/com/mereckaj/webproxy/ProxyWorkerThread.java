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
	private static final boolean filteringEnabled = ProxySettings.getInstance()
			.isFilteringEnabled();
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
	 * Temporary char and byte buffers TODO: Stop using char buffer
	 */
	char[] charTmp;
	byte[] byteTmp;

	/**
	 * Constructor for this class.
	 * 
	 * @param s
	 *            = {@link Socket} socket which is received from {@link Main}
	 */
	public ProxyWorkerThread(Socket s) {
		this.clientToProxySocket = s;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 * 
	 * This function will do all of the work for this thread.
	 * 
	 * It will take the data being passed by the client, parse it, create a
	 * connection to some host, receive the reply, pass the reply back to the
	 * user.
	 */
	public void run() {
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
			charTmp = new char[ProxySettings.getInstance().getMaxBuffer()];
			int size = incomingBufferedReader.read(charTmp, 0, charTmp.length);
			char[] charBuffer = new char[size];
			System.arraycopy(charTmp, 0, charBuffer, 0, size);

			/*
			 * Extract the url from payload
			 */
			URL url = new URL(Utils.getUrl(charBuffer));

			/*
			 * Check if filtering is enabled, if so Check if the host is
			 * blocked,
			 * 
			 * If it is, log the attempted access and perform some action
			 * indicated by doActionIfBlocked
			 * 
			 * (return some status and close socket)
			 */
			if (filteringEnabled && isBlockedHost(url.getHost())) {
				ProxyDataLogger.getInstance().log(
						Level.INFO,
						"Blocked :" + url.getHost() + " from: "
								+ clientToProxySocket.getInetAddress() + " blocked host");
				doActionIfBlocked();
			}
			/*
			 * Host is not blocked so check the content for may violations it.
			 * 
			 * If yes, then log the violation and do some action as specified by
			 * doActionIfBlocked(); (Check the response from server afterwards)
			 */
			if (filteringEnabled && containsBlockedContent()) {
				ProxyDataLogger.getInstance().log(
						Level.INFO,
						"Blocked :" + url.getHost() + " from: "
								+ clientToProxySocket.getInetAddress()
								+ " contenct violation");
				doActionIfBlocked();
			}

			/*
			 * Content isn't blocked and host isn't blocked, forward the
			 * connection
			 */
			InetAddress addr = InetAddress.getByName(url.getHost());
			System.out.println(addr.getHostAddress());
			proxyToServerSocket = new Socket(addr.getHostAddress(),
					url.getPort() == -1 ? url.getDefaultPort() : url.getPort());
			System.out.println(proxyToServerSocket.isConnected());
			
			/*
			 * Read client inStream
			 */
			//TODO:
			/*
			 * Write this data to server socket
			 */

			//TODO:
			/*
			 * Get response from server
			 */

			//TODO:
			/*
			 * Return response to user
			 */

			//TODO:
			/*
			 * Close sockets and data streams
			 */
			clientToProxySocket.close();

		} catch (IOException e) {

			/*
			 * Something bad happened, log the problems occurrence and refuse
			 * connection
			 */
			ProxyLogger.getInstance().log(
					Level.SEVERE,
					"Could not get the input/output streams from the socket"
							+ clientToProxySocket.hashCode());
			closeConnection();
			e.printStackTrace();
		}
	}

	/*
	 * Method used to close the connection. Should be called if an error
	 * occurred or content/host contain disallowed items.
	 */
	private void closeConnection() {
		try {
			clientToProxySocket.close();
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