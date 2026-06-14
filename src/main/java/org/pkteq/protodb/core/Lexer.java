package org.pkteq.protodb.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

public class Lexer {
    private final InputStream input;
    private int currentByte;

    public Lexer(InputStream input) {
        this.input = input;
    }

    private void next() throws IOException {
        this.currentByte = input.read();
    }

    public List<Object> generateTokens() throws IOException {
        List<Object> tokens = new LinkedList<>();

        // Skip leading whitespace or CRLFs if any
        do {
            this.next();
        } while (this.currentByte == '\r' || this.currentByte == '\n');

        if (this.currentByte == -1) return null;

        Object token = parseNext();
        if (token != null) {
            tokens.add(token);
        }
        return tokens;
    }

    private Object parseNext() throws IOException {
        if (this.currentByte == -1) return null;

        return switch ((char) this.currentByte) {
            case '+' -> handleSimpleString();
            case '-' -> handleError();
            case ':' -> handleInteger();
            case '$' -> handleBulkString();
            case '*' -> handleArray();
            default -> null;
        };
    }

    private String handleSimpleString() throws IOException {
        StringBuilder sb = new StringBuilder();
        next();
        while (this.currentByte != -1 && this.currentByte != '\r') {
            sb.append((char) this.currentByte);
            next();
        }
        consumeCRLF();
        return sb.toString();
    }

    private String handleError() throws IOException {
        return handleSimpleString();
    }

    private Long handleInteger() throws IOException {
        StringBuilder sb = new StringBuilder();
        next();
        while (this.currentByte != -1 && this.currentByte != '\r') {
            sb.append((char) this.currentByte);
            next();
        }
        consumeCRLF();
        return Long.parseLong(sb.toString());
    }

    private String handleBulkString() throws IOException {
        long len = handleInteger();
        if (len == -1) return null;

        byte[] bytes = new byte[(int) len];
        int read = 0;
        while (read < len) {
            int n = input.read(bytes, read, (int) (len - read));
            if (n == -1) throw new IOException("Unexpected EOF in bulk string");
            read += n;
        }
        // Bulk strings are followed by CRLF
        // We need to consume exactly 2 bytes
        int cr = input.read();
        int lf = input.read();
        return new String(bytes);
    }

    private List<Object> handleArray() throws IOException {
        long len = handleInteger();
        if (len == -1) return null;

        List<Object> list = new LinkedList<>();
        for (int i = 0; i < len; i++) {
            next(); // Move to the prefix of the next element (+, :, $, *)
            list.add(parseNext());
        }
        return list;
    }

    private void consumeCRLF() throws IOException {
        if (this.currentByte == '\r') {
            next();
            if (this.currentByte == '\n') {
                // Do not call next() here, because the loop/caller usually calls next()
                // or we want to be ready for the next command's prefix.
            }
        }
    }
}
