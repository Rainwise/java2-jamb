package hr.ipicek.jamb.model;

import hr.ipicek.jamb.util.ScoreCalculator;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

public class GameEngine {

    public static final int MAX_ROLLS = 3;
    private static final int NUM_PLAYERS = 2;

    private final List<Player> players;
    private final DiceSet diceSet = new DiceSet();

    private final IntegerProperty rollCount = new SimpleIntegerProperty(0);
    private final IntegerProperty currentPlayerIndex = new SimpleIntegerProperty(0);
    private final BooleanProperty gameOver = new SimpleBooleanProperty(false);

    private final ObservableList<StringProperty> diceImagePaths = FXCollections.observableArrayList();

    public GameEngine(List<String> playerNames) {
        if (playerNames == null || playerNames.size() != NUM_PLAYERS)
            throw new IllegalArgumentException("Igra mora imati točno 2 igrača.");

        this.players = new ArrayList<>();
        playerNames.forEach(name -> players.add(new Player(name)));

        initDiceImageBindings();
        nextTurn();
    }

    private GameEngine(List<Player> restoredPlayers, List<Integer> diceValues, int rollCount, int currentPlayer) {
        this.players = restoredPlayers;
        initDiceImageBindings();

        for (int i = 0; i < diceSet.getDice().size(); i++) {
            diceSet.getDice().get(i).setValue(diceValues.get(i));
        }

        this.rollCount.set(rollCount);
        this.currentPlayerIndex.set(currentPlayer);
    }

    private void initDiceImageBindings() {
        for (Die die : diceSet.getDice()) {
            StringProperty imagePath = new SimpleStringProperty(getDiceImagePath(die.getValue()));
            die.valueProperty().addListener((obs, oldVal, newVal) ->
                    imagePath.set(getDiceImagePath(newVal.intValue()))
            );
            diceImagePaths.add(imagePath);
        }
    }

    private String getDiceImagePath(int value) {
        return "/images/dice" + value + ".png";
    }

    public ObservableList<StringProperty> getDiceImagePaths() {
        return diceImagePaths;
    }

    public void roll() {
        if (rollCount.get() >= MAX_ROLLS) return;
        diceSet.roll();
        rollCount.set(rollCount.get() + 1);
    }


    private boolean isGameFinished() {
        for (Player p : players) {
            if (!p.getSheet().isFull()) return false;
        }
        return true;
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

        if (isGameFinished()) {
            gameOver.set(true);
            return;
        }

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

    public List<Player> getPlayers() {
        return players;
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex.get());
    }

    public DiceSet getDiceSet() {
        return diceSet;
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex.get();
    }

    public IntegerProperty currentPlayerIndexProperty() {
        return currentPlayerIndex;
    }

    public IntegerProperty rollCountProperty() {
        return rollCount;
    }

    public BooleanProperty gameOverProperty() {
        return gameOver;
    }

    public GameEngineState toSerializableState() {
        List<Player.PlayerState> playerStates = new ArrayList<>();
        for (Player p : players)
            playerStates.add(p.toSerializableState());

        List<Integer> diceValues = diceSet.getDiceValues();

        return new GameEngineState(
                playerStates,
                diceValues,
                rollCount.get(),
                currentPlayerIndex.get()
        );
    }

    public static GameEngine fromSerializableState(GameEngineState state) {
        List<Player> restoredPlayers = new ArrayList<>();
        for (Player.PlayerState ps : state.playerStates)
            restoredPlayers.add(Player.fromSerializableState(ps));

        return new GameEngine(
                restoredPlayers,
                state.diceValues,
                state.rollCount,
                state.currentPlayerIndex
        );
    }

    public record GameEngineState(List<Player.PlayerState> playerStates, List<Integer> diceValues, int rollCount, int currentPlayerIndex) implements Serializable {
            @Serial
            private static final long serialVersionUID = 1L;
    }
}