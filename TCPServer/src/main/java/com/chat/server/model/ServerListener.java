package com.chat.server.model;

/** Callbacks from the model to whoever presents it (the JavaFX view). Called from worker threads. */
public interface ServerListener {
    void onLog(String message);
    void onUserJoined(String username);
    void onUserLeft(String username);
}
