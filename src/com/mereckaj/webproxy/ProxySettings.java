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
 * Singleton class that holds settings and information about the proxy
 */
public class ProxySettings {
	/**
	 * Path to proxy configuration file
	 */
	private final String CONFIG_FILE_PATH = ".proxy_config";

	private int proxyPort;
	// Default max buffer size is 64k bytes
	private int maxBufferSize = 65536;
	private boolean loggingEnabled;
	private boolean filterEnabled;

	private boolean running = true;

	// Path to the log the proxy will use when working, not the one ProxyLogger
	// uses
	private String pathToLog;
	private String pathToFilters;
	private String pathToBlocked;

	// Static instance on this class, use getInstance() to get to it
	private static ProxySettings instance = new ProxySettings();

	/*
	 * Data that will be displayed if the host/ip is blocked by the proxy.
	 */
	private byte[] refusedData;

	private BufferedReader configFile;

	/***
	 * Constructor for this class, private because you should only access this
	 * class by using ProxySettings.getInstance() method
	 * 
	 * Also checks if the config file has already been read in. If not, then
	 * read it in or create, if it doesn't exist
	 */
	private ProxySettings() {
		if (configFile == null) {
			configFile = Utils.openOrCreateFile(CONFIG_FILE_PATH);
		}
		if (refusedData == null) {
			Path path = Paths.get("proxy_refused_connection.html");
			try {
				refusedData = Files.readAllBytes(path);
			} catch (IOException e) {
				System.out.println("Cant read refused file");
			}
		}
	}

	/**
	 * Returns the instance of this class
	 */
	public static ProxySettings getInstance() {
		return instance;
	}

	private void parseConfigFile() {
		// Check if the file is empty
		if (configFile != null) {
			String currentLine;
			try {
				while ((currentLine = configFile.readLine()) != null) {
					/**
					 * Read log line by line, ignoring lines starting with a new
					 * line char '\n', or with a comment char '#'
					 * 
					 * Also, log the settings used and any unexpected findings
					 */
					if (!currentLine.startsWith("#") && !currentLine.isEmpty()) {
						if (currentLine.contains("PROXY_PORT")) {
							proxyPort = Integer
									.parseInt(getAfterEquals(currentLine));
							ProxyLogger.getInstance().log(Level.INFO,
									"Proxy port: " + proxyPort);
						} else if (currentLine.contains("LOGGING_ENABLED")) {
							if (Integer.parseInt(getAfterEquals(currentLine)) == 0) {
								loggingEnabled = false;
							} else if (Integer
									.parseInt(getAfterEquals(currentLine)) == 1) {
								loggingEnabled = true;
							} else {
								loggingEnabled = false;
								ProxyLogger
										.getInstance()
										.log(Level.WARNING,
												"Config file contained invalid value for LOGGING_ENABLED");
							}
							ProxyLogger.getInstance().log(Level.INFO,
									"Logging enabled: " + loggingEnabled);
						} else if (currentLine.contains("FILTER_CONTENT")) {
							if (Integer.parseInt(getAfterEquals(currentLine)) == 0) {
								filterEnabled = false;
							} else if (Integer
									.parseInt(getAfterEquals(currentLine)) == 1) {
								filterEnabled = true;
							} else {
								filterEnabled = false;
								ProxyLogger
										.getInstance()
										.log(Level.WARNING,
												"Config file contained invalid value for FILTER_CONTENT");
							}
							ProxyLogger.getInstance().log(Level.INFO,
									"Filtering enabled: " + filterEnabled);
						} else if (currentLine.contains("PATH_LOG")) {
							String path = getAfterEquals(currentLine);
							pathToLog = path;
							ProxyLogger.getInstance().log(Level.INFO,
									"Path to log: " + pathToLog);
						} else if (currentLine.contains("PATH_FILTER")) {
							String path = getAfterEquals(currentLine);
							pathToFilters = path;
							ProxyLogger.getInstance().log(Level.INFO,
									"Path to filters: " + pathToFilters);
						} else if (currentLine.contains("PATH_BLOCKED")) {
							String path = getAfterEquals(currentLine);
							pathToBlocked = path;
							ProxyLogger.getInstance().log(Level.INFO,
									"Path to blocked ip: " + pathToBlocked);
						} else if (currentLine.contains("MAX_BUFFER_SIZE")) {
							maxBufferSize = Integer
									.parseInt(getAfterEquals(currentLine));
							ProxyLogger.getInstance().log(Level.INFO,
									"Max buffer size: " + maxBufferSize);
						} else {
							ProxyLogger.getInstance().log(
									Level.WARNING,
									"Found something weird in the log file: "
											+ currentLine);
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			ProxyLogger.getInstance().log(Level.SEVERE,
					"Config file was not found");
		}
	}

	/**
	 * Used for parsing the config file, returns everything after the equals
	 * char
	 * 
	 * e.g given "PROXY_PORT = 4000" will return "4000"
	 */
	private static String getAfterEquals(String s) {
		return s.substring(s.indexOf('=') + 1).trim();
	}

	/**
	 * Check if proxy port has been set, if it has not try read the config file,
	 * otherwise return the proxy port
	 * 
	 * This check should never happen realistically.
	 * 
	 */
	public int getProxyPort() {
		if (proxyPort == 0) {
			parseConfigFile();
		}
		return proxyPort;
	}

	/**
	 * Return: <b>true</b> if logging is enabled, <b>false</b> otherwise.
	 * 
	 * Default: <b> false </b>
	 */
	public boolean isLoggingEnabled() {
		return loggingEnabled;
	}

	/**
	 * Return: <b>true</b> if filtering of traffic is enabled, <b>false</b>
	 * otherwise.
	 * 
	 * Default: <b> false </b>
	 */
	public boolean isFilteringEnabled() {
		return filterEnabled;
	}

	/**
	 * Return: path to log, if it doesn't exists, parse the config file to find
	 * it.
	 */
	public String getPathToLog() {
		if (pathToLog == null) {
			parseConfigFile();
		}
		return pathToLog;
	}

	/**
	 * Return: path to filters file, used to filder traffic for keywords, if it
	 * doesn't exists, parse the config file to find it.
	 */
	public String getPathToFilter() {
		if (pathToFilters == null) {
			parseConfigFile();
		}
		return pathToFilters;
	}

	/**
	 * Return: path to blocked IP file, if it doesn't exists, parse the config
	 * file to find it.
	 */
	public String getPathToBlocked() {
		if (pathToBlocked == null) {
			parseConfigFile();
		}
		return pathToBlocked;
	}

	/**
	 * returns true if proxy should still be running returns false if the proxy
	 * should stop
	 */
	public boolean getRunning() {
		return running;
	}

	/**
	 * Set whether proxy should run or stop
	 * 
	 * @param v
	 */
	public void setRunning(boolean v) {
		if (v == false) {
			onShutdown();
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
	}

	public void setFilteringEnabled(boolean v) {
		filterEnabled = v;
	}

	/*
	 * Writes a new config file. Good luck and have fun reading the code below.
	 */
	public void onShutdown() {
		writeConfigToFile();
		writeBlockedHostIpToFile();
		writeFilteredPhrasesToFile();
	}

	private void writeFilteredPhrasesToFile() {
		File f = new File(pathToFilters);
		if (!f.exists()) {
			ProxyLogger.getInstance().log(ProxyLogLevel.WARNING,
					"No filter file found");
		} else {
			try {
				FileWriter fw = new FileWriter(f.getAbsoluteFile());
				BufferedWriter br = new BufferedWriter(fw);
				
				br.write("# This file contains blocked phrases\n"
						+"# Each new line is threaded as a phrase which the program will try to match\n"
						+"# Lines starting with # or \\n are ignored\n"
						+"# Proxy ignores case\n"
						+ "# This will even block HTML/CSS/JS words. Bug/Feature ? you decide.\n");
				List<String> plist = ProxyTrafficFilter.getInstance().getBlockedPhraseList();
				for(int i = 0; i < plist.size(); i++){
					br.write(plist.get(i)+"\n");
				}
				br.close();
			} catch (IOException e) {
				ProxyLogger.getInstance().log(ProxyLogLevel.EXCEPTION,
						"Could not write to filters file: " + e.getMessage());
			}
		}

	}

	private void writeBlockedHostIpToFile() {
		File f = new File(pathToBlocked);
		if (!f.exists()) {
			ProxyLogger.getInstance().log(ProxyLogLevel.WARNING,
					"No blocked ip/host file found");
		} else {
			try {
				FileWriter fw = new FileWriter(f.getAbsoluteFile());
				BufferedWriter br = new BufferedWriter(fw);
				
				br.write("# This file contains lists of blocked hosts\n"
						+"# and blocked IP's.\n"
						+"# Hosts and IP's must be in the correct block,\n"
						+"# denoted by [HOST] or [IP].\n"
						+"# Removal of [HOST] or [IP] might cause undefined behaviour\n"
						+"# by the program.\n\n");
				br.write("# Below is a list of blocked host names.\n"
						+"# Each new blocked host must be entered on a new line\n"
						+"[HOST]\n");
				List<String> hlist = ProxyTrafficFilter.getInstance().getBlockedHostList();
				for(int i = 0; i < hlist.size();i++){
					br.write(hlist.get(i)+"\n");
				}
				br.write("\n");
				br.write("# Below is a list of blocked IP's\n"
						+"# Each new blocked ip must be entered on a new line\n"
						+"[IP]\n");
				List<String> iplist = ProxyTrafficFilter.getInstance().getBlockedIpList();
				for(int i = 0; i < iplist.size();i++){
					br.write(iplist.get(i)+"\n");
				}
				br.write("\n");				
				br.close();
			} catch (IOException e) {
				ProxyLogger.getInstance().log(ProxyLogLevel.EXCEPTION,
						"Could not write to filters file: " + e.getMessage());
			}
		}
	}

	private void writeConfigToFile() {
		File f = new File(CONFIG_FILE_PATH);
		if (!f.exists()) {
			ProxyLogger.getInstance().log(ProxyLogLevel.SEVERE,
					"Log file not found, How did you start the program ?");
		} else {
			try {
				FileWriter fw = new FileWriter(f.getAbsoluteFile());
				BufferedWriter br = new BufferedWriter(fw);
				br.write("# This file contains the settings that the proxy will load on start up\n");
				br.write("# Author Julius Mereckas\n\n");
				br.write("#Set the port to which the proxy will bind\n");
				br.write("PROXY_PORT = " + proxyPort + "\n\n");
				br.write("#If logging is disabled no writes to log will made \n"
						+ "#Management engine will not do anything\n"
						+ "# 0 = disabled\n" + "# 1 = enabled\n");
				br.write("LOGGING_ENABLED = "
						+ (loggingEnabled == true ? "1" : "0") + "\n\n");
				br.write("#If enabled the content will be filtered for any phrases"
						+ "in the .proxy_filter file\n"
						+ "#and for any ip's in .proxy_blocked\n"
						+ "# 0 = disabled\n"
						+ "# 1 = enabled\n"
						+ "# default = disabled\n");
				br.write("FILTER_CONTENT = "
						+ (filterEnabled == true ? "1" : "0") + "\n\n");
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
				ProxyLogger.getInstance().log(ProxyLogLevel.EXCEPTION,
						"Unable to write config file " + e.getMessage());
			}
		}

	}
}
