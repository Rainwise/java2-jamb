package hr.ipicek.jamb.util;

public final class NetworkConstants {

    private NetworkConstants() {}

    // RMI Configuration
    public static final int RMI_REGISTRY_PORT = 1099;
    public static final String RMI_LOBBY_SERVICE = "LobbyService";
    public static final String RMI_CHAT_SERVICE = "ChatService";

    // Game Server Configuration
    public static final int DEFAULT_GAME_SERVER_PORT = 8888;
    public static final int SERVER_BACKLOG = 50;

    // Network Timeouts (milliseconds)
    public static final int CONNECTION_TIMEOUT = 5000;
    public static final int READ_TIMEOUT = 10000;

    // Game Configuration
    public static final int MAX_PLAYERS = 2;
    public static final int MIN_PLAYERS = 2;

    // Message Size Limits
    public static final int MAX_MESSAGE_SIZE = 65536; // 64KB
    public static final int BUFFER_SIZE = 8192; // 8KB

    // Retry Configuration
    public static final int MAX_RETRY_ATTEMPTS = 3;
    public static final int RETRY_DELAY_MS = 1000;
}