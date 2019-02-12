package com.simple.wechatsimple.util;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class CacheMap<K, V> {
    private final LinkedHashMap<K, V> map;
    private int size;
    private int maxSize;
    private int putCount;
    private int createCount;
    private int evictionCount;
    private int hitCount;
    private int missCount;

    public CacheMap(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        } else {
            this.maxSize = maxSize;
            this.map = new LinkedHashMap(0, 0.75F, true);
        }
    }

    public void resize(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        } else {
            synchronized (this) {
                this.maxSize = maxSize;
            }

            this.trimToSize(maxSize);
        }
    }

    public final V get(K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        } else {
            V mapValue;
            synchronized (this) {
                mapValue = this.map.get(key);
                if (mapValue != null) {
                    ++this.hitCount;
                    return mapValue;
                }

                ++this.missCount;
            }

            V createdValue = this.create(key);
            if (createdValue == null) {
                return null;
            } else {
                synchronized (this) {
                    ++this.createCount;
                    mapValue = this.map.put(key, createdValue);
                    if (mapValue != null) {
                        this.map.put(key, mapValue);
                    } else {
                        this.size += this.safeSizeOf(key, createdValue);
                    }
                }

                if (mapValue != null) {
                    this.entryRemoved(false, key, createdValue, mapValue);
                    return mapValue;
                } else {
                    this.trimToSize(this.maxSize);
                    return createdValue;
                }
            }
        }
    }

    public final V put(K key, V value) {
        if (key != null && value != null) {
            V previous;
            synchronized (this) {
                ++this.putCount;
                this.size += this.safeSizeOf(key, value);
                previous = this.map.put(key, value);
                if (previous != null) {
                    this.size -= this.safeSizeOf(key, previous);
                }
            }

            if (previous != null) {
                this.entryRemoved(false, key, previous, value);
            }

            this.trimToSize(this.maxSize);
            return previous;
        } else {
            throw new NullPointerException("key == null || value == null");
        }
    }

    private void trimToSize(int maxSize) {
        while (true) {
            K key;
            V value;
            synchronized (this) {
                label51:
                {
                    if (this.size < 0 || this.map.isEmpty() && this.size != 0) {
                        throw new IllegalStateException(this.getClass().getName() + ".sizeOf() is reporting inconsistent results! size = " + this.size);
                    }

                    if (this.size > maxSize) {
                        Entry<K, V> toEvict = null;
                        Iterator var6 = this.map.entrySet().iterator();
                        if (var6.hasNext()) {
                            Entry<K, V> entry = (Entry) var6.next();
                            toEvict = entry;
                        }

                        if (toEvict != null) {
                            key = toEvict.getKey();
                            value = toEvict.getValue();
                            this.map.remove(key);
                            this.size -= this.safeSizeOf(key, value);
                            ++this.evictionCount;
                            break label51;
                        }
                    }

                    return;
                }
            }

            this.entryRemoved(true, key, value, null);
        }
    }

    public final V remove(K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        } else {
            V previous;
            synchronized (this) {
                previous = this.map.remove(key);
                if (previous != null) {
                    this.size -= this.safeSizeOf(key, previous);
                }
            }

            if (previous != null) {
                this.entryRemoved(false, key, previous, null);
            }

            return previous;
        }
    }

    public void clear() {
        synchronized (this) {
            this.map.clear();
            this.size = 0;
        }

        this.putCount = 0;
        this.createCount = 0;
        this.evictionCount = 0;
        this.hitCount = 0;
        this.missCount = 0;
        this.size = 0;
    }

    protected void entryRemoved(boolean evicted, K key, V oldValue, V newValue) {
    }

    protected V create(K key) {
        return null;
    }

    private int safeSizeOf(K key, V value) {
        int result = this.sizeOf(key, value);
        if (result < 0) {
            throw new IllegalStateException("Negative size: " + key + "=" + value);
        } else {
            return result;
        }
    }

    protected int sizeOf(K key, V value) {
        return 1;
    }

    public final void evictAll() {
        this.trimToSize(-1);
    }

    public final synchronized int size() {
        return this.size;
    }

    public final synchronized int maxSize() {
        return this.maxSize;
    }

    public final synchronized int hitCount() {
        return this.hitCount;
    }

    public final synchronized int missCount() {
        return this.missCount;
    }

    public final synchronized int createCount() {
        return this.createCount;
    }

    public final synchronized int putCount() {
        return this.putCount;
    }

    public final synchronized int evictionCount() {
        return this.evictionCount;
    }

    public final synchronized Map<K, V> snapshot() {
        return new LinkedHashMap(this.map);
    }

    public final synchronized String toString() {
        int accesses = this.hitCount + this.missCount;
        int hitPercent = accesses != 0 ? 100 * this.hitCount / accesses : 0;
        return String.format("LruCache[maxSize=%d,hits=%d,misses=%d,hitRate=%d%%]", this.maxSize, this.hitCount, this.missCount, hitPercent);
    }
}

