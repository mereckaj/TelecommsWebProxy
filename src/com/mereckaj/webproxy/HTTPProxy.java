package com.mereckaj.webproxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.Level;

import com.mereckaj.webproxy.utils.Utils;

/**
 * This class creates a new instance of a HTTP Proxy. This class extends thread,
 * so to run the proxy use run() or start() methods.
 * 
 * @author julius
 * 
 */
public class HTTPProxy extends Thread {
    static ServerSocket socket;
    static int port;
    private ProxyDataLogger dataLogger;
    private ProxyLogger logger;
    private ProxySettings settings;

    /**
     * Instantiate some of the static resources that will be used
     * inside this class.
     */
    public HTTPProxy() {
	dataLogger = ProxyDataLogger.getInstance();
	logger = ProxyLogger.getInstance();
	settings = ProxySettings.getInstance();
    }

    public void run() {
	port = ProxySettings.getInstance().getProxyPort();
	try {
	    socket = new ServerSocket(port);
	    dataLogger.log(Level.INFO, "Start: " + Utils.getCurrentDate());

	    /*
	     * Main proxy loop. Each time the socket accepts a connection it
	     * will span a new worker thread that will deal with it.
	     */
	    while (settings.getRunning()) {
		new ProxyWorkerThread(socket.accept()).start();
	    }

	} catch (IOException e) {
	    logger.log(Level.SEVERE, "Error creating proxy" + e.getMessage());
	    System.exit(1);
	}
    }
}
