package com.mereckaj.webproxy;

import java.util.Hashtable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ProxyCacheManager {
	private static final boolean READ_WRITE_LOCK_IS_FAIR = true;
	private static final long MAXIMUM_LOCK_WAIT_TIME = 50;
	private static final TimeUnit LOCK_WAIT_TIME_UNIT = TimeUnit.MILLISECONDS;
	private static ProxyCacheManager instance = new ProxyCacheManager();

	private static Hashtable<String, byte[]> cache = new Hashtable<String, byte[]>();
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
			if (cacheLock.readLock().tryLock(MAXIMUM_LOCK_WAIT_TIME,
					LOCK_WAIT_TIME_UNIT)) {
				if (cache.containsKey(url)) {
					result = true;
				} else {
					result = false;
				}
				cacheLock.readLock().unlock();
			}
		} catch (InterruptedException e) {
			ProxyLogger.getInstance().log(ProxyLogLevel.CACHE_ERROR,
					"Exception: " + e.getMessage());
		}
		return result;
	}

	public byte[] getData(String url) {
		byte[] result = null;
		try {
			if (cacheLock.readLock().tryLock(MAXIMUM_LOCK_WAIT_TIME,
					LOCK_WAIT_TIME_UNIT)) {
				result = cache.get(url);
				cacheLock.readLock().unlock();
			}
		} catch (InterruptedException e) {
			ProxyLogger.getInstance().log(ProxyLogLevel.CACHE_ERROR,
					"Exception: " + e.getMessage());
		}
		return result;
	}

	public boolean cacheIn(String url, byte[] data) {
		boolean result = false;
		try {
			if (cacheLock.writeLock().tryLock(MAXIMUM_LOCK_WAIT_TIME,
					LOCK_WAIT_TIME_UNIT)) {
				cache.put(url, data);
				result = true;
				cacheLock.writeLock().unlock();
			}
		} catch (InterruptedException e) {
			ProxyLogger.getInstance().log(ProxyLogLevel.CACHE_ERROR,
					"Exception: " + e.getMessage());
		}
		return result;
	}

}
