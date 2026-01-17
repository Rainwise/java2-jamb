package hr.ipicek.jamb.network;

import hr.ipicek.jamb.network.rmi.*;
import javafx.application.Platform;

import javax.naming.NamingException;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;


 // integrira RMI servise sa NetworkGameEngine.
 // Podržava i RMI Registry API i JNDI API za pristup servisima.

public class NetworkGameManager {

    private final RMIClient rmiClient;
    private final JNDIClient jndiClient;
    private final boolean useJNDI;
    private NetworkGameEngine gameEngine;
    private String gameId;
    private ChatListenerImpl chatListener;

    private Consumer<List<GameInfo>> onGamesListUpdated;
    private Consumer<ChatMessage> onChatMessageReceived;


     // Konstruktor sa RMI Registry API (default)
    public NetworkGameManager(String rmiHost, int rmiPort) {
        this(rmiHost, rmiPort, false);
    }


     // Konstruktor sa opcijom za JNDI ili RMI
    public NetworkGameManager(String rmiHost, int rmiPort, boolean useJNDI) {
        this.useJNDI = useJNDI;

        if (useJNDI) {
            this.jndiClient = new JNDIClient(rmiHost, rmiPort);
            this.rmiClient = null;
        } else {
            this.rmiClient = new RMIClient(rmiHost, rmiPort);
            this.jndiClient = null;
        }
    }

    public NetworkGameManager(String rmiHost) {
        this(rmiHost, RMIRegistryServer.DEFAULT_RMI_PORT);
    }


     // Povezuje se na RMI servise preko RMI Registry ili JNDI API
    public void connect() throws Exception {
        if (useJNDI) {
            jndiClient.connect();
        } else {
            rmiClient.connect();
        }
    }


     // Kreira novu igru kao host
    public NetworkGameEngine createGame(String hostName, int gamePort)
            throws Exception {

        // Kreiraj NetworkGameEngine kao host
        gameEngine = new NetworkGameEngine(hostName, gamePort);

        // Generiraj jedinstveni game ID
        gameId = UUID.randomUUID().toString();

        // Registriraj igru u RMI lobby
        String serverAddress = InetAddress.getLocalHost().getHostAddress();
        GameInfo gameInfo = new GameInfo(
                gameId,
                hostName,
                serverAddress,
                gamePort,
                1, // Trenutno 1 igrač (host)
                2, // Max 2 igrača
                GameInfo.GameStatus.WAITING
        );

        LobbyService lobby = getLobbyService();
        lobby.registerGame(gameInfo);

        System.out.println("[NetworkGameManager] Igra kreirana i registrirana: " + gameId);

        // Setup chat listener
        setupChatListener(hostName);

        // Callback za update statusa
        gameEngine.playersInLobbyProperty().addListener((obs, oldVal, newVal) -> {
            try {
                GameInfo.GameStatus status = newVal.intValue() >= 2 ?
                        GameInfo.GameStatus.FULL : GameInfo.GameStatus.WAITING;
                lobby.updateGameStatus(gameId, newVal.intValue(), status);
            } catch (RemoteException e) {
                System.err.println("[NetworkGameManager] Greška pri update game status: " + e.getMessage());
            }
        });

        // Callback za game start
        gameEngine.setOnGameStartCallback(() -> {
            try {
                lobby.updateGameStatus(gameId, 2, GameInfo.GameStatus.IN_PROGRESS);
            } catch (RemoteException e) {
                System.err.println("[NetworkGameManager] Greška pri update game status: " + e.getMessage());
            }
        });

        // Callback za game over
        gameEngine.setOnGameOverCallback(() -> {
            try {
                lobby.updateGameStatus(gameId, 2, GameInfo.GameStatus.FINISHED);
            } catch (RemoteException e) {
                System.err.println("[NetworkGameManager] Greška pri update game status: " + e.getMessage());
            }
        });

        return gameEngine;
    }


     // Pridružuje se postojećoj igri
    public NetworkGameEngine joinGame(GameInfo gameInfo, String playerName)
            throws Exception {

        // Provjeri može li se pridružiti
        LobbyService lobby = getLobbyService();
        if (!lobby.joinGame(gameInfo.getGameId(), playerName)) {
            throw new IllegalStateException("Ne može se pridružiti igri");
        }

        this.gameId = gameInfo.getGameId();

        // Kreiraj NetworkGameEngine kao client
        gameEngine = new NetworkGameEngine(
                playerName,
                gameInfo.getServerAddress(),
                gameInfo.getServerPort()
        );

        System.out.println("[NetworkGameManager] Pridružen igri: " + gameId);

        // Setup chat listener
        setupChatListener(playerName);

        return gameEngine;
    }


    public List<GameInfo> getAvailableGames() throws Exception {
        LobbyService lobby = getLobbyService();
        return lobby.getAvailableGames();
    }


    public void sendChatMessage(String message) throws Exception {
        if (gameId == null) {
            throw new IllegalStateException("Nije u igri");
        }

        ChatService chat = getChatService();
        String playerName = gameEngine != null ? gameEngine.getLocalPlayerName() : "Unknown";
        ChatMessage chatMsg = ChatMessage.regular(playerName, message);
        chat.sendMessage(gameId, chatMsg);
    }

    public List<ChatMessage> getChatMessages() throws Exception {
        if (gameId == null) {
            throw new IllegalStateException("Nije u igri");
        }

        ChatService chat = getChatService();
        return chat.getMessages(gameId);
    }


    private void setupChatListener(String playerName) throws Exception {
        ChatService chat = getChatService();

        chatListener = new ChatListenerImpl(message -> {
            if (onChatMessageReceived != null) {
                Platform.runLater(() -> onChatMessageReceived.accept(message));
            }
        });

        chat.registerChatListener(gameId, playerName, chatListener);
    }

    public void shutdown() {
        if (gameEngine != null) {
            gameEngine.shutdown();
        }

        if (chatListener != null && gameId != null) {
            try {
                String playerName = gameEngine != null ? gameEngine.getLocalPlayerName() : "Unknown";
                ChatService chat = getChatService();
                chat.unregisterChatListener(gameId, playerName);
            } catch (Exception e) {
                System.err.println("[NetworkGameManager] Greška pri chat unregister: " + e.getMessage());
            }
        }

        if (gameId != null) {
            try {
                LobbyService lobby = getLobbyService();
                lobby.removeGame(gameId);
            } catch (Exception e) {
                System.err.println("[NetworkGameManager] Greška pri remove game: " + e.getMessage());
            }
        }

        rmiClient.disconnect();
    }

    // Getteri i setteri
    public NetworkGameEngine getGameEngine() {
        return gameEngine;
    }

    public String getGameId() {
        return gameId;
    }

    public void setOnGamesListUpdated(Consumer<List<GameInfo>> callback) {
        this.onGamesListUpdated = callback;
    }

    public void setOnChatMessageReceived(Consumer<ChatMessage> callback) {
        this.onChatMessageReceived = callback;
    }

    public boolean isConnected() {
        if (useJNDI) {
            return jndiClient.testConnection();
        } else {
            return rmiClient.testConnection();
        }
    }


     // Helper metoda za dobijanje LobbyService preko RMI ili JNDI
    private LobbyService getLobbyService() throws Exception {
        if (useJNDI) {
            return jndiClient.getLobbyService();
        } else {
            return getLobbyService();
        }
    }


     // Helper metoda za dobijanje ChatService preko RMI ili JNDI
    private ChatService getChatService() throws Exception {
        if (useJNDI) {
            return jndiClient.getChatService();
        } else {
            return getChatService();
        }
    }


     // Implementacija ChatListener-a za primanje poruka
    private static class ChatListenerImpl extends UnicastRemoteObject implements ChatListener {

        private final Consumer<ChatMessage> callback;

        protected ChatListenerImpl(Consumer<ChatMessage> callback) throws RemoteException {
            super();
            this.callback = callback;
        }

        @Override
        public void onMessageReceived(ChatMessage message) throws RemoteException {
            if (callback != null) {
                callback.accept(message);
            }
        }
    }
}