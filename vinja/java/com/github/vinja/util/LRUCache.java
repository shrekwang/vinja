package com.github.vinja.util;

import java.util.LinkedHashMap;
import java.util.Map;


public class LRUCache<A, B> extends LinkedHashMap<A, B> {
	
	private static final long serialVersionUID = 1030637569396291456L;
	
	private final int maxEntries;

    public LRUCache(final int maxEntries) {
        super(maxEntries + 1, 1.0f, true);
        this.maxEntries = maxEntries;
    }
    protected boolean removeEldestEntry(final Map.Entry<A, B> eldest) {
        return super.size() > maxEntries;
    }
}
