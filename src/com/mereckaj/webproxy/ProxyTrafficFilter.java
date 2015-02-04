package com.mereckaj.webproxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.mereckaj.webproxy.utils.Utils;

public class ProxyTrafficFilter {
	private String pathToBlocked;
	private String pathToFiltered;

	private BufferedReader blockedIpFile;
	private BufferedReader filteredFile;

	private List<String> blockedIpList;
	private List<String> blockedHostList;
	private List<String> blockedPhraseList;

	private static ProxyTrafficFilter instance = new ProxyTrafficFilter(
			ProxySettings.getInstance().getPathToBlocked(), ProxySettings
					.getInstance().getPathToFilter());

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

	private void parseFiles() {
		parseForBlockedIpAndHost();
		parseForBlockedPhrase();
	}

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

	private void parseForBlockedIpAndHost() {
		String s = "";
		try {
			while ((s = blockedIpFile.readLine()) != null) {
				if (!s.startsWith("#") && !s.isEmpty()) {
					if (s.equals("[HOST]")) {
						while (!(s = blockedIpFile.readLine()).equals("[IP]")
								&& s != null && !s.startsWith("#")) {
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

	public static ProxyTrafficFilter getInstance() {
		return instance;
	}

	public boolean isBlockedIP(String ip) {
		for (int i = 0; i < blockedIpList.size(); i++) {
			if (blockedIpList.get(i).equalsIgnoreCase(ip)) {
				return true;
			}
		}
		return false;
	}

	public boolean isBlockedHost(String host) {
		for (int i = 0; i < blockedHostList.size(); i++) {
			if (blockedHostList.get(i).equalsIgnoreCase(host)) {
				return true;
			}
		}
		return false;
	}

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
		ProxyLogger.getInstance().log(ProxyLogLevel.INFO,
				"Blocked host:" + host);
	}

	public void addBlockedIP(String ip) {
		blockedIpList.add(ip);
		ProxyLogger.getInstance().log(ProxyLogLevel.INFO, "Blocked IP:" + ip);
	}

	public void addBlockedPhrase(String phrase) {
		blockedPhraseList.add(phrase);
		ProxyLogger.getInstance().log(ProxyLogLevel.INFO,
				"Blocked phrase:" + phrase);
	}

	public boolean removeBlockedHost(String host) {
		for (int i = 0; i < blockedHostList.size(); i++) {
			if (blockedHostList.get(i).equals(host)) {
				blockedHostList.remove(i);
				ProxyLogger.getInstance().log(ProxyLogLevel.INFO,
						"Unblocked host:" + host);
				return true;
			}
		}
		return false;
	}

	public boolean removeBlockedIP(String ip) {
		for (int i = 0; i < blockedIpList.size(); i++) {
			if (blockedIpList.get(i).equals(ip)) {
				blockedIpList.remove(i);
				ProxyLogger.getInstance().log(ProxyLogLevel.INFO,
						"Unblocked ip:" + ip);
				return true;
			}
		}
		return false;
	}

	public boolean removeBlockedPhrase(String phrase) {
		for (int i = 0; i < blockedPhraseList.size(); i++) {
			if (blockedPhraseList.get(i).equals(phrase)) {
				blockedPhraseList.remove(i);
				ProxyLogger.getInstance().log(ProxyLogLevel.INFO,
						"Unblocked phrase:" + phrase);
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

	public void writeFiles() {
		// TODO: Method called when proxy is about to shut down, this method
		// should write IP/HOST/PHRASE to files
	}
}
