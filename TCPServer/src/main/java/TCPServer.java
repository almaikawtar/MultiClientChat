import com.chat.server.view.ServerApp;
import javafx.application.Application;

/**
 * Entry point:  java TCPServer
 * (Plain launcher class so the fat JAR works without the JavaFX module path.)
 */
public class TCPServer {
    public static void main(String[] args) {
        Application.launch(ServerApp.class, args);
    }
}
