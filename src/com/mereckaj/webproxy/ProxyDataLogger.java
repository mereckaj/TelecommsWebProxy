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
public class ProxyDataLogger {
    // Static instance of this object
    private static ProxyDataLogger instance = new ProxyDataLogger();

    private Logger log;
    private FileHandler fileHandle;
    private String pathToLog;
    private ProxySettings settings;

    private ProxyDataLogger() {
	if(settings==null){
	    settings = ProxySettings.getInstance();
	}
	/*
	 * If this is the first time the logger is being instantiated
	 * then create a new file handle to the log file
	 */
	if (log == null) {
	    log = Logger.getLogger("ProxyDataLog");
	    pathToLog = settings.getPathToLog();
	    try {
		fileHandle = new FileHandler(pathToLog + "_" + Utils.getCurrentDate());
		log.addHandler(fileHandle);
		fileHandle.setFormatter(new SimpleFormatter());
	    } catch (SecurityException e) {
		e.printStackTrace();
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
	if (!settings.isLoggingEnabled()) {
	    log.setLevel(Level.OFF);
	}
    }
    
    /**
     * This method should be used to obtain an isntance of this class.
     * @return instance of this class.
     */
    public static ProxyDataLogger getInstance() {
	return instance;
    }
    
    /**
     * Log a message at a given level
     * @param l {@link Level} or {@link ProxyLogLevel} at which the message should be logged
     * @param m message to add to the log
     */
    public void log(Level l, String m) {
	log.log(l, m);
    }
    
    /**
     * Method used by the GUI to disable or enable logging in real time.
     */
    public void setEnabled(boolean e) {
	if (e == false) {
	    log.setLevel(Level.OFF);
	} else {
	    if (log.getLevel() == Level.OFF) {
		log.setLevel(Level.ALL);
	    }
	}
    }
}
