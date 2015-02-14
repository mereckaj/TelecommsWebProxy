package com.mereckaj.webproxy;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.mereckaj.webproxy.utils.Utils;

/**
 * This class logs any exceptions or messages that were not related to the
 * proxying of data<br>
 * It will log errors and config changes
 * 
 * This is a singleton object and the constructor is private.<br>
 * This means that the only way to get an instance of this object is through
 * getInstance() method.
 * 
 * @author julius
 * 
 */
public class ProxyLogger {
    private Logger log;
    private FileHandler fileHandle;
    private static ProxyLogger instance = new ProxyLogger();

    private ProxyLogger() {
	/*
	 *  If this is the first instantiation of this object, set up the 
	 *  file handles and so on.
	 */
	if (log == null) {
	    log = Logger.getLogger("ProxyLog");
	    try {
		fileHandle = new FileHandler(".log_" + Utils.getCurrentDate());
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
     * Get the instance of this object
     * @return instance of this object
     */
    public static ProxyLogger getInstance() {
	return instance;
    }
    
    /**
     * Log a message at level {@link Level} or {@link ProxyLogLevel}
     * @param l level to use for logging
     * @param m message to add to the log
     */
    public void log(Level l, String m) {
	log.log(l, m);
    }
}
