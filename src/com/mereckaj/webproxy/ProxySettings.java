package com.mereckaj.webproxy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;

import com.mereckaj.webproxy.utils.Utils;

/**
 * This class deals with reading/writing changes to the config file.
 * 
 * This is a singleton object. This means the constructor is private. 
 * getInstance() method should be used to get an isntance of this class.
 * @author julius
 *
 */
public class ProxySettings {

    private final String CONFIG_FILE_PATH = ".proxy_config";

    private int proxyPort;
    private int maxBufferSize;
    private boolean loggingEnabled;
    private boolean filterEnabled;

    private boolean running = true;

    private String pathToLog;
    private String pathToFilters;
    private String pathToBlocked;

    private static ProxySettings instance = new ProxySettings();

    private byte[] refusedData;
    private BufferedReader configFile;
    private ProxyLogger logger;

    private ProxySettings() {
	if(maxBufferSize==0){
	    maxBufferSize = 65536;
	}
	if(proxyPort==0){
	    proxyPort=8080;
	}
	if (logger == null) {
	    logger = ProxyLogger.getInstance();
	}
	if (configFile == null) {
	    configFile = Utils.openOrCreateFile(CONFIG_FILE_PATH);
	}
	if (refusedData == null) {
	    Path path = Paths.get("proxy_refused_connection.html");
	    try {
		refusedData = Files.readAllBytes(path);
	    } catch (IOException e) {
		logger.log(ProxyLogLevel.EXCEPTION,
			"Could not read proxy_refused_connection.html file");
	    }
	}
    }
    
    /**
     * Returns an instance of this object.
     * @return instance of this object
     */
    public static ProxySettings getInstance() {
	return instance;
    }

    /**
     * Parse the config file. All it this does is reads in a line from a file
     * and checks what it was if it contains some sort of a value that is used
     * by the proxy it will parse it. otherwise it will ignore it.
     */
    private void parseConfigFile() {
	if (configFile != null) {
	    String currentLine;
	    try {
		while ((currentLine = configFile.readLine()) != null) {

		    if (!currentLine.startsWith("#") && !currentLine.isEmpty()) {
			if (currentLine.contains("PROXY_PORT")) {
			    proxyPort = Integer.parseInt(getAfterEquals(currentLine));
			    logger.log(Level.INFO, "Proxy port: " + proxyPort);
			} else if (currentLine.contains("LOGGING_ENABLED")) {
			    if (Integer.parseInt(getAfterEquals(currentLine)) == 0) {
				loggingEnabled = false;
			    } else if (Integer.parseInt(getAfterEquals(currentLine)) == 1) {
				loggingEnabled = true;
			    } else {
				loggingEnabled = false;
				logger.log(Level.WARNING,
					"Config file contained invalid value for LOGGING_ENABLED");
			    }
			    logger.log(Level.INFO, "Logging enabled: " + loggingEnabled);
			} else if (currentLine.contains("FILTER_CONTENT")) {
			    if (Integer.parseInt(getAfterEquals(currentLine)) == 0) {
				filterEnabled = false;
			    } else if (Integer.parseInt(getAfterEquals(currentLine)) == 1) {
				filterEnabled = true;
			    } else {
				filterEnabled = false;
				logger.log(Level.WARNING,
					"Config file contained invalid value for FILTER_CONTENT");
			    }
			    logger.log(Level.INFO, "Filtering enabled: " + filterEnabled);
			} else if (currentLine.contains("PATH_LOG")) {
			    String path = getAfterEquals(currentLine);
			    pathToLog = path;
			    logger.log(Level.INFO, "Path to log: " + pathToLog);
			} else if (currentLine.contains("PATH_FILTER")) {
			    String path = getAfterEquals(currentLine);
			    pathToFilters = path;
			    logger.log(Level.INFO, "Path to filters: " + pathToFilters);
			} else if (currentLine.contains("PATH_BLOCKED")) {
			    String path = getAfterEquals(currentLine);
			    pathToBlocked = path;
			    logger.log(Level.INFO, "Path to blocked ip: " + pathToBlocked);
			} else if (currentLine.contains("MAX_BUFFER_SIZE")) {
			    maxBufferSize = Integer.parseInt(getAfterEquals(currentLine));
			    logger.log(Level.INFO, "Max buffer size: " + maxBufferSize);
			} else {
			    logger.log(Level.WARNING, "Found something weird in the log file: "
				    + currentLine);
			}
		    }
		}
	    } catch (IOException e) {
		logger.log(ProxyLogLevel.EXCEPTION, "Exception: " + e.getMessage());
	    }
	} else {
	    logger.log(Level.SEVERE, "Config file was not found");
	}
    }
    
    /**
     * This method is called by the GUI when the "SAVE" button is hit in the config editor
     * or when the proxy is turned off. This makes sure that any changes made to the proxy while
     * it was running are saved.
     * 
     * This method saves config changes and additions or removals from blocked Host/IP/Phrase lists
     */
    public void writeConfigHostIpPhraseDataToFile() {
	writeConfigToFile();
	writeBlockedHostIpToFile();
	writeFilteredPhrasesToFile();
    }
    
    /**
     * What a nasty implementation.
     * This just writes the blocked phrases to file.
     */
    private void writeFilteredPhrasesToFile() {
	File f = new File(pathToFilters);
	if (!f.exists()) {
	    logger.log(ProxyLogLevel.WARNING, "No filter file found");
	} else {
	    try {
		FileWriter fw = new FileWriter(f.getAbsoluteFile());
		BufferedWriter br = new BufferedWriter(fw);

		br.write("# This file contains blocked phrases\n"
			+ "# Each new line is threaded as a phrase which the program will try to match\n"
			+ "# Lines starting with # or \\n are ignored\n" + "# Proxy ignores case\n"
			+ "# This will even block HTML/CSS/JS words. Bug/Feature ? you decide.\n");
		List<String> plist = ProxyTrafficFilter.getInstance().getBlockedPhraseList();
		for (int i = 0; i < plist.size(); i++) {
		    br.write(plist.get(i) + "\n");
		}
		br.close();
	    } catch (IOException e) {
		logger.log(ProxyLogLevel.EXCEPTION,
			"Could not write to filters file: " + e.getMessage());
	    }
	}

    }
    
    /**
     * What a nasty implementation.
     * This just writes the blocked hosts and IP's to file.
     */
    private void writeBlockedHostIpToFile() {
	File f = new File(pathToBlocked);
	if (!f.exists()) {
	    logger.log(ProxyLogLevel.WARNING, "No blocked ip/host file found");
	} else {
	    try {
		FileWriter fw = new FileWriter(f.getAbsoluteFile());
		BufferedWriter br = new BufferedWriter(fw);

		br.write("# This file contains lists of blocked hosts\n" + "# and blocked IP's.\n"
			+ "# Hosts and IP's must be in the correct block,\n"
			+ "# denoted by [HOST] or [IP].\n"
			+ "# Removal of [HOST] or [IP] might cause undefined behaviour\n"
			+ "# by the program.\n\n");
		br.write("# Below is a list of blocked host names.\n"
			+ "# Each new blocked host must be entered on a new line\n" + "[HOST]\n");
		List<String> hlist = ProxyTrafficFilter.getInstance().getBlockedHostList();
		for (int i = 0; i < hlist.size(); i++) {
		    br.write(hlist.get(i) + "\n");
		}
		br.write("\n");
		br.write("# Below is a list of blocked IP's\n"
			+ "# Each new blocked ip must be entered on a new line\n" + "[IP]\n");
		List<String> iplist = ProxyTrafficFilter.getInstance().getBlockedIpList();
		for (int i = 0; i < iplist.size(); i++) {
		    br.write(iplist.get(i) + "\n");
		}
		br.write("\n");
		br.close();
	    } catch (IOException e) {
		logger.log(ProxyLogLevel.EXCEPTION,
			"Could not write to filters file: " + e.getMessage());
	    }
	}
    }
    
    /**
     * What a nasty implementation.
     * This just writes the config to file.
     */
    private void writeConfigToFile() {
	File f = new File(CONFIG_FILE_PATH);
	if (!f.exists()) {
	    logger.log(ProxyLogLevel.SEVERE, "Log file not found, How did you start the program ?");
	} else {
	    try {
		FileWriter fw = new FileWriter(f.getAbsoluteFile());
		BufferedWriter br = new BufferedWriter(fw);
		br.write("# This file contains the settings that the proxy will load on start up\n");
		br.write("# Author Julius Mereckas\n\n");
		br.write("#Set the port to which the proxy will bind\n");
		br.write("PROXY_PORT = " + proxyPort + "\n\n");
		br.write("#If logging is disabled no writes to log will made \n"
			+ "#Management engine will not do anything\n" + "# 0 = disabled\n"
			+ "# 1 = enabled\n");
		br.write("LOGGING_ENABLED = " + (loggingEnabled == true ? "1" : "0") + "\n\n");
		br.write("#If enabled the content will be filtered for any phrases"
			+ "in the .proxy_filter file\n" + "#and for any ip's in .proxy_blocked\n"
			+ "# 0 = disabled\n" + "# 1 = enabled\n" + "# default = disabled\n");
		br.write("FILTER_CONTENT = " + (filterEnabled == true ? "1" : "0") + "\n\n");
		br.write("#Path to log file\n");
		br.write("PATH_LOG = " + pathToLog + "\n\n");
		br.write("#Path to filters file\n");
		br.write("PATH_FILTERS = " + pathToFilters + "\n\n");
		br.write("#Path to blocked ip/host file\n");
		br.write("PATH_BLOCKED = " + pathToBlocked + "\n\n");
		br.write("#Maximum size for the buffer in each thread\n");
		br.write("MAX_BUFFER_SIZE = " + maxBufferSize + "\n\n");
		br.close();
	    } catch (IOException e) {
		logger.log(ProxyLogLevel.EXCEPTION, "Unable to write config file " + e.getMessage());
	    }
	}

    }

    private static String getAfterEquals(String s) {
	return s.substring(s.indexOf('=') + 1).trim();
    }

    public int getProxyPort() {
	if (proxyPort == 0) {
	    parseConfigFile();
	}
	return proxyPort;
    }

    public boolean isLoggingEnabled() {
	return loggingEnabled;
    }

    public boolean isFilteringEnabled() {
	return filterEnabled;
    }

    public String getPathToLog() {
	if (pathToLog == null) {
	    parseConfigFile();
	}
	return pathToLog;
    }

    public String getPathToFilter() {
	if (pathToFilters == null) {
	    parseConfigFile();
	}
	return pathToFilters;
    }

    public String getPathToBlocked() {
	if (pathToBlocked == null) {
	    parseConfigFile();
	}
	return pathToBlocked;
    }

    public boolean getRunning() {
	return running;
    }

    public void setRunning(boolean v) {
	if (v == false) {
	    writeConfigHostIpPhraseDataToFile();
	}
	running = v;
    }

    public int getMaxBuffer() {
	return maxBufferSize;
    }

    public byte[] getRefused() {
	return refusedData;
    }

    public void setLogFilePath(String path) {
	pathToLog = path;
    }

    public void setBlockedFilePath(String path) {
	pathToBlocked = path;
    }

    public void setFiltersFilePath(String path) {
	pathToFilters = path;
    }

    public void setProxyPort(int p) {
	proxyPort = p;
    }

    public void setMaxBuffer(int b) {
	maxBufferSize = b;
    }

    public void setLoggingEnabled(boolean v) {
	loggingEnabled = v;
	ProxyDataLogger.getInstance().setEnabled(v);
    }

    public void setFilteringEnabled(boolean v) {
	filterEnabled = v;
    }
}
