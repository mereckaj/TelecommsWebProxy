package com.mereckaj.webproxy;

import java.util.logging.Level;

public class ProxyLogLevel extends Level {
	private static final long serialVersionUID = 3026501222008994684L;
	public static final Level CONNECT = new ProxyLogLevel("CONNECT",
			Level.SEVERE.intValue() + 1);
	public static final Level DISCONNECT = new ProxyLogLevel("DISCONNECT",
			Level.SEVERE.intValue() + 2);
	public static final Level USAGE = new ProxyLogLevel("USAGE",
			Level.SEVERE.intValue() + 3);
	public static final Level EXCEPTION = new ProxyLogLevel("EXCEPTION",
			Level.SEVERE.intValue() + 4);

	public ProxyLogLevel(String name, int value) {
		super(name, value);
	}
}
