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
	
	public static ProxyLogger getInstance() {
		return instance;
	}
	
	
	public void log(Level l, String m) {
		log.log(l, m);
	}
}
