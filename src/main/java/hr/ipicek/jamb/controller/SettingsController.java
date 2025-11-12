package hr.ipicek.jamb.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.Consumer;

public class SettingsController {

    @FXML private TextField txtPlayer1;
    @FXML private TextField txtPlayer2;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    private Consumer<List<String>> onSaveCallback;

    @FXML
    private void initialize() {
        btnSave.setOnAction(e -> saveSettings());
        btnCancel.setOnAction(e -> closeWindow());
    }

    public void init(List<String> defaultNames, Consumer<List<String>> onSave) {
        txtPlayer1.setText(defaultNames.get(0));
        txtPlayer2.setText(defaultNames.get(1));
        this.onSaveCallback = onSave;
    }

    private void saveSettings() {
        List<String> names = List.of(
                txtPlayer1.getText().isBlank() ? "Igrač 1" : txtPlayer1.getText(),
                txtPlayer2.getText().isBlank() ? "Igrač 2" : txtPlayer2.getText()
        );

        if (onSaveCallback != null)
            onSaveCallback.accept(names);

        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }
}