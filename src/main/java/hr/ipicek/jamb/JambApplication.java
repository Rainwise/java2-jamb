package hr.ipicek.jamb;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class JambApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/menuView.fxml"));
        Scene scene = new Scene(loader.load(), 400, 400);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/style/mainMenuStyle.css")).toExternalForm()
        );

        stage.setTitle("JAMB - Glavni izbornik");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}