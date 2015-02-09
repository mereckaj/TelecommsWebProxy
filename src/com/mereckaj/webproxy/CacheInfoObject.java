package com.mereckaj.webproxy;

public class CacheInfoObject {
	private boolean noCache;
	private boolean isPrivate;
	private boolean isPublic;
	private boolean noModify;
	private int maxAge;
	private String date;
	private byte[] data;
	public CacheInfoObject(){
		setNoCache(false);
		setPrivate(false);
		setPublic(false);
		setNoModify(false);
		setMaxAge(-1);
		setDate("");
		setData(null);
	}
	public boolean hasExpired(String date){
		return false;
	}
	public boolean isNoCache() {
		return noCache;
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
	public void setData(byte[] data) {
		this.data = data;
	}
}
