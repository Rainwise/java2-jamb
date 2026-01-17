package hr.ipicek.jamb.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.Serial;
import java.io.Serializable;

public class Player {

    private final StringProperty name;
    private final ScoreSheet sheet;

    public Player(String name) {
        this.name = new SimpleStringProperty(name);
        this.sheet = new ScoreSheet();
    }

    public String getName() {
        return name.get();
    }

    public ScoreSheet getSheet() {
        return sheet;
    }

    public PlayerState toSerializableState() {
        return new PlayerState(name.get(), sheet.toSerializableState());
    }

    public static Player fromSerializableState(PlayerState state) {
        Player p = new Player(state.name);
        p.getSheet().loadFromSerializableState(state.sheetState);
        return p;
    }

    public record PlayerState(String name, ScoreSheet.SheetState sheetState) implements Serializable {
            @Serial
            private static final long serialVersionUID = 1L;

    }

    @Override
    public String toString() {
        return name.get() + " (" + sheet.total() + " pts)";
    }
}