package hr.ipicek.jamb.model;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;

public class Player {

    private final StringProperty name = new SimpleStringProperty();
    private final ScoreSheet sheet = new ScoreSheet();

    // Computed property (binds to sheet total)
    private final ReadOnlyIntegerWrapper totalScore = new ReadOnlyIntegerWrapper();

    public Player(String name) {
        this.name.set(name);

        // Bind totalScore dynamically to sheet total
        totalScore.bind(Bindings.createIntegerBinding(
                sheet::total,
                sheet.scoreProperty(ScoreCategory.ONES),
                sheet.scoreProperty(ScoreCategory.TWOS),
                sheet.scoreProperty(ScoreCategory.THREES),
                sheet.scoreProperty(ScoreCategory.FOURS),
                sheet.scoreProperty(ScoreCategory.FIVES),
                sheet.scoreProperty(ScoreCategory.SIXES),
                sheet.scoreProperty(ScoreCategory.THREE_OF_A_KIND),
                sheet.scoreProperty(ScoreCategory.FOUR_OF_A_KIND),
                sheet.scoreProperty(ScoreCategory.FULL_HOUSE),
                sheet.scoreProperty(ScoreCategory.SMALL_STRAIGHT),
                sheet.scoreProperty(ScoreCategory.LARGE_STRAIGHT),
                sheet.scoreProperty(ScoreCategory.YAHTZEE),
                sheet.scoreProperty(ScoreCategory.CHANCE)
        ));
    }

    public String getName() {
        return name.get();
    }

    public ScoreSheet getSheet() {
        return sheet;
    }

    public int getTotalScore() {
        return totalScore.get();
    }

    @Override
    public String toString() {
        return getName() + " (" + getTotalScore() + " pts)";
    }
}