package hr.ipicek.jamb.controller;

import hr.ipicek.jamb.model.GameEngine;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class MenuController {

    @FXML private Button btnNewGame;
    @FXML private Button btnLoadGame;
    @FXML private Button btnExit;

    @FXML
    private void initialize() {
        btnNewGame.setOnAction(e -> startNewGame());
        btnLoadGame.setOnAction(e -> loadPreviousGame());
        btnExit.setOnAction(e -> System.exit(0));
    }

    private void startNewGame() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/mainView.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    getClass().getResource("/style/scoreTable.css").toExternalForm()
            );

            MainController controller = loader.getController();
            controller.init(new GameEngine(List.of("Igrač 1", "Igrač 2")));

            Stage stage = (Stage) btnNewGame.getScene().getWindow();
            stage.setTitle("JAMB");
            stage.setScene(scene);
            stage.show();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void loadPreviousGame() {
    }
}