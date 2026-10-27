package com.chat.server.model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Loads host/port at runtime: ./config.properties if present, otherwise the bundled default. */
public record ServerConfig(String host, int port) {

    public static ServerConfig load() {
        Properties p = new Properties();
        Path external = Path.of("config.properties");
        try (InputStream in = Files.exists(external)
                ? Files.newInputStream(external)
                : ServerConfig.class.getResourceAsStream("/config.properties")) {
            if (in != null) p.load(in);
        } catch (IOException e) {
            System.err.println("Could not read config.properties, using defaults: " + e.getMessage());
        }
        String host = p.getProperty("server.host", "0.0.0.0").trim();
        int port;
        try {
            port = Integer.parseInt(p.getProperty("server.port", "3000").trim());
        } catch (NumberFormatException e) {
            port = 3000;
        }
        return new ServerConfig(host, port);
    }
}
