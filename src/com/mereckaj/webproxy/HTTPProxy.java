package com.mereckaj.webproxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.Level;

import com.mereckaj.webproxy.utils.Utils;


public class HTTPProxy extends Thread{
	public HTTPProxy(){
		
	}
	
	
	static ServerSocket socket;
	
	
	static int port;
	
	
	public void run() {

		
		port = ProxySettings.getInstance().getProxyPort();
		
		try {
			
			socket = new ServerSocket(port);
			
		} catch (IOException e) {
			
			
			ProxyLogger.getInstance().log(Level.SEVERE,
					"Couldn't create a new server socket: " + e.getMessage());
			
			
			System.exit(1);
		}
		
		
		ProxyDataLogger.getInstance().log(Level.INFO, "Started proxy at: " + Utils.getCurrentDate());
		
		
		while (ProxySettings.getInstance().getRunning()) {
			
			try {
				
				
				new ProxyWorkerThread(socket.accept()).start();
			} catch (IOException e) {
				
				
				ProxyLogger
						.getInstance()
						.log(Level.WARNING,
								"Could not create new thread to deal with a new connection");
			}
		}
		
		
		System.exit(0);
	}
}
