# Multi-Client Chat Application

A real-time group chat application built with **Java 21, TCP Sockets, Virtual Threads, and JavaFX**.

The application follows a client-server architecture: multiple clients connect to a central server, exchange messages, and view the list of active users. The server handles connections and distributes messages to the connected clients.

**Mini Project 1 — Advanced Distributed Programming Paradigms**

```
chat-project/
├── TCPServer/   Maven project (server)  -> target/TCPServer.jar
├── TCPClient/   Maven project (client)  -> target/TCPClient.jar
└── docs/        PlantUML: class, deployment, sequence, use-case diagrams
```

## Requirements
* JDK 21+  and  Maven 3.9+
* Open each folder (`TCPServer`, `TCPClient`) as a Maven project in IntelliJ IDEA.

## Build
```bash
cd TCPServer && mvn clean package     # -> target/TCPServer.jar
cd ../TCPClient && mvn clean package  # -> target/TCPClient.jar
```
The JARs are fat JARs (JavaFX bundled for the OS you build on).

## Run
```bash
# 1. server (reads host/port from config.properties)
java -jar TCPServer/target/TCPServer.jar          # or: cd TCPServer && mvn javafx:run

# 2. one or more clients
java -jar TCPClient/target/TCPClient.jar localhost 3000
# or: cd TCPClient && mvn javafx:run -Djavafx.args="localhost 3000"
```
From compiled classes this is `java TCPServer` / `java TCPClient localhost 3000`.

## Configuration
`config.properties` is bundled in each JAR; a `config.properties` placed in the **working directory**
overrides it with no recompilation.
* Server: `server.host` (bind address, default `0.0.0.0`), `server.port` (default `3000`).
* Client: command-line `<ServerIPAddress> <PortNumber>` take priority; `config.properties` is the fallback.

## Features checklist
| Requirement | Where |
|---|---|
| Username before chatting | Login screen (`ClientApp.showLogin`) |
| READ-ONLY MODE (no username) | Input disabled in client **and** enforced by server (`ClientHandler`) |
| Send via SEND button / Enter | `ClientApp.showChat` (Shift+Enter = new line) |
| `allUsers` command | `ChatClient.handleInput` -> `ALLUSERS` -> `USERS|...` reply to that client only |
| `end`, `bye`, LEAVE | `ChatClient.handleInput` / `disconnect()` sends `LEAVE`, server confirms `BYE` |
| "Online" label + circle | Status bar of chat screen |
| Concurrent clients | `Executors.newVirtualThreadPerTaskExecutor()` in `ChatServer` |
| Messages formatted `[HH:mm:ss] user: text` | `ChatServer.broadcastChat` |
| Live user ListView + random colours | `ServerApp` (`ListCell` with random pastel HSB colour per user) |
| Server log ("Server Started", "Waiting for Client...", "Welcome X") | `ChatServer` -> `ServerListener.onLog` |
| Config file | `ServerConfig`, `ClientConfig` |
| Model/View decoupling | `*.model` packages have **no** JavaFX imports; views talk to them via `ServerListener` / `ChatListener` |

Note: a message is sent to **all** active connections including the sender, so the sender sees the same
timestamped line as everyone else. Usernames are unique (case-insensitive) and limited to 1-20 chars.

## Protocol (one text line per message, `COMMAND|payload`)
Client -> server: `HELLO|name` (empty = read-only), `MSG|text`, `ALLUSERS|`, `LEAVE|`
Server -> client: `OK|`, `ERROR|reason`, `CHAT|line`, `INFO|text`, `USERS|a,b,c`, `BYE|`

## UML
Sources in `docs/*.puml`. Render with the PlantUML IntelliJ plugin or https://www.plantuml.com/plantuml
and export PNG/PDF for the submission.

## Deliverables still to do on your side
1. Build and test the JARs on your machine.
2. Record the 3-minute demo (suggested script: show `config.properties` -> start server -> join 2 users + 1 read-only
   -> chat -> `allUsers` -> `bye` / LEAVE -> brief walk through `model` vs `view` packages and virtual threads).
3. Export the UML diagrams.
4. `git init`, commit regularly, push to GitHub and share the link.
