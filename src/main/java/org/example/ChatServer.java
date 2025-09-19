package org.example;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ChatServer {
    private final int port;
    private final ConcurrentMap<String, ClientHandler> users = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Transfer> transfers = new ConcurrentHashMap<>();

    public ChatServer(int port) { this.port = port; }

    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("server lytter på port " + port);
        ExecutorService pool = Executors.newCachedThreadPool();
        while (true) {
            Socket socket = serverSocket.accept();
            pool.execute(new ClientHandler(socket, this));
        }
    }

    // brugerhåndtering
    void registerUser(String username, ClientHandler handler) {
        users.put(username, handler);
        System.out.println("login: " + username);
    }

    void deregisterUser(String username) {
        if (username != null) {
            users.remove(username);
            System.out.println("logout: " + username);
        }
    }

    ClientHandler findUser(String username) { return users.get(username); }

    // file offer -> prompt modtager
    void handleFileOffer(String from, String to, String filename, long size, ClientHandler sender) {
        ClientHandler receiver = findUser(to);
        if (receiver == null) {
            sender.sendJson(Json.obj(
                    "type","ERROR","message","modtager er ikke online"
            ));
            return;
        }
        String transferId = java.util.UUID.randomUUID().toString();
        Transfer t = new Transfer(transferId, from, to, filename, size);
        transfers.put(transferId, t);

        receiver.sendJson(Json.obj(
                "type","FILE_OFFER_PROMPT",
                "from", from,
                "to", to,
                "transferId", transferId,
                "filename", filename,
                "size", size
        ));
    }

    void handleFileAccept(String from, String transferId) {
        Transfer t = transfers.get(transferId);
        if (t == null) return;
        if (!t.to.equals(from)) return; // kun rette modtager
        t.accepted.set(true);

        ClientHandler sender = findUser(t.from);
        if (sender != null) {
            sender.sendJson(Json.obj(
                    "type","INFO",
                    "to", t.from,
                    "transferId", t.id,
                    "status", "ACCEPTED"
            ));
        }
    }

    void handleFileReject(String from, String transferId) {
        Transfer t = transfers.remove(transferId);
        if (t == null) return;
        if (!t.to.equals(from)) return;

        ClientHandler sender = findUser(t.from);
        if (sender != null) {
            sender.sendJson(Json.obj(
                    "type","INFO",
                    "to", t.from,
                    "transferId", t.id,
                    "status", "REJECTED"
            ));
        }
    }

    void handleFileChunk(String from, String transferId, long seq, String base64) {
        Transfer t = transfers.get(transferId);
        if (t == null) return;
        if (!t.from.equals(from)) return;
        if (!t.accepted.get()) return;

        ClientHandler receiver = findUser(t.to);
        if (receiver != null) {
            receiver.sendJson(Json.obj(
                    "type","FILE_CHUNK",
                    "from", from,
                    "transferId", transferId,
                    "seq", seq,
                    "data", base64
            ));
        }
        try {
            t.transferredBytes.addAndGet(java.util.Base64.getDecoder().decode(base64).length);
        } catch (IllegalArgumentException ignored) {}
    }

    void handleFileEnd(String from, String transferId) {
        Transfer t = transfers.remove(transferId);
        if (t == null) return;
        if (!t.from.equals(from)) return;

        ClientHandler receiver = findUser(t.to);
        if (receiver != null) {
            receiver.sendJson(Json.obj(
                    "type","FILE_END",
                    "from", from,
                    "transferId", transferId
            ));
        }
        System.out.printf("transfer %s afsluttet (%d/%d bytes)%n",
                transferId, t.transferredBytes.get(), t.size);
    }

    public static void main(String[] args) throws IOException {
        int port = 5555;
        if (args.length > 0) port = Integer.parseInt(args[0]);
        new ChatServer(port).start();
    }
}
