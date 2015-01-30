package com.mereckaj.webproxy;

import java.io.BufferedOutputStream;
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

import org.apache.commons.io.IOUtils;

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
			byteTmp = new byte[ProxySettings.getInstance().getMaxBuffer()];
			int size = incomingInputStream.read(byteTmp, 0, byteTmp.length);
			byte[] byteBuffer = new byte[size];
			System.arraycopy(byteTmp, 0, byteBuffer, 0, size);

			/*
			 * Create a string representation of the http header
			 */
			String payloadAsString = new String(byteBuffer);

			/*
			 * Extract the url from payload
			 */
			URL url = new URL(Utils.getUrl(payloadAsString));

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
								+ clientToProxySocket.getInetAddress()
								+ " blocked host");
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
			 * Log connection request
			 */
			ProxyDataLogger.getInstance().log(Level.INFO, "Connect: ");

			/*
			 * Content isn't blocked and host isn't blocked, forward the
			 * connection
			 */
			InetAddress serverAddress = InetAddress.getByName(url.getHost());
			proxyToServerSocket = new Socket(serverAddress, 80);
			outgoingOutputStream = proxyToServerSocket.getOutputStream();
			outgoingInputStream = proxyToServerSocket.getInputStream();
			while (proxyToServerSocket.isConnected()) {
				
				/*
				 * 
				 */
				outgoingOutputStream.write(byteBuffer, 0, byteBuffer.length);
				outgoingOutputStream.flush();

				/*
				 * Get response from server
				 */
				byteTmp = new byte[ProxySettings.getInstance().getMaxBuffer()];
				size = outgoingInputStream.read(byteTmp, 0, byteTmp.length);
				if (size <= 0) {
					break;
				}
				byteBuffer = new byte[size];
				System.arraycopy(byteTmp, 0, byteBuffer, 0, size);

				String replyAsString = new String(byteBuffer);
				System.out.println("DEBUG:\n" + replyAsString
						+ "\nDEBUG FINISHED");

				/*
				 * Return response to user
				 */
				incomingOutputStream.write(byteBuffer, 0, byteBuffer.length);
				incomingOutputStream.flush();
			}
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