package org.pkteq.protodb.core;

import java.util.List;

public class Store {

    record Obj (Object value, long expiresAt) {}

    static Obj setExpiration(Object items, long durationMS) {
        long expiresAt = -1;

        if (durationMS > 0) expiresAt = System.currentTimeMillis()+durationMS;
        return new Obj(items, expiresAt);
    }
}
