package hr.ipicek.jamb.model;

import hr.ipicek.jamb.util.ScoreCalculator;
import javafx.beans.property.*;
import java.util.*;

public class GameEngine {

    public static final int MAX_ROLLS = 3;
    private static final int NUM_PLAYERS = 2;

    private final List<Player> players;
    private final DiceSet diceSet = new DiceSet();

    private final IntegerProperty rollCount = new SimpleIntegerProperty(0);
    private final IntegerProperty currentPlayerIndex = new SimpleIntegerProperty(0);

    public GameEngine(List<String> playerNames) {
        if (playerNames == null || playerNames.size() != NUM_PLAYERS)
            throw new IllegalArgumentException("Igra mora imati 2 igrača.");

        players = new ArrayList<>();
        for (String name : playerNames) players.add(new Player(name));

        nextTurn();
    }

    public void roll() {
        if (rollCount.get() >= MAX_ROLLS) return;
        diceSet.roll();
        rollCount.set(rollCount.get() + 1);
    }

    public int previewScore(ScoreCategory category) {
        return ScoreCalculator.calculate(category, diceSet);
    }

    public void applyScore(ScoreCategory category) {
        Player current = getCurrentPlayer();
        ScoreSheet sheet = current.getSheet();

        if (sheet.filledProperty(category).get()) return;

        int score = previewScore(category);
        sheet.setScore(category, score);

        nextPlayer();
    }

    public void nextPlayer() {
        int next = (currentPlayerIndex.get() + 1) % NUM_PLAYERS;
        currentPlayerIndex.set(next);
        nextTurn();
    }

    private void nextTurn() {
        rollCount.set(0);
        diceSet.resetHolds();
        roll();
    }

    public List<Player> getPlayers() { return players; }

    public Player getCurrentPlayer() { return players.get(currentPlayerIndex.get()); }

    public DiceSet getDiceSet() { return diceSet; }

    public int getCurrentPlayerIndex() { return currentPlayerIndex.get(); }

    public IntegerProperty currentPlayerIndexProperty() { return currentPlayerIndex; }

    public IntegerProperty rollCountProperty() { return rollCount; }
}