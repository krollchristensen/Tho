package org.example;

import java.io.*;
import java.net.Socket;
import java.util.Map;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChatServer server;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                Map<String, Object> msg = Json.parse(line);
                String type = (String) msg.get("type");
                if (type == null) { sendJson(Json.obj("type","ERROR","message","missing type")); continue; }

                switch (type) {
                    case "LOGIN" -> {
                        String from = (String) msg.get("from");
                        if (from == null || from.isBlank()) {
                            sendJson(Json.obj("type","ERROR","message","ugyldigt brugernavn"));
                            break;
                        }
                        this.username = from;
                        server.registerUser(from, this);
                        sendJson(Json.obj("type","INFO","message","LOGIN_OK"));
                    }
                    case "FILE_OFFER" -> {
                        String from = (String) msg.get("from");
                        String to = (String) msg.get("to");
                        String filename = (String) msg.get("filename");
                        long size = Json.asLong(msg.get("size"), -1L);
                        server.handleFileOffer(from, to, filename, size, this);
                    }
                    case "FILE_ACCEPT" -> {
                        String transferId = (String) msg.get("transferId");
                        server.handleFileAccept(username, transferId);
                    }
                    case "FILE_REJECT" -> {
                        String transferId = (String) msg.get("transferId");
                        server.handleFileReject(username, transferId);
                    }
                    case "FILE_CHUNK" -> {
                        String transferId = (String) msg.get("transferId");
                        long seq = Json.asLong(msg.get("seq"), 0L);
                        String data = (String) msg.get("data");
                        server.handleFileChunk(username, transferId, seq, data);
                    }
                    case "FILE_END" -> {
                        String transferId = (String) msg.get("transferId");
                        server.handleFileEnd(username, transferId);
                    }
                    default -> sendJson(Json.obj("type","ERROR","message","ukendt type: " + type));
                }
            }
        } catch (IOException e) {
            // luk stille
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
            server.deregisterUser(username);
        }
    }

    public void sendJson(Map<String, Object> map) { out.println(Json.stringify(map)); }
}
