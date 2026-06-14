package org.pkteq.protodb;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import org.pkteq.protodb.core.RedisCmd;
import org.pkteq.protodb.core.Resp;

import static org.pkteq.protodb.core.Eval.evalAndRespond;

public class SyncTcpServer { 
    private static final int PORT = 7379;

    static void main(String[] args) {
        System.out.println("Server starting on port " + PORT + "...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening. Waiting for clients...\n");

            while (true) {
                Socket clientSocket = serverSocket.accept();

                try (clientSocket) {
                    System.out.println("Client connected: " + clientSocket.getInetAddress());
                    while (true) {
                        RedisCmd cmd = readCommand(clientSocket);
                        if (cmd == null || "error".equalsIgnoreCase(cmd.cmd())) {
                            break;
                        }
                        evalAndRespond(cmd, clientSocket);
                    }
                } catch (IOException e) {
                    System.err.println("Client error: " + e.getMessage());
                } finally {
                    System.out.println("Client disconnected.");
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

    private static void respondError(String error, Socket socket) throws IOException {
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), false);
        writer.print(String.format("-%s\r\n", error));
        writer.flush();
    }
}
