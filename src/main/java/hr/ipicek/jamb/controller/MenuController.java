package hr.ipicek.jamb.controller;

import hr.ipicek.jamb.model.GameEngine;
import hr.ipicek.jamb.util.DialogUtils;
import hr.ipicek.jamb.util.SaveLoadUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class MenuController {

    @FXML private Button btnNewGame;
    @FXML private Button btnLoadGame;
    @FXML private Button btnSettings;
    @FXML private Button btnExit;

    private List<String> playerNames = List.of("Igrač 1", "Igrač 2");
    private static final String ERROR_TITLE = "Greška";

    @FXML
    private void initialize() {
        btnNewGame.setOnAction(e -> startNewGame());
        btnLoadGame.setOnAction(e -> loadPreviousGame());
        btnSettings.setOnAction(e -> openSettings());
        btnExit.setOnAction(e -> System.exit(0));
    }

    private void startNewGame() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/mainView.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/style/scoreTable.css")).toExternalForm()
            );

            MainController controller = loader.getController();
            controller.init(new GameEngine(playerNames));

            Stage stage = (Stage) btnNewGame.getScene().getWindow();
            stage.setTitle("JAMB");
            stage.setScene(scene);
            stage.show();
        } catch (IOException ex) {
            DialogUtils.showError(ERROR_TITLE, "Nije moguće pokrenuti novu igru.", ex);
        }
    }

    private void openSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/settingsView.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/style/menu.css")).toExternalForm()
            );

            Stage dialog = new Stage();
            dialog.setTitle("Postavke");
            dialog.setScene(scene);
            dialog.initOwner(btnSettings.getScene().getWindow());

            SettingsController controller = loader.getController();
            controller.init(playerNames, newNames -> this.playerNames = newNames);

            dialog.showAndWait();
        } catch (IOException ex) {
            DialogUtils.showError(ERROR_TITLE, "Neuspjelo otvaranje postavki.", ex);
        }
    }

    private void loadPreviousGame() {
        Stage stage = (Stage) btnLoadGame.getScene().getWindow();
        GameEngine loaded = SaveLoadUtil.loadGameWithDialog(stage);

        if (loaded == null) {
            DialogUtils.showInfo("Učitavanje igre", "Nije učitana nijedna igra.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/mainView.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/style/scoreTable.css")).toExternalForm()
            );

            MainController controller = loader.getController();
            controller.init(loaded);

            stage.setTitle("JAMB - Učitana igra");
            stage.setScene(scene);
            stage.show();
        } catch (IOException ex) {
            DialogUtils.showError(ERROR_TITLE, "Neuspjelo učitavanje igre.", ex);
        }
    }
}