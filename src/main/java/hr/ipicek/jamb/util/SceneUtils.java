package hr.ipicek.jamb.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

// switchanje scena i fxml loading
public final class SceneUtils {

    private SceneUtils() {}

    // switch na novu scenu
    public static void switchScene(Node currentNode, String fxmlPath, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(SceneUtils.class.getResource(fxmlPath));
        Parent root = loader.load();

        Stage stage = (Stage) currentNode.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle(title);
    }

    // switch na novu scenu koja ima css klasu
    public static void switchSceneWithCSS(Node currentNode, String fxmlPath, String title, String cssPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(SceneUtils.class.getResource(fxmlPath));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                SceneUtils.class.getResource(cssPath).toExternalForm()
        );

        Stage stage = (Stage) currentNode.getScene().getWindow();
        stage.setScene(scene);
        stage.setTitle(title);
    }

    // load fxml i kontrolera
    public static FXMLLoader loadFXML(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(SceneUtils.class.getResource(fxmlPath));
        loader.load(); // Load the FXML
        return loader;
    }

    // load fxml i root parent
    public static Parent loadRoot(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(SceneUtils.class.getResource(fxmlPath));
        return loader.load();
    }
}