package hr.ipicek.jamb.model;

import javafx.beans.property.*;
import java.util.EnumMap;

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

    // For restart
    public void reset() {
        for (ScoreCategory c : ScoreCategory.values()) {
            scores.get(c).set(0);
            filled.get(c).set(false);
        }
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
            sb.append(String.format("  %-15s : %3d %s%n", c.name(), getScore(c), isFilled(c) ? "(✓)" : ""));
        }
        sb.append("Total: ").append(total());
        return sb.toString();
    }
}