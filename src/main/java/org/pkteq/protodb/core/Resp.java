package org.pkteq.protodb.core;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class Resp {

    public static String encodeSimpleString(String str) {
        return "+" + str + "\r\n";
    }


    public static String encodeError(String error) {
        return "-" + error + "\r\n";
    }

    public static String encodeInteger(long number) {
        return ":" + number + "\r\n";
    }

    public static String encodeBulkString(String str) {
        if (str == null) {
            return "$-1\r\n"; // Null bulk string
        }
        return "$" + str.length() + "\r\n" + str + "\r\n";
    }

    public static String encodeArray(String[] elements) {
        if (elements == null) {
            return "*-1\r\n"; // Null array
        }
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(elements.length).append("\r\n");
        for (String element : elements) {

            try {
                int e = Integer.parseInt(element);
                sb.append(encodeInteger(e));
            } catch (NumberFormatException ex) {
                // Not an integer, treat as bulk string
                sb.append(encodeBulkString(element));
            }


        }
        return sb.toString();
    }

//    public static String encodeCmd(Cmd cmd) {
////        String[] elements = new String[cmd.args().length + 1];
//
//        elements[0] = cmd.cmd().toUpperCase();
//        System.arraycopy(cmd.args(), 0, elements, 1, cmd.args().length);
//        return encodeArray(elements);
//    }

    public static List<?> decodeString(String fmt) {
        return null; // Deprecated or remove
    }

    public static List<?> decodeStream(java.io.InputStream input) throws java.io.IOException {
        return (new Lexer(input)).generateTokens();
    }

    public static String encodeString(Object value, boolean isSimple){
        String str = String.valueOf(value);
        if (isSimple) return String.format("+%s\r\n", str);
        else return String.format("$%d\r\n%s\r\n", str.length(), str);

    }
}
