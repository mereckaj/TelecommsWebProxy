package com.mereckaj.webproxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.Level;

import com.mereckaj.webproxy.utils.Utils;

/*
 * Starting function of the proxy, class will call the main
 * method which initializes the proxy.
 */
public class Main {
	
	/*
	 * Main socket to which all of the users will connect.
	 */
	static ServerSocket socket;
	
	/*
	 * Port on which the ServerSocket will bind,
	 * 
	 * This value will be set with the value found in the 
	 * config file
	 */
	static int port;
	
	/*
	 * main method that initializes and launches the proxy
	 */
	public static void main(String[] args) {

		/*
		 *  Read in the port specified by the config file
		 */
		port = ProxySettings.getInstance().getProxyPort();
		
		try {
			/*
			 * Attempt to bind to that port
			 */
			socket = new ServerSocket(port);
			
		} catch (IOException e) {
			
			/*
			 * log details to log file and quit, this error is really bad
			 */
			ProxyLogger.getInstance().log(Level.SEVERE,
					"Couldn't create a new server socket: " + e.getMessage());
			
			/*
			 * Exit with an abnormal exit code.
			 * Exit code into can be found at ExitCoes file
			 */
			System.exit(1);
		}
		
		/*
		 * Insert a log entry that the proxy has successfully started at current time
		 */
		ProxyDataLogger.getInstance().log(Level.INFO, "Started proxy at: " + Utils.getCurrentDate());
		
		/*
		 * Proxy will run until ProxySettings.isRunning == true
		 */
		while (ProxySettings.getInstance().getRunning()) {
			
			try {
				
				/*
				 * Attempt to create a new thread that will process this request and start it
				 */
				new ProxyWorkerThread(socket.accept()).start();
			} catch (IOException e) {
				
				/*
				 * log details to log file,
				 * not shutting down because maybe it was just this request
				 * that caused some errro.
				 */
				ProxyLogger
						.getInstance()
						.log(Level.WARNING,
								"Could not create new thread to deal with a new connection");
			}
		}
		
		/*
		 * Exit with normal exit code.
		 * Exit code info can be found it ExitCodes file
		 */
		System.exit(0);
	}
}
