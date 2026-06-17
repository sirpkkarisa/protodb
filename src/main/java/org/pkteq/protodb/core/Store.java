package org.pkteq.protodb.core;

public class Store {
    public record Obj(Object value, long expiresAt) {}

    public static Obj setExpiration(Object value, long expireDurationMS) {
        long expiresAt = -1;
        if (expireDurationMS != -1) {
            expiresAt = System.currentTimeMillis() + expireDurationMS;
        }
        return new Obj(value, expiresAt);
    }
}
