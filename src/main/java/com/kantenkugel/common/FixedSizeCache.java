package com.kantenkugel.common;

import java.util.Map;
import java.util.LinkedHashMap;

public class FixedSizeCache<K, V> extends LinkedHashMap<K,V> {
    private final int maxSize;

    public FixedSizeCache(int size) {
        super(size+2, 1F);
        this.maxSize = size;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
        return size() > maxSize;
    }
}