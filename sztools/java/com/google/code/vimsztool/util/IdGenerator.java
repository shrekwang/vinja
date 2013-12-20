package com.google.code.vimsztool.util;

import java.util.concurrent.atomic.AtomicLong;

public class IdGenerator {
	
	private static AtomicLong id = new AtomicLong(1);
	
	public static String  getUniqueId() {
		long v = id.getAndIncrement();
		return String.valueOf(v);
	}

}
