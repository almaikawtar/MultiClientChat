package com.chat.server.model;

/** Line-based text protocol: COMMAND|payload  (one message per line). */
public final class Protocol {
    private Protocol() {}

    // client -> server
    public static final String HELLO = "HELLO";       // HELLO|username   (empty username = read-only)
    public static final String MSG = "MSG";           // MSG|text
    public static final String ALL_USERS = "ALLUSERS";
    public static final String LEAVE = "LEAVE";
    // server -> client
    public static final String OK = "OK";
    public static final String ERROR = "ERROR";       // ERROR|reason
    public static final String CHAT = "CHAT";         // CHAT|[HH:mm:ss] user: text
    public static final String INFO = "INFO";         // INFO|server notice
    public static final String USERS = "USERS";       // USERS|a,b,c
    public static final String BYE = "BYE";

    public static String build(String command, String payload) {
        return command + "|" + payload;
    }

    /** @return {command, payload} (payload is "" when absent) */
    public static String[] parse(String line) {
        int i = line.indexOf('|');
        return i < 0 ? new String[]{line, ""} : new String[]{line.substring(0, i), line.substring(i + 1)};
    }
}
