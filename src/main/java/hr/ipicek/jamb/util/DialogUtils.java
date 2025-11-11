package hr.ipicek.jamb.util;

import hr.ipicek.jamb.model.GameEngine;
import hr.ipicek.jamb.model.ScoreCategory;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.util.Optional;

public class DialogUtils {

    public static void showScoreConfirmation(GameEngine engine, ScoreCategory category) {
        int potentialScore = engine.previewScore(category);
        int rolls = engine.rollCountProperty().get();

        var alert = getAlert(category, rolls, potentialScore);

        ButtonType yes = new ButtonType("Upiši", ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType("Odustani", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(yes, no);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == yes) {
            engine.applyScore(category);
        }
    }

    private static Alert getAlert(ScoreCategory category, int rolls, int potentialScore) {
        var alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Upis rezultata");
        alert.setHeaderText("Potvrda unosa");

        if (rolls < GameEngine.MAX_ROLLS) {
            alert.setContentText(
                    "Upisat ćeš " + potentialScore + " bodova u polje " + category.displayName() +
                            ".\nIskoristio si " + rolls + " od " + GameEngine.MAX_ROLLS + " bacanja.\n\nŽeliš li potvrditi unos?"
            );
        } else {
            alert.setContentText(
                    "Upisat ćeš " + potentialScore + " bodova u polje " + category.displayName() +
                            ".\nIskoristio si sva bacanja.\n\nŽeliš li potvrditi unos?"
            );
        }
        return alert;
    }
}