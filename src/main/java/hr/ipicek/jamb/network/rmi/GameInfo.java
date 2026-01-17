package hr.ipicek.jamb.network.rmi;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;


// Informacije koje se vide u lobbyu
public class GameInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String gameId;
    private final String hostName;
    private final String serverAddress;
    private final int serverPort;
    private final int currentPlayers;
    private final int maxPlayers;
    private final LocalDateTime createdAt;
    private final GameStatus status;

    public enum GameStatus {
        WAITING,    // Čeka igrače
        FULL,       // Puna
        IN_PROGRESS, // Igra u tijeku
        FINISHED    // Završena
    }

    public GameInfo(String gameId, String hostName, String serverAddress,
                    int serverPort, int currentPlayers, int maxPlayers, GameStatus status) {
        this.gameId = gameId;
        this.hostName = hostName;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.currentPlayers = currentPlayers;
        this.maxPlayers = maxPlayers;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    public String getGameId() {
        return gameId;
    }
    public String getHostName() {
        return hostName;
    }
    public String getServerAddress() {
        return serverAddress;
    }
    public int getServerPort() {
        return serverPort;
    }
    public int getCurrentPlayers() {
        return currentPlayers;
    }
    public int getMaxPlayers() {
        return maxPlayers;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public GameStatus getStatus() {
        return status;
    }

    // Helperi
    public boolean isFull() {
        return currentPlayers >= maxPlayers || status == GameStatus.FULL;
    }

    public boolean isJoinable() {
        return status == GameStatus.WAITING && !isFull();
    }

    public String getDisplayName() {
        return hostName + "'s game (" + currentPlayers + "/" + maxPlayers + ")";
    }

    @Override
    public String toString() {
        return String.format("GameInfo[id=%s, host=%s, address=%s:%d, players=%d/%d, status=%s]",
                gameId, hostName, serverAddress, serverPort, currentPlayers, maxPlayers, status);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof GameInfo other)) return false;
        return gameId.equals(other.gameId);
    }

    @Override
    public int hashCode() {
        return gameId.hashCode();
    }
}