package com.chat.server.view;

import com.chat.server.model.ChatServer;
import com.chat.server.model.ServerConfig;
import com.chat.server.model.ServerListener;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** JavaFX view for the server. All socket logic lives in ChatServer (model). */
public class ServerApp extends Application implements ServerListener {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ObservableList<String> users = FXCollections.observableArrayList();
    private final Map<String, Color> colors = new HashMap<>();
    private final Random random = new Random();

    private final TextArea logArea = new TextArea();
    private final Label statusLabel = new Label("Stopped");
    private final Circle statusDot = new Circle(7, Color.GRAY);
    private final Button toggleButton = new Button("START SERVER");
    private ChatServer server;

    @Override
    public void start(Stage stage) {
        // --- header
        Label title = new Label("Chat Server");
        title.getStyleClass().add("title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(8, title, spacer, statusDot, statusLabel);
        header.setAlignment(Pos.CENTER_LEFT);

        // --- connected users (random background per user)
        ListView<String> userList = new ListView<>(users);
        userList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    Color c = colors.computeIfAbsent(item, k -> Color.hsb(random.nextDouble() * 360, 0.35, 0.95));
                    setStyle("-fx-background-color: " + toWeb(c) + "; -fx-text-fill: #222222;");
                }
            }
        });

        logArea.setEditable(false);
        logArea.setWrapText(true);

        toggleButton.setMaxWidth(Double.MAX_VALUE);
        toggleButton.setOnAction(e -> {
            if (server != null && server.isRunning()) stopServer();
            else startServer();
        });

        // --- layout (GridPane)
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.setPadding(new Insets(16));
        grid.add(header, 0, 0, 2, 1);
        grid.add(new Label("Connected users"), 0, 1);
        grid.add(new Label("Activity log"), 1, 1);
        grid.add(userList, 0, 2);
        grid.add(logArea, 1, 2);
        grid.add(toggleButton, 0, 3, 2, 1);

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setPercentWidth(32);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(68);
        grid.getColumnConstraints().addAll(c0, c1);
        RowConstraints r2 = new RowConstraints();
        r2.setVgrow(Priority.ALWAYS);
        grid.getRowConstraints().addAll(new RowConstraints(), new RowConstraints(), r2, new RowConstraints());

        Scene scene = new Scene(grid, 760, 520);
        scene.getStylesheets().add(getClass().getResource("/server.css").toExternalForm());
        stage.setTitle("TCP Chat Server");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            stopServer();
            Platform.exit();
            System.exit(0);
        });
        stage.show();

        startServer();
    }

    private void startServer() {
        try {
            ServerConfig cfg = ServerConfig.load();
            server = new ChatServer(cfg.host(), cfg.port(), this);
            server.start();
            statusDot.setFill(Color.LIMEGREEN);
            statusLabel.setText("Running on port " + cfg.port());
            toggleButton.setText("STOP SERVER");
        } catch (IOException e) {
            appendLog("Failed to start server: " + e.getMessage());
        }
    }

    private void stopServer() {
        if (server != null && server.isRunning()) server.stop();
        users.clear();
        statusDot.setFill(Color.GRAY);
        statusLabel.setText("Stopped");
        toggleButton.setText("START SERVER");
    }

    // ---- ServerListener (called from worker threads -> hop onto the FX thread)
    @Override public void onLog(String message) { Platform.runLater(() -> appendLog(message)); }
    @Override public void onUserJoined(String u) { Platform.runLater(() -> { if (!users.contains(u)) users.add(u); }); }
    @Override public void onUserLeft(String u) { Platform.runLater(() -> { users.remove(u); colors.remove(u); }); }

    private void appendLog(String message) {
        logArea.appendText("[" + LocalTime.now().format(TIME) + "] " + message + "\n");
    }

    private static String toWeb(Color c) {
        return String.format("#%02x%02x%02x",
                (int) Math.round(c.getRed() * 255), (int) Math.round(c.getGreen() * 255), (int) Math.round(c.getBlue() * 255));
    }
}
