package hr.ipicek.jamb.util;

import hr.ipicek.jamb.model.GameEngine;
import hr.ipicek.jamb.model.ScoreCategory;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Optional;

public final class DialogUtils {

    private DialogUtils() {}

    public static void showScoreConfirmation(GameEngine engine, ScoreCategory category) {
        if (showScoreConfirmation(category, engine.previewScore(category), engine.rollCountProperty().get())) {
            engine.applyScore(category);
        }
    }


    public static boolean showScoreConfirmation(ScoreCategory category, int potentialScore, int rolls) {
        var alert = getScoreAlert(category, rolls, potentialScore);
        ButtonType yes = new ButtonType("Upiši", ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType("Odustani", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(yes, no);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == yes;
    }

    private static Alert getScoreAlert(ScoreCategory category, int rolls, int potentialScore) {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Upis rezultata");
        alert.setHeaderText("Potvrda unosa");

        if (rolls < GameEngine.MAX_ROLLS) {
            alert.setContentText(String.format(
                    "Upisat ćeš %d bodova u polje %s.%nIskoristio si %d od %d bacanja.%n%nŽeliš li potvrditi unos?",
                    potentialScore, category.displayName(), rolls, GameEngine.MAX_ROLLS
            ));
        } else {
            alert.setContentText(String.format(
                    "Upisat ćeš %d bodova u polje %s.%nIskoristio si sva bacanja.%n%nŽeliš li potvrditi unos?",
                    potentialScore, category.displayName()
            ));
        }

        return alert;
    }

    public static void announceWinner(GameEngine engine) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Kraj igre!");
        alert.setHeaderText("Rezultati");

        var players = engine.getPlayers();
        var p1 = players.get(0);
        var p2 = players.get(1);

        int score1 = p1.getSheet().total();
        int score2 = p2.getSheet().total();

        if (score1 > score2)
            alert.setContentText(p1.getName() + " je pobijedio sa " + score1 + " bodova!");
        else if (score2 > score1)
            alert.setContentText(p2.getName() + " je pobijedio sa " + score2 + " bodova!");
        else
            alert.setContentText("Neriješeno! Oba igrača imaju " + score1 + " bodova.");

        alert.showAndWait();
    }

    public static File showSaveDialog(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Spremi igru kao...");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JAMB datoteke (*.jamb)", "*.jamb")
        );
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));

        File file = chooser.showSaveDialog(stage);
        if (file != null && !file.getName().toLowerCase().endsWith(".jamb")) {
            file = new File(file.getParentFile(), file.getName() + ".jamb");
        }
        return file;
    }

    public static File showLoadDialog(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Učitaj spremljenu igru");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JAMB datoteke (*.jamb)", "*.jamb")
        );
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        return chooser.showOpenDialog(stage);
    }


    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public static void showError(String title, String message, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Greška");
        alert.setContentText(message + (e != null ? ("\n\nDetalji: " + e.getMessage()) : ""));
        alert.showAndWait();
    }

    public static void showError(String title, String message) {
        showError(title, message, null);
    }

    public static void showError(String message) {
        showError("Greška", message, null);
    }

    public static boolean showConfirmation(String title, String message) {
        return showConfirmation(title, null, message);
    }

    public static boolean showConfirmation(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}