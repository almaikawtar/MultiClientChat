package com.chat.client.model;

/** Line-based text protocol: COMMAND|payload (identical copy of the server's class). */
public final class Protocol {
    private Protocol() {}

    public static final String HELLO = "HELLO";
    public static final String MSG = "MSG";
    public static final String ALL_USERS = "ALLUSERS";
    public static final String LEAVE = "LEAVE";
    public static final String OK = "OK";
    public static final String ERROR = "ERROR";
    public static final String CHAT = "CHAT";
    public static final String INFO = "INFO";
    public static final String USERS = "USERS";
    public static final String BYE = "BYE";

    public static String build(String command, String payload) {
        return command + "|" + payload;
    }

    public static String[] parse(String line) {
        int i = line.indexOf('|');
        return i < 0 ? new String[]{line, ""} : new String[]{line.substring(0, i), line.substring(i + 1)};
    }
}
