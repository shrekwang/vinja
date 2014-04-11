package com.github.vinja.util;

import java.util.HashMap;
import java.util.Map;

public class BufferStore {
	
	private static Map<String,StringBuffer> store = new HashMap<String,StringBuffer>();
	
	public static void put(String uuid, StringBuffer buffer) {
		store.put(uuid, buffer);
	}
	
	public static String getContent(String uuid) {
		StringBuffer buffer = store.get(uuid);
		if (buffer != null) {
			String tmp = buffer.toString();
			tmp = tmp.replace("\r\r\n", "\n");
			tmp = tmp.replace("\r\n", "\n");
			buffer.delete(0, buffer.length());
			return tmp;
		}
		return "";
	}

}
