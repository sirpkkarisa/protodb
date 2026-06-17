package org.pkteq.protodb.core;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Store {
    private static final Map<String, Obj> DATA = new ConcurrentHashMap<>();

    public record Obj(Object value, long expiresAt) {
        public boolean isExpired() {
            return expiresAt != -1 && expiresAt <= System.currentTimeMillis();
        }
    }

    public static void set(String key, Object value, long expireDurationMS) {
        long expiresAt = -1;
        if (expireDurationMS != -1) {
            expiresAt = System.currentTimeMillis() + expireDurationMS;
        }
        DATA.put(key, new Obj(value, expiresAt));
    }

    /**
     * Controlled GET operation with Lazy Expiration.
     * This ensures expired keys are removed even if the background task hasn't hit them yet.
     */
    public static Obj get(String key) {
        Obj obj = DATA.get(key);
        if (obj != null && obj.isExpired()) {
            DATA.remove(key);
            return null;
        }
        return obj;
    }

    public static boolean delete(String key) {
        return DATA.remove(key) != null;
    }

    public static boolean exists(String key) {
        return get(key) != null; // Use get() to trigger lazy expiration
    }

    public static int size() {
        return DATA.size();
    }

    public static boolean isEmpty() {
        return DATA.isEmpty();
    }

    /**
     * Provides an iterator for background expiration sampling.
     */
    public static Iterator<Map.Entry<String, Obj>> entryIterator() {
        return DATA.entrySet().iterator();
    }

    /**
     * Manual update of an object (e.g. for EXPIRE command)
     */
    public static void putRaw(String key, Obj obj) {
        DATA.put(key, obj);
    }
}
