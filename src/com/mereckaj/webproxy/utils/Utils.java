package com.mereckaj.webproxy.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Level;

import com.mereckaj.webproxy.ProxyLogger;

/**
 * This class is a general utility class that holds all of the methods that were
 * not suited for other classes
 * 
 * This method contains file opening, writing and date getting methods Nothing
 * too exciting
 * 
 * @author julius
 * 
 */
public class Utils {

    public static BufferedReader openFile(String path) {

	File f;
	BufferedReader bufReader = null;
	FileReader reader;

	try {

	    f = getFile(path);
	    reader = new FileReader(f);
	    bufReader = new BufferedReader(reader);
	} catch (IOException e) {

	    ProxyLogger.getInstance().log(Level.WARNING, "Could not open file: " + path);
	}
	return bufReader;
    }

    public static BufferedReader openOrCreateFile(String path) {

	BufferedReader br = openFile(path);
	File f = getFile(path);

	if (br == null) {
	    try {

		f.createNewFile();

		br = new BufferedReader(new FileReader(f));
	    } catch (IOException e) {

		ProxyLogger.getInstance()
			.log(Level.SEVERE, "Couldn't open or create file: " + path);
	    }
	}
	return br;
    }

    private static File getFile(String path) {
	return new File(path);
    }

    public static String getCurrentDate() {
	DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
	Calendar cal = Calendar.getInstance();
	return dateFormat.format(cal.getTime());
    }

    public static String getUrl(String s) {
	int i = s.indexOf(' ');
	int n = s.indexOf('\n');
	return s.substring(i, n).trim();
    }
}
