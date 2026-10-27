package com.chat.server.model;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Serves one client connection; runs on its own virtual thread. */
class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChatServer server;
    private PrintWriter out;
    private volatile String username = "";

    ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    String getUsername() { return username; }
    void setUsername(String u) { this.username = u; }
    boolean isReadOnly() { return username.isEmpty(); }
    String address() { return String.valueOf(socket.getRemoteSocketAddress()); }

    void send(String line) {
        if (out != null) out.println(line);
    }

    void close() {
        try { socket.close(); } catch (IOException ignored) {}
    }

    @Override
    public void run() {
        boolean registered = false;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            String hello = in.readLine();
            if (hello == null) return;
            String[] f = Protocol.parse(hello);
            if (!Protocol.HELLO.equals(f[0])) {
                send(Protocol.build(Protocol.ERROR, "Expected HELLO"));
                return;
            }
            String error = server.register(this, f[1].trim());
            if (error != null) {
                send(Protocol.build(Protocol.ERROR, error));
                return;
            }
            registered = true;
            send(Protocol.build(Protocol.OK, ""));
            server.announceJoin(this);

            String line;
            loop:
            while ((line = in.readLine()) != null) {
                f = Protocol.parse(line);
                switch (f[0]) {
                    case Protocol.MSG -> {
                        if (isReadOnly()) send(Protocol.build(Protocol.ERROR, "READ-ONLY MODE: you cannot send messages"));
                        else if (!f[1].isBlank()) server.broadcastChat(this, f[1]);
                    }
                    case Protocol.ALL_USERS ->
                        send(Protocol.build(Protocol.USERS, String.join(",", server.userNames())));
                    case Protocol.LEAVE -> {
                        send(Protocol.build(Protocol.BYE, ""));
                        break loop;
                    }
                    default -> send(Protocol.build(Protocol.ERROR, "Unknown command"));
                }
            }
        } catch (IOException ignored) {
            // connection dropped
        } finally {
            if (registered) server.remove(this);
            close();
        }
    }
}
