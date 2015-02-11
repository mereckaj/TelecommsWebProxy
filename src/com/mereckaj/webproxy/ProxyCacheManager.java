package com.mereckaj.webproxy;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ProxyCacheManager {
    private static final boolean READ_WRITE_LOCK_IS_FAIR = true;
    private static final long MAXIMUM_LOCK_WAIT_TIME = 50;
    private static final TimeUnit LOCK_WAIT_TIME_UNIT = TimeUnit.MILLISECONDS;

    private static ProxyCacheManager instance = new ProxyCacheManager();

    private static Hashtable<String, CacheInfoObject> cache = new Hashtable<String, CacheInfoObject>();
    private static ReentrantReadWriteLock cacheLock;

    private ProxyCacheManager() {
	if (cacheLock == null) {
	    cacheLock = new ReentrantReadWriteLock(READ_WRITE_LOCK_IS_FAIR);
	}
    }

    public static ProxyCacheManager getInstance() {
	return instance;
    }

    public boolean isCached(String url) {
	boolean result = false;
	try {
	    if (cacheLock.readLock().tryLock(MAXIMUM_LOCK_WAIT_TIME, LOCK_WAIT_TIME_UNIT)) {
		if (cache.containsKey(url)) {
		    result = true;
		} else {
		    result = false;
		}
		cacheLock.readLock().unlock();
		if (result == true) {
		    CacheInfoObject r = cache.get(url);
		    if (r.getMaxAge() == -1) {
			r = null;
		    } else {
			Date d = parseDate(r.getDate());
			Calendar c = Calendar.getInstance();
			c.setTime(d);
			c.add(Calendar.SECOND, r.getMaxAge());
			if (c.after(d)) {
			    result = false;
			}
		    }
		}
	    }
	} catch (InterruptedException e) {
	    ProxyLogger.getInstance()
		    .log(ProxyLogLevel.CACHE_ERROR, "Exception: " + e.getMessage());
	}
	return result;
    }

    public CacheInfoObject getData(String url) {
	CacheInfoObject result = null;
	try {
	    if (cacheLock.readLock().tryLock(MAXIMUM_LOCK_WAIT_TIME, LOCK_WAIT_TIME_UNIT)) {
		result = cache.get(url);
		cacheLock.readLock().unlock();
	    }
	} catch (InterruptedException e) {
	    ProxyLogger.getInstance()
		    .log(ProxyLogLevel.CACHE_ERROR, "Exception: " + e.getMessage());
	}
	return result;
    }

    public boolean cacheIn(String url, CacheInfoObject data) {
	boolean result = false;
	try {
	    if (cacheLock.writeLock().tryLock(MAXIMUM_LOCK_WAIT_TIME, LOCK_WAIT_TIME_UNIT)) {
		cache.put(url, data);
		result = true;
		cacheLock.writeLock().unlock();
	    }
	} catch (InterruptedException e) {
	    ProxyLogger.getInstance()
		    .log(ProxyLogLevel.CACHE_ERROR, "Exception: " + e.getMessage());
	}
	return result;
    }

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
