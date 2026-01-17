package hr.ipicek.jamb.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


// jedan potez unutar igre, serijaliziran za logger
public class Move implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public enum MoveType {
        ROLL_DICE("Bacio kockice"),
        SCORE_APPLIED("Upisao rezultat"),
        DICE_HELD("Držao kockice");

        private final String displayName;

        MoveType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final String playerName;
    private final MoveType type;
    private final ScoreCategory category;  // Null if not SCORE_APPLIED
    private final int score;               // 0 if not SCORE_APPLIED
    private final String diceValues;       // For ROLL_DICE
    private final LocalDateTime timestamp;

    // Constructor for ROLL_DICE
    public Move(String playerName, String diceValues) {
        this.playerName = playerName;
        this.type = MoveType.ROLL_DICE;
        this.category = null;
        this.score = 0;
        this.diceValues = diceValues;
        this.timestamp = LocalDateTime.now();
    }

    // Constructor for SCORE_APPLIED
    public Move(String playerName, ScoreCategory category, int score) {
        this.playerName = playerName;
        this.type = MoveType.SCORE_APPLIED;
        this.category = category;
        this.score = score;
        this.diceValues = null;
        this.timestamp = LocalDateTime.now();
    }

    // Constructor for DICE_HELD
    public Move(String playerName, int dieIndex, boolean held) {
        this.playerName = playerName;
        this.type = MoveType.DICE_HELD;
        this.category = null;
        this.score = 0;
        this.diceValues = "Kockica " + (dieIndex + 1) + ": " + (held ? "držana" : "puštena");
        this.timestamp = LocalDateTime.now();
    }

    // Getters
    public String getPlayerName() {
        return playerName;
    }

    public MoveType getType() {
        return type;
    }

    public ScoreCategory getCategory() {
        return category;
    }

    public int getScore() {
        return score;
    }

    public String getDiceValues() {
        return diceValues;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getFormattedTime() {
        return timestamp.format(TIME_FORMATTER);
    }


    public String getDescription() {
        return switch (type) {
            case ROLL_DICE -> "Bacio kockice: " + diceValues;
            case SCORE_APPLIED -> "Upisao u " + category.displayName() + " (" + score + " bodova)";
            case DICE_HELD -> diceValues;
        };
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s",
                getFormattedTime(),
                playerName,
                getDescription());
    }
}