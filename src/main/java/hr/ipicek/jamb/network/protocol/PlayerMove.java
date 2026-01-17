package hr.ipicek.jamb.network.protocol;

import hr.ipicek.jamb.model.ScoreCategory;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

// Potez igrača, to se onda replicira na drugom client
// dice values koristim u trenutku poteza
// rollNumber si koristim da vidim broj bacanja
public record PlayerMove(String playerName, ScoreCategory category, int score, List<Integer> diceValues,
                         int rollNumber) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public PlayerMove(String playerName, ScoreCategory category, int score,
                      List<Integer> diceValues, int rollNumber) {
        this.playerName = playerName;
        this.category = category;
        this.score = score;
        this.diceValues = List.copyOf(diceValues); // Immutable kopija
        this.rollNumber = rollNumber;
    }

    // Getteri
    @Override
    public String toString() {
        return String.format("PlayerMove[player=%s, category=%s, score=%d, roll=%d, dice=%s]",
                playerName, category.displayName(), score, rollNumber, diceValues);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PlayerMove other)) return false;
        return playerName.equals(other.playerName) &&
                category == other.category &&
                score == other.score &&
                rollNumber == other.rollNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerName, category, score, rollNumber);
    }
}