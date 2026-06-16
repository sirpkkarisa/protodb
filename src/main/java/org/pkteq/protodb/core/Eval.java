package org.pkteq.protodb.core;

import java.util.List;
import static org.pkteq.protodb.core.Resp.encodeString;

public class Eval {
    public static String eval(RedisCmd redisCmd) {
        String cmd = redisCmd.cmd().toUpperCase();
        switch (cmd) {
            case "PING":
                return evalPING(redisCmd.args());
            case "COMMAND":
                return encodeString("OK", true);
            default:
                return evalPING(redisCmd.args());
        }
    }

    public static String evalPING(List<?> args) {
        if (args.size() >= 2) {
            return "-ERR wrong number of arguments for 'ping' command\r\n";
        }

        if (args.isEmpty()) {
            return encodeString("PONG", true);
        } else {
            return encodeString(args.getFirst(), false);
        }
    }
}
