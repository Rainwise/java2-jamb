package hr.ipicek.jamb.network.protocol;

import hr.ipicek.jamb.model.ScoreCategory;

import java.io.Serializable;
import java.util.List;
import java.util.Map;


// Game state za broadcast prema svim klijentima (Update za UI)
public record GameStateUpdate(List<Integer> diceValues, List<Boolean> diceHeld, int rollCount, int currentPlayerIndex,
                              String currentPlayerName, Map<String, Map<ScoreCategory, Integer>> scoreSheets,
                              Map<String, Integer> totalScores, boolean gameOver,
                              String winnerName) implements Serializable {


    @Override
    public String toString() {
        return "GameStateUpdate{" +
                "rollCount=" + rollCount +
                ", currentPlayer=" + currentPlayerName +
                ", diceValues=" + diceValues +
                ", gameOver=" + gameOver +
                '}';
    }
}