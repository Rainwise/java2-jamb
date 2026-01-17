package hr.ipicek.jamb.network.protocol;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;


// Objekt za poruke, serijaliziran zbog slanja za TCP/UDP socket
public class GameMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final MessageType type;
    private final String senderName;
    // tu si pospremam sve (playemove, string za chat, gameState)
    private final Object payload;
    private final LocalDateTime timestamp;

    public GameMessage(MessageType type, String senderName, Object payload) {
        this.type = type;
        this.senderName = senderName;
        this.payload = payload;
        this.timestamp = LocalDateTime.now();
    }

    // Static factory metode za kreaciju poruka
    public static GameMessage playerJoined(String playerName) {
        return new GameMessage(MessageType.PLAYER_JOINED, playerName, null);
    }

    public static GameMessage gameStart() {
        return new GameMessage(MessageType.GAME_START, "SYSTEM", null);
    }

    public static GameMessage gameStateUpdate(GameStateUpdate state) {
        return new GameMessage(MessageType.GAME_STATE_UPDATE, "SYSTEM", state);
    }

    public static GameMessage diceRolled(String playerName, int[] diceValues) {
        return new GameMessage(MessageType.DICE_ROLLED, playerName, diceValues);
    }

    public static GameMessage diceHeld(String playerName, int dieIndex, boolean held) {
        return new GameMessage(MessageType.DICE_HOLD_TOGGLE, playerName,
                new DiceHoldData(dieIndex, held));
    }

    public static GameMessage scoreApplied(String playerName, PlayerMove move) {
        return new GameMessage(MessageType.SCORE_APPLIED, playerName, move);
    }

    public static GameMessage turnChange(String nextPlayerName) {
        return new GameMessage(MessageType.TURN_CHANGE, "SYSTEM", nextPlayerName);
    }

    public static GameMessage chatMessage(String senderName, String message) {
        return new GameMessage(MessageType.CHAT_MESSAGE, senderName, message);
    }

    public static GameMessage gameOver(String winnerName, int score) {
        return new GameMessage(MessageType.GAME_OVER, "SYSTEM",
                new GameOverData(winnerName, score));
    }

    public static GameMessage disconnect(String playerName) {
        return new GameMessage(MessageType.DISCONNECT, playerName, null);
    }

    public static GameMessage error(String errorMessage) {
        return new GameMessage(MessageType.ERROR, "SYSTEM", errorMessage);
    }

    // Getteri
    public MessageType getType() {
        return type;
    }
    public String getSenderName() {
        return senderName;
    }
    public Object getPayload() {
        return payload;
    }
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    // Helper metode za type-safe pristup payload-u
    @SuppressWarnings("unchecked")
    public <T> T getPayloadAs(Class<T> clazz) {
        if (clazz.isInstance(payload)) {
            return (T) payload;
        }
        return null;
    }

    public String getPayloadAsString() {
        return payload != null ? payload.toString() : "";
    }

    @Override
    public String toString() {
        return String.format("GameMessage[type=%s, sender=%s, time=%s, payload=%s]",
                type, senderName, timestamp, payload);
    }

    // Inner klase za specifične payloade
    public record DiceHoldData(int dieIndex, boolean held) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }

    public record GameOverData(String winnerName, int score) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }
}