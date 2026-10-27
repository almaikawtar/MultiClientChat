package com.chat.server.model;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Pure model: sockets, threads and chat logic. No JavaFX dependency. */
public class ChatServer {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final String host;
    private final int port;
    private final ServerListener listener;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    // One virtual thread per client connection
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private volatile ServerSocket serverSocket;
    private volatile boolean running;

    public ChatServer(String host, int port, ServerListener listener) {
        this.host = host;
        this.port = port;
        this.listener = listener;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port, 50, InetAddress.getByName(host));
        running = true;
        listener.onLog("Server Started on " + host + ":" + port);
        executor.submit(this::acceptLoop);
    }

    private void acceptLoop() {
        while (running) {
            try {
                listener.onLog("Waiting for Client...");
                Socket socket = serverSocket.accept();
                executor.submit(new ClientHandler(socket, this));
            } catch (IOException e) {
                if (running) listener.onLog("Accept error: " + e.getMessage());
            }
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}
        clients.forEach(ClientHandler::close);
        executor.shutdownNow();
        listener.onLog("Server Stopped");
    }

    public boolean isRunning() {
        return running;
    }

    /** @return null on success, otherwise an error message. */
    synchronized String register(ClientHandler handler, String name) {
        if (!name.isEmpty()) {
            if (!name.matches("[\\p{L}\\p{N}_ .-]{1,20}"))
                return "Invalid username (1-20 letters, digits, space, _ . -)";
            boolean taken = clients.stream().anyMatch(c -> name.equalsIgnoreCase(c.getUsername()));
            if (taken) return "Username already in use";
        }
        handler.setUsername(name);
        clients.add(handler);
        return null;
    }

    void announceJoin(ClientHandler h) {
        if (h.isReadOnly()) {
            listener.onLog("Read-only client connected (" + h.address() + ")");
            h.send(Protocol.build(Protocol.INFO, "Connected in READ-ONLY MODE: you can read but not send messages."));
        } else {
            listener.onLog("Welcome " + h.getUsername());
            h.send(Protocol.build(Protocol.INFO, "Welcome " + h.getUsername() + "! Commands: allUsers, end, bye"));
            broadcast(Protocol.build(Protocol.INFO, h.getUsername() + " joined the chat"));
            listener.onUserJoined(h.getUsername());
        }
    }

    void broadcastChat(ClientHandler sender, String text) {
        String line = "[" + LocalTime.now().format(TIME) + "] " + sender.getUsername() + ": " + text;
        listener.onLog(line);
        broadcast(Protocol.build(Protocol.CHAT, line));
    }

    void broadcast(String line) {
        for (ClientHandler c : clients) c.send(line);
    }

    void remove(ClientHandler h) {
        if (clients.remove(h)) {
            if (h.isReadOnly()) {
                listener.onLog("Read-only client disconnected (" + h.address() + ")");
            } else {
                listener.onLog(h.getUsername() + " left the chat");
                broadcast(Protocol.build(Protocol.INFO, h.getUsername() + " left the chat"));
                listener.onUserLeft(h.getUsername());
            }
        }
    }

    List<String> userNames() {
        return clients.stream().filter(c -> !c.isReadOnly()).map(ClientHandler::getUsername).toList();
    }
}
