package org.pkteq.protodb;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import org.pkteq.protodb.core.RedisCmd;
import org.pkteq.protodb.core.Resp;
import org.pkteq.protodb.core.Eval;

public class SyncTcpServer { 
    private static final int PORT = 7379;

    static void main(String[] args) {
        System.out.println("Server starting on port " + PORT + "...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening. Waiting for clients...\n");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                try {
                    while (true) {
                        RedisCmd cmd = readCommand(clientSocket);
                        if (cmd == null || "error".equalsIgnoreCase(cmd.cmd())) {
                            break;
                        }
                        String result = Eval.eval(cmd);
                        sendResponse(result, clientSocket);
                    }
                } catch (IOException e) {
                    System.err.println("Client error: " + e.getMessage());
                } finally {
                    System.out.println("Client disconnected.");
                    clientSocket.close();
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private static RedisCmd readCommand(Socket socket) throws IOException {
        List<?> response = Resp.decodeStream(socket.getInputStream());
        if (response != null && !response.isEmpty()) {
            List<?> cmdList = (List<?>) response.getFirst();
            String name = (String) cmdList.getFirst();
            List<?> args = cmdList.subList(1, cmdList.size());
            return new RedisCmd(name, args);
        }
        return null;
    }

    private static void sendResponse(String result, Socket socket) throws IOException {
        PrintWriter out = new PrintWriter(socket.getOutputStream(), false);
        out.print(result);
        out.flush();
    }
}
