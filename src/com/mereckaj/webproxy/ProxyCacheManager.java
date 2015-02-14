package com.mereckaj.webproxy;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This is a singleton class of a Cache Manager used by the proxy <br>
 * This class does not have a public constructor. To get an instance of this
 * class use getInstance() method. <br>
 * The cache is implemented as a {@link Hashtable} where Key is a URL (In the
 * form of a String) and Value is a {@link CacheInfoObject} <br>
 * 
 * The cache never removes items from itself. This is done to increase the cache
 * speed. If the object has expired then the cache will say that the page is not
 * cache, in which case the Worker thread will cache that page again once more.
 * @author julius
 * 
 */
public class ProxyCacheManager {
    /*
     * Some final global variables used by the locking mechanism.
     */
    private static final boolean READ_WRITE_LOCK_IS_FAIR = true;
    private static final long MAXIMUM_LOCK_WAIT_TIME = 50;
    private static final TimeUnit LOCK_WAIT_TIME_UNIT = TimeUnit.MILLISECONDS;

    // This is the ONLY instance of this class in the whole program.
    private static ProxyCacheManager instance = new ProxyCacheManager();

    private static Hashtable<String, CacheInfoObject> cache = new Hashtable<String, CacheInfoObject>();
    private static ReentrantReadWriteLock cacheLock;
    private ProxyLogger logger;

    private ProxyCacheManager() {
	// Check to see if a lock already exists, create it if not
	if (cacheLock == null) {
	    cacheLock = new ReentrantReadWriteLock(READ_WRITE_LOCK_IS_FAIR);
	}

	// Check if the logger instance exists, get it if it hasn't.
	if (logger == null) {
	    logger = ProxyLogger.getInstance();
	}
    }

    /**
     * This function returns the instance of this class.
     */
    public static ProxyCacheManager getInstance() {
	return instance;
    }

    /**
     * This function checks if a URL is cached.
     * 
     * @param url
     *            Key used to index the Hashtable
     * @return <b>true</b> if the url is cached and it has not yet expired.<br>
     *         <b>false</b> if the url is not cache or is cache but has expired
     */
    public synchronized boolean isCached(String url) {
	boolean result = false;
	try {
	    /*
	     * Attempt to obtain a lock to the cache. This lock will only
	     * attempt for a limited time. This is due to the fact that we don't
	     * want the thread that requested this check to hang if there ever
	     * is a situation of deadlock
	     */
	    try {
		/*
		 * If the result is cached, get a copy of it and release the
		 * lock so that other threads may access the cache while
		 * checking if the data is valid
		 */
		if (cacheLock.readLock().tryLock(MAXIMUM_LOCK_WAIT_TIME, LOCK_WAIT_TIME_UNIT)) {
		    if (cache.containsKey(url)) {
			result = true;
		    } else {
			result = false;
		    }
		    if (result == true) {
			CacheInfoObject r = cache.get(url);
			cacheLock.readLock().unlock();
			/*
			 * Check if max age was set by the HTTPResponseParser If
			 * it has not then this item shouldn't even be in the
			 * cache
			 */
			if (r.getMaxAge() == -1) {
			    r = null;
			    result = false;
			} else {
			    /*
			     * In the case this item is cached, get it's time
			     * and check if that time + maxAge has passed. In
			     * which case the cached object has expired.
			     */
			    // TODO:Change CacheInfoObject's date to Date rather
			    // than String
			    Date d = parseDate(r.getDate());
			    Calendar c = Calendar.getInstance();
			    c.setTime(d);
			    c.add(Calendar.SECOND, r.getMaxAge());
			    if (c.after(d)) {
				result = false;
			    }
			}
		    } else {
			cacheLock.readLock().unlock();
		    }
		}
	    } finally {
		// Just a precaution to make sure that we always release the
		// lock
		cacheLock.readLock();
	    }
	} catch (InterruptedException e) {
	    /*
	     * If there is an InteruptException, log it and return false as
	     * there may have been changes inside the cache during the interupt
	     */
	    result = false;
	    logger.log(ProxyLogLevel.CACHE_ERROR, "Exception: " + e.getMessage());
	}
	return result;
    }

    /**
     * This method returns the data that was stored inside the cached object.
     * 
     * It checks if the data is still inside the cache just a precaution.
     * 
     * @param url to check if it is cached
     * @return <b>null</b> if the method was interupted or the object is no longer in the cache
     * or has expired
     */
    public synchronized CacheInfoObject getData(String url) {
	CacheInfoObject result = null;
	if (isCached(url)) {
	    try {
		if (cacheLock.readLock().tryLock(MAXIMUM_LOCK_WAIT_TIME, LOCK_WAIT_TIME_UNIT)) {
		    result = cache.get(url);
		    cacheLock.readLock().unlock();
		}
	    } catch (InterruptedException e) {
		logger.log(ProxyLogLevel.CACHE_ERROR, "Exception: " + e.getMessage());
	    }
	}
	return result;
    }
    
    /**
     * This method is used to add items to the cache.
     * @param url
     * @param data
     * @return
     */
    public synchronized boolean cacheIn(String url, CacheInfoObject data) {
	boolean result = false;
	try {
	    if (cacheLock.writeLock().tryLock(MAXIMUM_LOCK_WAIT_TIME, LOCK_WAIT_TIME_UNIT)) {
		cache.put(url, data);
		result = true;
		cacheLock.writeLock().unlock();
	    }
	} catch (InterruptedException e) {
	    try{
		cacheLock.writeLock().unlock();
	    } catch (IllegalMonitorStateException f){
		/*
		 * Attempted to unlock when didn't have a lock. No big problem here.
		 * Just a precaution move.
		 */
	    }
	    logger.log(ProxyLogLevel.CACHE_ERROR, "Exception: " + e.getMessage());
	}
	return result;
    }
    
    /**
     * This method parses the HTTP format date into a {@link Date} object.
     * This is used to enable comparison of dates by the cahce.
     * @param date the string format date to be parsed
     * @return <b>null</b> if a {@link ParseException} is caught<br>
     * {@link Date} if the parse is successful.
     */
    public Date parseDate(String date) {
	Date d = null;
	SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
	try {
	    d = format.parse(date);
	} catch (ParseException e) {
	}
	return d;
    }

}
