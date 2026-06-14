package org.pkteq.protodb.core;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

import static org.pkteq.protodb.core.Resp.encodeString;

public class Eval {
    public static String evalAndRespond(RedisCmd redisCmd, Socket socket) throws IOException {
        String cmd = redisCmd.cmd().toUpperCase();
        switch (cmd) {
            case "PING":
                return evalPING(redisCmd.args(), socket);
            case "COMMAND":
                // redis-cli often sends 'COMMAND' on startup. Return an empty array or OK.
                return sendResponse(encodeString("OK", true), socket);
            default:
                return evalPING(redisCmd.args(), socket);
        }
    }

    private static String sendResponse(String result, Socket socket) throws IOException {
        PrintWriter out = new PrintWriter(socket.getOutputStream(), false);
        out.print(result);
        out.flush(); // CRITICAL: Ensure data is sent immediately
        return result;
    }

    public static String evalPING(List<?> args, Socket socket) throws IOException {
        if (args.size() >= 2) {
            return sendResponse("-ERR wrong number of arguments for 'ping' command\r\n", socket);
        }

        String result;
        if (args.isEmpty()) {
            result = encodeString("PONG", true);
        } else {
            result = encodeString(args.getFirst(), false);
        }

        return sendResponse(result, socket);
    }

}
