package hr.ipicek.jamb.util;

import hr.ipicek.jamb.model.GameEngine;
import javafx.stage.Stage;

import java.io.*;

public final class SaveLoadUtil {

    private SaveLoadUtil() {
    }

    private static final String DEFAULT_SAVE_EXTENSION = ".jamb";
    public static void saveGameWithDialog(GameEngine engine, Stage stage) {
        File file = DialogUtils.showSaveDialog(stage);
        if (file == null) return; // user cancelled

        if (!file.getName().toLowerCase().endsWith(DEFAULT_SAVE_EXTENSION)) {
            file = new File(file.getAbsolutePath() + DEFAULT_SAVE_EXTENSION);
        }

        saveGame(engine, file);
    }

    public static void saveGame(GameEngine engine, File file) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(engine.toSerializableState());
            DialogUtils.showInfo("Spremanje igre", "Igra je uspješno spremljena:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            DialogUtils.showError("Greška pri spremanju igre", "Nije moguće spremiti igru.", e);
        }
    }

    public static GameEngine loadGameWithDialog(Stage stage) {
        File file = DialogUtils.showLoadDialog(stage);
        if (file == null) return null;

        return loadGame(file);
    }

    public static GameEngine loadGame(File file) {
        if (file == null || !file.exists()) {
            DialogUtils.showError("Učitavanje igre", "Odabrana datoteka ne postoji.", null);
            return null;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            GameEngine.GameEngineState state = (GameEngine.GameEngineState) ois.readObject();
            DialogUtils.showInfo("Učitavanje igre", "Igra je uspješno učitana:\n" + file.getName());
            return GameEngine.fromSerializableState(state);
        } catch (Exception e) {
            DialogUtils.showError("Greška pri učitavanju", "Neuspješno učitavanje igre.", e);
            return null;
        }
    }
}