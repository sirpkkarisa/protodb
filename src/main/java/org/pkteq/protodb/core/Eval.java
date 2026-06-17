package org.pkteq.protodb.core;

import org.pkteq.protodb.AsyncTcpServer;

import java.util.List;

import static org.pkteq.protodb.core.Resp.encodeInteger;
import static org.pkteq.protodb.core.Resp.encodeString;

public class Eval {
    private static final String RESP_NIL = "$-1\r\n";
    private static final String RESP_EMPTY = "$-2\r\n";
    public static String eval(RedisCmd redisCmd) {
        String cmd = redisCmd.cmd().toUpperCase();
        return switch (cmd) {
            case "PING" -> evalPING(redisCmd.args());
            case "SET" -> evalSET(redisCmd.args());
            case "GET" -> evalGET(redisCmd.args());
            case "TTL" -> evalTTL(redisCmd.args());
            case "DEL" -> evalDEL(redisCmd.args());
            case "EXPIRE" -> evalEXPIRE(redisCmd.args());
            case "COMMAND" -> encodeString("OK", true);
            default -> RESP_NIL;
        };
    }

    private static String evalEXPIRE(List<?> args) {

        if (args.size() <= 1) {
            return "- ERR wrong number of arguments for 'expire' command";
        }

        String key = (String) args.getFirst();
        long expireDurationSec = Long.parseLong((String) args.get(1));


        Store.Obj obj = (Store.Obj) AsyncTcpServer.STORE.get(key);

        if (obj == null) {
            return encodeInteger(0);
        }

        long expiresAt = System.currentTimeMillis() + expireDurationSec * 1000;
        AsyncTcpServer.STORE.put(key, new Store.Obj(obj.value(), expiresAt));

        return encodeInteger(1);
    }

    private static String evalDEL(List<?> args) {
        long keysDeletedCount = 0;

        for (Object key: args) {
            if (AsyncTcpServer.STORE.remove((String) key) != null) {
                keysDeletedCount += 1;
            }
        }

        return encodeInteger(keysDeletedCount);
    }

    private static String evalTTL(List<?> args) {
        if (args.size() != 1) {
            return "- ERR wrong number of arguments for 'get' command\r\n";
        }
        String key = (String) args.getFirst();

        Store.Obj ob = (Store.Obj) AsyncTcpServer.STORE.get(key);

        if (ob == null) {
            return encodeInteger(-2);
        }

        if (ob.expiresAt() == -1) {
            return encodeInteger(-1);
        }

        long durationMs = ob.expiresAt() - System.currentTimeMillis();

        if (durationMs <= 0) {
            return encodeInteger(-2);
        }

        return encodeInteger(durationMs / 1000);
    }

    private static String evalGET(List<?> args) {
        if (args.size() != 1) {
            return "- ERR wrong number of arguments for 'get' command\r\n";
        }

        String key = (String) args.getFirst();

        Store.Obj ob = (Store.Obj) AsyncTcpServer.STORE.get(key);

        if (ob == null) {
            return RESP_NIL;
        }

        if (ob.expiresAt() != -1 && ob.expiresAt() <= System.currentTimeMillis()) {
            AsyncTcpServer.STORE.remove(key);
            return RESP_NIL;
        }

        System.out.println(AsyncTcpServer.STORE);
        return encodeString(ob.value(), false);
    }

    private static String evalSET(List<?> args) {

        if (args.size() <= 1) {
            return "- ERR wrong number of arguments for 'set' command\r\n";
        }

        long expireDurationMS = -1L;
        String key = (String) args.getFirst();
        String value = (String) args.get(1);

        for (int i = 2; i < args.size(); i++) {
            switch ((String) args.get(i)) {
                case "EX", "ex":
                    i += 1;

                    if (i == args.size()) {
                        return "- ERR Syntax error";
                    }

                    expireDurationMS = Long.parseLong(String.valueOf(args.get(i)));
                    expireDurationMS = expireDurationMS * 1000L;
                    break;
                default:
                    return "- ERR Syntax error";
            }
        }

        AsyncTcpServer.STORE.put(key, Store.setExpiration(value, expireDurationMS));
        return encodeString("OK",true);
    }

    public static String evalPING(List<?> args) {
        if (args.size() >= 2) {
            return "- (error) ERR wrong number of arguments for 'ping' command\r\n";
        }

        if (args.isEmpty()) {
            return encodeString("PONG", true);
        } else {
            return encodeString(args.getFirst(), false);
        }
    }
}
