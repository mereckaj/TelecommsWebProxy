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

	
	public static ProxyDataLogger getInstance() {
		return instance;
	}

	
	public void log(Level l, String m) {
		log.log(l, m);
	}
}
