package com.mereckaj.webproxy;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.mereckaj.webproxy.utils.Utils;

public class ProxyLogger {
	private Logger log;
	private FileHandler fileHandle;
	private static ProxyLogger instance = new ProxyLogger();
	
	/**
	 * private constructor because accessing this class should be done by ProxyLogger.getInstance()
	 * 
	 * Checks if the log has been created, if not then create it.
	 * 
	 * New log is created each time the proxy is started 
	 * with the name: .log_, followed by the current date and time
	 */
	private ProxyLogger() {
		if (log == null) {
			log = Logger.getLogger("ProxyLog");
			try {
				fileHandle = new FileHandler(".log_"+Utils.getCurrentDate());
				log.addHandler(fileHandle);
				fileHandle.setFormatter(new SimpleFormatter());
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	/**
	 * get the instance of this class
	 * @return ProxyLogger instance
	 */
	public static ProxyLogger getInstance() {
		return instance;
	}
	
	/**
	 * indirect access to loggers log function
	 * @param l = Level of the message
	 * @param m = message to log
	 */
	public void log(Level l, String m) {
		log.log(l, m);
	}
}
