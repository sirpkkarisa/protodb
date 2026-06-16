package org.pkteq.protodb.core;

import org.pkteq.protodb.AsyncTcpServer;

import java.util.List;
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
            case "COMMAND" -> encodeString("OK", true);
            default -> evalPING(redisCmd.args());
        };
    }

    private static String evalTTL(List<?> args) {
        if (args.size() != 1) {
            return "- ERR wrong number of arguments for 'get' command\r\n";
        }
        String key = (String) args.getFirst();

        Store.Obj ob = (Store.Obj) AsyncTcpServer.STORE.get(key);

        if (ob == null) {
            return RESP_EMPTY;
        }

        if (ob.expiresAt() == -1) {
            return RESP_NIL;
        }

        long durationMs = ob.expiresAt() - System.currentTimeMillis();

        if (durationMs <= 0) {
            return RESP_EMPTY;
        }

        return encodeString(ob.expiresAt() / 1000, false);
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
            return RESP_NIL;
        }

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
