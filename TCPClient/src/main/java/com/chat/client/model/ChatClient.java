package com.chat.client.model;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Pure model: socket communication + command handling. No JavaFX dependency. */
public class ChatClient {

    /** Events delivered from the reader thread; the view must hop to its own UI thread. */
    public interface ChatListener {
        void onMessage(String text);
        void onUsers(List<String> users);
        void onDisconnected(String reason);
    }

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ChatListener listener;
    private String username = "";
    private final AtomicBoolean notified = new AtomicBoolean(false);
    private volatile boolean connected;

    public void setListener(ChatListener listener) { this.listener = listener; }
    public String getUsername() { return username; }
    public boolean isReadOnly() { return username.isEmpty(); }
    public boolean isConnected() { return connected; }

    /** Connects and performs the handshake. An empty username means READ-ONLY MODE. */
    public void connect(String host, int port, String username) throws IOException {
        this.username = username == null ? "" : username.trim();
        socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), 5000);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            out.println(Protocol.build(Protocol.HELLO, this.username));

            String reply = in.readLine();
            if (reply == null) throw new IOException("Server closed the connection");
            String[] f = Protocol.parse(reply);
            if (Protocol.ERROR.equals(f[0])) throw new IOException(f[1]);
            if (!Protocol.OK.equals(f[0])) throw new IOException("Unexpected server reply");
            connected = true;
        } catch (IOException e) {
            closeQuietly();
            throw e;
        }
    }

    /** Starts the background reader (virtual thread). Call once the UI is ready to receive events. */
    public void startListening() {
        Thread.ofVirtual().name("client-reader").start(this::readLoop);
    }

    private void readLoop() {
        String reason = "Connection to the server was lost";
        try {
            String line;
            while ((line = in.readLine()) != null) {
                String[] f = Protocol.parse(line);
                switch (f[0]) {
                    case Protocol.CHAT, Protocol.INFO -> fire(l -> l.onMessage(f[1]));
                    case Protocol.ERROR -> fire(l -> l.onMessage("! " + f[1]));
                    case Protocol.USERS -> {
                        List<String> names = f[1].isEmpty() ? List.of() : Arrays.asList(f[1].split(","));
                        fire(l -> l.onUsers(names));
                    }
                    case Protocol.BYE -> {
                        reason = "You left the chat";
                        closeQuietly();
                        notifyDisconnected(reason);
                        return;
                    }
                    default -> { }
                }
            }
        } catch (IOException ignored) {
            // socket closed or broken
        }
        closeQuietly();
        notifyDisconnected(reason);
    }

    /**
     * Handles whatever the user typed: "end"/"bye" disconnect, "allUsers" asks the server for the
     * active-user list, anything else is sent as a chat message.
     * @throws IllegalStateException in READ-ONLY MODE or when not connected
     */
    public void handleInput(String raw) {
        if (raw == null) return;
        String text = raw.replaceAll("\\s*[\\r\\n]+\\s*", " ").trim();
        if (text.isEmpty()) return;
        if (!connected) throw new IllegalStateException("Not connected");
        if (isReadOnly()) throw new IllegalStateException("READ-ONLY MODE: you cannot send messages");

        if (text.equalsIgnoreCase("end") || text.equalsIgnoreCase("bye")) {
            disconnect();
        } else if (text.equalsIgnoreCase("allUsers")) {
            out.println(Protocol.build(Protocol.ALL_USERS, ""));
        } else {
            out.println(Protocol.build(Protocol.MSG, text));
        }
    }

    /** Notifies the server and closes the connection. */
    public void disconnect() {
        if (connected && out != null) out.println(Protocol.build(Protocol.LEAVE, ""));
        connected = false;
        closeQuietly();
        notifyDisconnected("You left the chat");
    }

    private void notifyDisconnected(String reason) {
        connected = false;
        if (notified.compareAndSet(false, true) && listener != null) listener.onDisconnected(reason);
    }

    private void fire(java.util.function.Consumer<ChatListener> action) {
        if (listener != null) action.accept(listener);
    }

    private void closeQuietly() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}
