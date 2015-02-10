package com.mereckaj.webproxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

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

	public ProxyWorkerThread(Socket s) {
		this.userToProxySocket = s;
		filteringEnabled = ProxySettings.getInstance().isFilteringEnabled();
	}

	public void run() {

		byte[] userToHostData;
		byte[] hostToUserData;

		HttpRequestParser httpRequestHeader = null;
		HttpResponseParser httpResponseHeader = null;
		try {

			incomingInputStream = userToProxySocket.getInputStream();
			incomingOutputStream = userToProxySocket.getOutputStream();

			userToHostData = getDataFromUserToRemoteHost();
			if (userToHostData == null) {
				return;
			}

			httpRequestHeader = new HttpRequestParser(userToHostData);

			if (filterHost(httpRequestHeader.getHost())) {
				return;
			}

			if (filterContent(userToHostData)) {
				return;
			}

			proxyToServerSocket = new Socket(httpRequestHeader.getHost(),
					HTTP_PORT);
			ProxyDataLogger.getInstance().log(
					ProxyLogLevel.CONNECT,
					"Connected: " + httpRequestHeader.getHost() + " For: "
							+ httpRequestHeader.getUrl() + " Port: "
							+ HTTP_PORT);

			outgoingInputStream = proxyToServerSocket.getInputStream();
			outgoingOutputStream = proxyToServerSocket.getOutputStream();

			sendUserRequestToRemoteHost(userToHostData);

			hostToUserData = getResponseFromRemoteHost();

			httpResponseHeader = new HttpResponseParser(hostToUserData);

			returnResponseFromHostToUser(hostToUserData);

			while (userToProxySocket.isConnected()
					&& proxyToServerSocket.isConnected()) {

				if (outgoingInputStream.available() != 0) {

					hostToUserData = getResponseFromRemoteHost();

					returnResponseFromHostToUser(hostToUserData);
				}
				if (incomingInputStream.available() != 0) {

					userToHostData = getDataFromUserToRemoteHost();

					if (filterContent(userToHostData)) {
						return;
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

			ProxyDataLogger.getInstance().log(ProxyLogLevel.DISCONNECT,
					"Disconnected:" + httpRequestHeader.getHost());

			closeConnection();

			closeDataStreams();

		} catch (IOException e) {
			ProxyLogger.getInstance().log(ProxyLogLevel.EXCEPTION,
					httpRequestHeader.getHost() + ": " + e.getMessage());
			e.printStackTrace();
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

	private void returnResponseFromHostToUser(byte[] byteBuffer)
			throws IOException {
		incomingOutputStream.write(byteBuffer, 0, byteBuffer.length);
		incomingOutputStream.flush();
	}

	private void sendUserRequestToRemoteHost(byte[] byteBuffer)
			throws IOException {
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
			ProxyDataLogger.getInstance().log(
					ProxyLogLevel.INFO,
					"Blocked: from: " + userToProxySocket.getInetAddress()
							+ " contenct violation");
			doActionIfBlocked();
			return true;
		}
		return false;
	}

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
			boolean hostIsBlocked = ProxyTrafficFilter.getInstance()
					.isBlockedHost(host);
			boolean ipIsBlocked = ProxyTrafficFilter.getInstance().isBlockedIP(
					ip);
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