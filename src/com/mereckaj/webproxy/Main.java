package com.mereckaj.webproxy;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.StringTokenizer;
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
//				new ProxyWorkerThread(socket.accept()).start();
				doStuff(socket.accept());
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
	int BUFFER_SIZE = 65536;
	private static void doStuff(Socket socket) {

        //get input from user
        //send request to server
        //get response from server
        //send response to user

        try {
            DataOutputStream out =
		new DataOutputStream(socket.getOutputStream());
            BufferedReader in = new BufferedReader(
		new InputStreamReader(socket.getInputStream()));

            String inputLine, outputLine;
            int cnt = 0;
            String urlToCall = "";
            ///////////////////////////////////
            //begin get request from client
            while ((inputLine = in.readLine()) != null) {
                try {
                    StringTokenizer tok = new StringTokenizer(inputLine);
                    tok.nextToken();
                } catch (Exception e) {
                    break;
                }
                //parse the first line of the request to find the url
                if (cnt == 0) {
                    String[] tokens = inputLine.split(" ");
                    urlToCall = tokens[1];
                    //can redirect this to output log
                    System.out.println("Request for : " + urlToCall);
                }

                cnt++;
            }
            //end get request from client
            ///////////////////////////////////


            BufferedReader rd = null;
            try {
                //System.out.println("sending request
		//to real server for url: "
                //        + urlToCall);
                ///////////////////////////////////
                //begin send request to server, get response from server
                URL url = new URL(urlToCall);
                URLConnection conn = url.openConnection();
                conn.setDoInput(true);
                //not doing HTTP posts
                conn.setDoOutput(false);
                //System.out.println("Type is: "
			//+ conn.getContentType());
                //System.out.println("content length: "
			//+ conn.getContentLength());
                //System.out.println("allowed user interaction: "
			//+ conn.getAllowUserInteraction());
                //System.out.println("content encoding: "
			//+ conn.getContentEncoding());
                //System.out.println("content type: "
			//+ conn.getContentType());

                // Get the response
                InputStream is = null;
                HttpURLConnection huc = (HttpURLConnection)conn;
                if (conn.getContentLength() > 0) {
                    try {
                        is = conn.getInputStream();
                        rd = new BufferedReader(new InputStreamReader(is));
                    } catch (IOException ioe) {
                        System.out.println(
				"********* IO EXCEPTION **********: " + ioe);
                    }
                }
                //end send request to server, get response from server
                ///////////////////////////////////

                ///////////////////////////////////
                //begin send response to client
                byte by[] = new byte[ BUFFER_SIZE ];
                int index = is.read( by, 0, BUFFER_SIZE );
                while ( index != -1 )
                {
                  out.write( by, 0, index );
                  index = is.read( by, 0, BUFFER_SIZE );
                }
                out.flush();

                //end send response to client
                ///////////////////////////////////
            } catch (Exception e) {
                //can redirect this to error log
                System.err.println("Encountered exception: " + e);
                //encountered error - just send nothing back, so
                //processing can continue
                out.writeBytes("");
            }
	}
}
