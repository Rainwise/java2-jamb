package hr.ipicek.jamb.network.rmi;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// poruka preko RMI clienta
public class ChatMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String sender;
    private final String message;
    private final LocalDateTime timestamp;
    private final MessageType type;

    public enum MessageType {
        REGULAR,        // Obična poruka
        SYSTEM,         // System poruka
        PRIVATE,        // Privatna poruka
        GAME_EVENT      // Notifikacija o game eventu
    }

    public ChatMessage(String sender, String message, MessageType type) {
        this.sender = sender;
        this.message = message;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }

    public ChatMessage(String sender, String message) {
        this(sender, message, MessageType.REGULAR);
    }

    // Static factory metode
    public static ChatMessage regular(String sender, String message) {
        return new ChatMessage(sender, message, MessageType.REGULAR);
    }

    public static ChatMessage system(String message) {
        return new ChatMessage("SYSTEM", message, MessageType.SYSTEM);
    }

    public static ChatMessage gameEvent(String message) {
        return new ChatMessage("GAME", message, MessageType.GAME_EVENT);
    }

    // Getteri
    public String getSender() {
        return sender;
    }
    public String getMessage() {
        return message;
    }
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    public MessageType getType() {
        return type;
    }

    // Formatiranje
    public String getFormattedTime() {
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
    public String getFormattedMessage() {
        return String.format("[%s] %s: %s", getFormattedTime(), sender, message);
    }
    @Override
    public String toString() {
        return getFormattedMessage();
    }
}