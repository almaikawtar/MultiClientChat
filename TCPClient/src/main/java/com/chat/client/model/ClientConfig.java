package com.chat.client.model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/** Server address: command-line arguments win, otherwise config.properties. */
public record ClientConfig(String host, int port) {

    public static final String USAGE = "Usage: java TCPClient <ServerIPAddress> <PortNumber>   e.g. java TCPClient localhost 3000";

    public static ClientConfig fromArgs(List<String> args) {
        if (args.size() == 2) {
            try {
                int port = Integer.parseInt(args.get(1));
                if (port < 1 || port > 65535) throw new NumberFormatException();
                return new ClientConfig(args.get(0), port);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid port number '" + args.get(1) + "'. " + USAGE);
            }
        }
        if (!args.isEmpty()) throw new IllegalArgumentException(USAGE);
        return fromProperties();
    }

    private static ClientConfig fromProperties() {
        Properties p = new Properties();
        Path external = Path.of("config.properties");
        try (InputStream in = Files.exists(external)
                ? Files.newInputStream(external)
                : ClientConfig.class.getResourceAsStream("/config.properties")) {
            if (in != null) p.load(in);
        } catch (IOException e) {
            System.err.println("Could not read config.properties, using defaults: " + e.getMessage());
        }
        int port;
        try {
            port = Integer.parseInt(p.getProperty("server.port", "3000").trim());
        } catch (NumberFormatException e) {
            port = 3000;
        }
        return new ClientConfig(p.getProperty("server.host", "localhost").trim(), port);
    }
}
