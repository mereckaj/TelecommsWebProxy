package com.mereckaj.webproxy;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.mereckaj.webproxy.utils.Utils;

public class ProxyDataLogger {
	private static ProxyDataLogger instance = new ProxyDataLogger();
	private Logger log;
	private FileHandler fileHandle;
	private String pathToLog;

	/**
	 * private constructor because accessing this class should be done by
	 * ProxyLogger.getInstance()
	 * 
	 * Checks if the log has been created, if not then create it.
	 * 
	 * New log is created each time the proxy is started with the name: .log_,
	 * followed by the current date and time
	 */
	private ProxyDataLogger() {
		if (log == null) {
			log = Logger.getLogger("ProxyDataLog");
			pathToLog = ProxySettings.getInstance().getPathToLog();
			try {
				fileHandle = new FileHandler(pathToLog + "_"
						+ Utils.getCurrentDate());
				log.addHandler(fileHandle);
				fileHandle.setFormatter(new SimpleFormatter());
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (!ProxySettings.getInstance().isLoggingEnabled()) {
			log.setLevel(Level.OFF);
		}
	}

	/**
	 * get the instance of this class
	 * 
	 * @return ProxyLogger instance
	 */
	public static ProxyDataLogger getInstance() {
		return instance;
	}

	/**
	 * indirect access to loggers log function
	 * 
	 * @param l
	 *            = Level of the message
	 * @param m
	 *            = message to log
	 */
	public void log(Level l, String m) {
		log.log(l, m);
	}
}
