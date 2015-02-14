package com.mereckaj.webproxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.mereckaj.webproxy.utils.Utils;

/**
 * This class deals with Traffic Filtering.
 * This class holds information on the blocked ip address'
 * blocked host names and blocked phrase names
 * 
 * This is a singleton object. This means the constructor is private.
 * To obtain an instance of this class use getInstance() method
 * @author julius
 *
 */
public class ProxyTrafficFilter {
    private String pathToBlocked;
    private String pathToFiltered;

    private BufferedReader blockedIpFile;
    private BufferedReader filteredFile;

    private List<String> blockedIpList;
    private List<String> blockedHostList;
    private List<String> blockedPhraseList;
    
    private ProxyLogger logger =  ProxyLogger.getInstance();

    private static ProxyTrafficFilter instance = new ProxyTrafficFilter(ProxySettings.getInstance()
	    .getPathToBlocked(), ProxySettings.getInstance().getPathToFilter());
    
    /**
     * Create an isntance of this class
     * @param blocked path to a file containing blocked host names and ip address'
     * @param filtered path to a file containing filtered phrases
     */
    private ProxyTrafficFilter(String blocked, String filtered) {
	pathToBlocked = blocked;
	pathToFiltered = filtered;
	if (blockedIpFile == null) {
	    blockedIpFile = Utils.openFile(pathToBlocked);
	}
	if (filteredFile == null) {
	    filteredFile = Utils.openFile(pathToFiltered);
	}
	if (blockedHostList == null) {
	    blockedHostList = new ArrayList<String>();
	}
	if (blockedIpList == null) {
	    blockedIpList = new ArrayList<String>();
	}
	if (blockedPhraseList == null) {
	    blockedPhraseList = new ArrayList<String>();
	}
	parseFiles();
    }
    
    /*
     * Called when parsing of the config file is needed 
     */
    private void parseFiles() {
	parseForBlockedIpAndHost();
	parseForBlockedPhrase();
    }
    
    /*
     * Parse the filtered file for phrases that need to be added to the 
     * blocked phrase list
     */
    private void parseForBlockedPhrase() {
	String s = "";
	try {
	    while ((s = filteredFile.readLine()) != null) {
		if (!s.startsWith("#") && !s.isEmpty()) {
		    s.trim();
		    blockedPhraseList.add(s);
		}
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }
    
    /*
     * Parse the blocked file for hostnames and ip address' that need
     * to be blocked
     */
    private void parseForBlockedIpAndHost() {
	String s = "";
	try {
	    while ((s = blockedIpFile.readLine()) != null) {
		if (!s.startsWith("#") && !s.isEmpty()) {
		    if (s.equals("[HOST]")) {
			while (!(s = blockedIpFile.readLine()).equals("[IP]") && s != null
				&& !s.startsWith("#")) {
			    if (!s.isEmpty()) {
				s.trim();
				blockedHostList.add(s);
			    }
			}
			while ((s = blockedIpFile.readLine()) != null) {
			    if (!s.startsWith("#") && !s.equals("[IP]")) {
				s.trim();
				blockedIpList.add(s);
			    }
			}
		    }
		}
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }
    
    /**
     * This method returns an instance of this object
     * @return instance of this object
     */
    public static ProxyTrafficFilter getInstance() {
	return instance;
    }
    
    /**
     * Given an ip address this method checks if it is in the blocked list
     * @param ip to check
     * @return <b>true</b> if the ip is blocked<br>
     * <b>false</b> otherwise
     */
    public boolean isBlockedIP(String ip) {
	for (int i = 0; i < blockedIpList.size(); i++) {
	    if (blockedIpList.get(i).equalsIgnoreCase(ip)) {
		return true;
	    }
	}
	return false;
    }
    
    /**
     * Given a hostname this method checks if it is in the blocked list
     * @param hostname to check
     * @return <b>true</b> if the host is blocked<br>
     * <b>false</b> otherwise
     */
    public boolean isBlockedHost(String host) {
	for (int i = 0; i < blockedHostList.size(); i++) {
	    if (blockedHostList.get(i).equalsIgnoreCase(host)) {
		return true;
	    }
	}
	return false;
    }
    
    /**
     * Give a soem data, this method will check if that data contains 
     * a blocked phrase or word.
     * 
     * This method is very primitive and will not exclude links/headers.
     * <b>Be careful</b>
     * @param data to check
     * @return <b>true</b> if the data contains blocked phrases <br>
     * <b>false</b> otherwise
     */
    public boolean containsBlockedKeyword(byte[] data) {
	String s = new String(data);
	for (int i = 0; i < blockedPhraseList.size(); i++) {
	    if (s.contains(blockedPhraseList.get(i))) {
		return true;
	    }
	}
	return false;
    }

    public void addBlockedHost(String host) {
	blockedHostList.add(host);
	logger.log(ProxyLogLevel.INFO, "Blocked host:" + host);
    }

    public void addBlockedIP(String ip) {
	blockedIpList.add(ip);
	logger.log(ProxyLogLevel.INFO, "Blocked IP:" + ip);
    }

    public void addBlockedPhrase(String phrase) {
	blockedPhraseList.add(phrase);
	logger.log(ProxyLogLevel.INFO, "Blocked phrase:" + phrase);
    }

    public boolean removeBlockedHost(String host) {
	for (int i = 0; i < blockedHostList.size(); i++) {
	    if (blockedHostList.get(i).equals(host)) {
		blockedHostList.remove(i);
		logger.log(ProxyLogLevel.INFO, "Unblocked host:" + host);
		return true;
	    }
	}
	return false;
    }

    public boolean removeBlockedIP(String ip) {
	for (int i = 0; i < blockedIpList.size(); i++) {
	    if (blockedIpList.get(i).equals(ip)) {
		blockedIpList.remove(i);
		logger.log(ProxyLogLevel.INFO, "Unblocked ip:" + ip);
		return true;
	    }
	}
	return false;
    }

    public boolean removeBlockedPhrase(String phrase) {
	for (int i = 0; i < blockedPhraseList.size(); i++) {
	    if (blockedPhraseList.get(i).equals(phrase)) {
		blockedPhraseList.remove(i);
		logger.log(ProxyLogLevel.INFO, "Unblocked phrase:" + phrase);
		return true;
	    }
	}
	return false;
    }

    public List<String> getBlockedIpList() {
	return blockedIpList;
    }

    public List<String> getBlockedHostList() {
	return blockedHostList;
    }

    public List<String> getBlockedPhraseList() {
	return blockedPhraseList;
    }
}
