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

/*
 * Utility class that holds methods that were not suited for 
 * some particular class, or that were needed by many classes
 */
public class Utils {

	/*
	 * Returns <b>null</b> if the file does not exist Returns BufferedReader
	 * from the file otherwise
	 */
	public static BufferedReader openFile(String path) {
		/*
		 * Created the needed objects to open a file
		 */
		File f;
		BufferedReader bufReader = null;
		FileReader reader;

		try {
			/*
			 * Get the file handle at some path then read it into a buffered
			 * reader
			 */
			f = getFile(path);
			reader = new FileReader(f);
			bufReader = new BufferedReader(reader);
		} catch (IOException e) {
			/*
			 * Log the error, will return null;
			 */
			ProxyLogger.getInstance().log(Level.WARNING,
					"Could not open file: " + path);
		}
		return bufReader;
	}

	/*
	 * Returns BufferedReader to the file if it exists, Creates a new file if it
	 * didn't already exist
	 */
	public static BufferedReader openOrCreateFile(String path) {

		/*
		 * Create the needed handles for the file
		 */
		BufferedReader br = openFile(path);
		File f = getFile(path);

		/*
		 * If openFile failed to open the file, maybe it doesn't exists
		 * 
		 * This method will attempt to create it.
		 */
		if (br == null) {
			try {

				/*
				 * Create the new file
				 */
				f.createNewFile();

				/*
				 * Get this file as a buffered reader
				 */
				br = new BufferedReader(new FileReader(f));
			} catch (IOException e) {
				
				/*
				 * Could not open or create the file, something has gone wrong.
				 * 
				 * Program will most likely crash since it isn't designed to handle this problems
				 */
				ProxyLogger.getInstance().log(Level.SEVERE,
						"Couldn't open or create file: " + path);
			}
		}
		return br;
	}

	/*
	 * Returns File from the path provided
	 */
	private static File getFile(String path) {
		return new File(path);
	}

	/*
	 * Mainly used to create a new log file with a unique name Returns the
	 * current date and time in the format of: yyyy_MM_dd_HH_mm_ss
	 */
	public static String getCurrentDate() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
		Calendar cal = Calendar.getInstance();
		return dateFormat.format(cal.getTime());
	}
	public static String getUrl(String s){
		int i = s.indexOf(' ');
		int n = s.indexOf('\n');
		return s.substring(i, n).trim();
	}
}
