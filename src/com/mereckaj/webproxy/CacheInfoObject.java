package com.mereckaj.webproxy;

public class CacheInfoObject {
    	public String wholeheader;
	private boolean noCache;
	private boolean isPrivate;
	private boolean isPublic;
	private boolean noModify;
	private int maxAge;
	private String date;
	private byte[] data;
	private boolean mustRevalidate;

	public CacheInfoObject() {
		setNoCache(false);
		setPrivate(false);
		setPublic(false);
		setNoModify(false);
		setMustRevalidate(false);
		setMaxAge(-1);
		setDate("");
		setData(null);
	}

	public boolean hasExpired(String date) {
		return false;
	}

	public boolean isCacheable() {
		return (!noCache);
	}

	public void setNoCache(boolean noCache) {
		this.noCache = noCache;
	}

	public boolean isPrivate() {
		return isPrivate;
	}

	public void setPrivate(boolean isPrivate) {
		this.isPrivate = isPrivate;
	}

	public boolean isPublic() {
		return isPublic;
	}

	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}

	public boolean isNoModify() {
		return noModify;
	}

	public void setNoModify(boolean noModify) {
		this.noModify = noModify;
	}

	public int getMaxAge() {
		return maxAge;
	}

	public void setMaxAge(int maxAge) {
		this.maxAge = maxAge;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public byte[] getData() {
		return data;
	}
	
	public void put(byte[] data){
	    if(this.data==null){
		setData(data);
	    }else{
		appendData(data);
	    }
	}
	public void setData(byte[] data) {
		this.data = data;
	}
	public void appendData(byte[] data){
		if(data!=null){
			byte[] newData = new byte[this.data.length + data.length];
			System.arraycopy(this.data, 0, newData, 0, this.data.length);
			System.arraycopy(data, 0, newData, this.data.length, data.length);
			this.data = newData;
		}
	}

	public boolean isMustRevalidate() {
	    return mustRevalidate;
	}

	public void setMustRevalidate(boolean mustRevalidate) {
	    this.mustRevalidate = mustRevalidate;
	}
}
