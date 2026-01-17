package hr.ipicek.jamb.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.io.Serial;
import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

public class ScoreSheet {

    private final EnumMap<ScoreCategory, IntegerProperty> scores = new EnumMap<>(ScoreCategory.class);
    private final EnumMap<ScoreCategory, BooleanProperty> filled = new EnumMap<>(ScoreCategory.class);

    public ScoreSheet() {
        for (ScoreCategory c : ScoreCategory.values()) {
            scores.put(c, new SimpleIntegerProperty(0));
            filled.put(c, new SimpleBooleanProperty(false));
        }
    }

    public IntegerProperty scoreProperty(ScoreCategory category) {
        return scores.get(category);
    }

    public BooleanProperty filledProperty(ScoreCategory category) {
        return filled.get(category);
    }

    public int getScore(ScoreCategory category) {
        return scores.get(category).get();
    }

    public boolean isFilled(ScoreCategory category) {
        return filled.get(category).get();
    }

    public void setScore(ScoreCategory category, int value) {
        scores.get(category).set(value);
        filled.get(category).set(true);
    }

    public boolean isFull() {
        for (ScoreCategory c : ScoreCategory.values()) {
            if (!filledProperty(c).get()) return false;
        }
        return true;
    }

    public int total() {
        return scores.values().stream()
                .mapToInt(IntegerProperty::get)
                .sum();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ScoreSheet:\n");
        for (var c : ScoreCategory.values()) {
            sb.append(String.format("  %-15s : %3d %s%n",
                    c.name(), getScore(c), isFilled(c) ? "(✓)" : ""));
        }
        sb.append("Total: ").append(total());
        return sb.toString();
    }


    public SheetState toSerializableState() {
        var state = new EnumMap<ScoreCategory, SheetState.CategoryState>(ScoreCategory.class);
        for (var c : ScoreCategory.values()) {
            state.put(c, new SheetState.CategoryState(getScore(c), isFilled(c)));
        }
        return new SheetState(state);
    }

    public void loadFromSerializableState(SheetState saved) {
        for (var c : ScoreCategory.values()) {
            var catState = saved.categories.get(c);
            if (catState != null) {
                scores.get(c).set(catState.score);
                filled.get(c).set(catState.filled);
            }
        }
    }

    public record SheetState(Map<ScoreCategory, CategoryState> categories) implements Serializable {
            @Serial
            private static final long serialVersionUID = 1L;

        public record CategoryState(int score, boolean filled) implements Serializable {
            @Serial
            private static final long serialVersionUID = 1L;
            }
        }
}