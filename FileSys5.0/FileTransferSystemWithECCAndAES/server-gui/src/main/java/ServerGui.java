import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ServerGui extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("Server");
        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("server_gui.fxml"));
        Scene home = new Scene(root);

        stage.setScene(home);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
