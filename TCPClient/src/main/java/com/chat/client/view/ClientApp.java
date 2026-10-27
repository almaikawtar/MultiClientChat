package com.chat.client.view;

import com.chat.client.model.ChatClient;
import com.chat.client.model.ClientConfig;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

/** JavaFX view for the client (login screen + chat screen). All networking lives in ChatClient. */
public class ClientApp extends Application {
    private Stage stage;
    private ClientConfig config;
    private ChatClient client;

    private TextField usernameField;
    private Label loginStatus;
    private Button joinButton;
    private TextArea chatArea;
    private TextArea inputArea;

    private final ChatClient.ChatListener listener = new ChatClient.ChatListener() {
        @Override public void onMessage(String text) { Platform.runLater(() -> append(text)); }
        @Override public void onUsers(List<String> users) {
            Platform.runLater(() -> append("Active users (" + users.size() + "): " + String.join(", ", users)));
        }
        @Override public void onDisconnected(String reason) {
            Platform.runLater(() -> { client = null; showLogin(reason); });
        }
    };

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        try {
            config = ClientConfig.fromArgs(getParameters().getRaw());
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            Platform.exit();
            return;
        }
        stage.setTitle("TCP Chat Client");
        stage.setOnCloseRequest(e -> {
            if (client != null) client.disconnect();
            Platform.exit();
            System.exit(0);
        });
        showLogin(null);
        stage.show();
    }

    // ------------------------------------------------------------------ login screen
    private void showLogin(String message) {
        Label title = new Label("Group Chat");
        title.getStyleClass().add("title");
        Label server = new Label("Server: " + config.host() + ":" + config.port());
        Label hint = new Label("Username (leave empty for READ-ONLY MODE)");

        usernameField = new TextField();
        usernameField.setPromptText("Your username");
        joinButton = new Button("JOIN");
        joinButton.setDefaultButton(true);
        joinButton.setOnAction(e -> connect());
        loginStatus = new Label(message == null ? "" : message);
        loginStatus.getStyleClass().add("error");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(30));
        grid.add(title, 0, 0, 2, 1);
        grid.add(server, 0, 1, 2, 1);
        grid.add(hint, 0, 2, 2, 1);
        grid.add(usernameField, 0, 3);
        grid.add(joinButton, 1, 3);
        grid.add(loginStatus, 0, 4, 2, 1);
        GridPane.setHgrow(usernameField, Priority.ALWAYS);

        setScene(grid, 460, 280);
    }

    private void connect() {
        String name = usernameField.getText().trim();
        joinButton.setDisable(true);
        loginStatus.setText("Connecting...");
        ChatClient c = new ChatClient();
        c.setListener(listener);
        Thread.ofVirtual().start(() -> {
            try {
                c.connect(config.host(), config.port(), name);
                Platform.runLater(() -> {
                    client = c;
                    showChat(c);
                    c.startListening(); // only after the UI exists
                });
            } catch (IOException ex) {
                Platform.runLater(() -> {
                    joinButton.setDisable(false);
                    loginStatus.setText("Could not join: " + ex.getMessage());
                });
            }
        });
    }

    // ------------------------------------------------------------------ chat screen
    private void showChat(ChatClient c) {
        // status bar: indicator + "Online" label
        Circle dot = new Circle(7, Color.LIMEGREEN);
        Label online = new Label("Online");
        online.getStyleClass().add("online");
        Label who = new Label(c.isReadOnly() ? "READ-ONLY MODE" : "User: " + c.getUsername());
        who.getStyleClass().add(c.isReadOnly() ? "readonly" : "user");
        Button leave = new Button("LEAVE");
        leave.getStyleClass().add("leave");
        leave.setOnAction(e -> c.disconnect());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(8, dot, online, spacer, who, leave);
        bar.setAlignment(Pos.CENTER_LEFT);

        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);

        inputArea = new TextArea();
        inputArea.setPrefRowCount(2);
        inputArea.setWrapText(true);
        Button send = new Button("SEND");
        send.setMaxHeight(Double.MAX_VALUE);
        send.setOnAction(e -> send(c));

        if (c.isReadOnly()) {
            inputArea.setDisable(true);
            send.setDisable(true);
            inputArea.setPromptText("READ-ONLY MODE: you cannot send messages");
        } else {
            inputArea.setPromptText("Type a message (Enter = send, Shift+Enter = new line). Commands: allUsers, end, bye");
            inputArea.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == KeyCode.ENTER) {
                    e.consume();
                    if (e.isShiftDown()) inputArea.appendText("\n");
                    else send(c);
                }
            });
        }

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(14));
        grid.add(bar, 0, 0, 2, 1);
        grid.add(chatArea, 0, 1, 2, 1);
        grid.add(inputArea, 0, 2);
        grid.add(send, 1, 2);
        ColumnConstraints c0 = new ColumnConstraints();
        c0.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c0, new ColumnConstraints());
        RowConstraints r1 = new RowConstraints();
        r1.setVgrow(Priority.ALWAYS);
        grid.getRowConstraints().addAll(new RowConstraints(), r1, new RowConstraints());

        setScene(grid, 640, 560);
        if (!c.isReadOnly()) inputArea.requestFocus();
    }

    private void send(ChatClient c) {
        String text = inputArea.getText();
        inputArea.clear();
        try {
            c.handleInput(text);
        } catch (IllegalStateException ex) {
            append("! " + ex.getMessage());
        }
    }

    private void append(String text) {
        if (chatArea != null) chatArea.appendText(text + "\n");
    }

    private void setScene(Parent root, double w, double h) {
        Scene scene = new Scene(root, w, h);
        scene.getStylesheets().add(getClass().getResource("/client.css").toExternalForm());
        stage.setScene(scene);
    }
}
