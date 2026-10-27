import com.chat.client.view.ClientApp;
import javafx.application.Application;

/**
 * Entry point:  java TCPClient <ServerIPAddress> <PortNumber>   (e.g. java TCPClient localhost 3000)
 * (Plain launcher class so the fat JAR works without the JavaFX module path.)
 */
public class TCPClient {
    public static void main(String[] args) {
        Application.launch(ClientApp.class, args);
    }
}
