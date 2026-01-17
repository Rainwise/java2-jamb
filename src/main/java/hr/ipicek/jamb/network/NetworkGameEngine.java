package hr.ipicek.jamb.network;

import hr.ipicek.jamb.logging.MoveDisplay;
import hr.ipicek.jamb.logging.MoveLogger;
import hr.ipicek.jamb.model.*;
import hr.ipicek.jamb.network.protocol.GameMessage;
import hr.ipicek.jamb.network.protocol.GameStateUpdate;
import hr.ipicek.jamb.network.protocol.MessageType;
import hr.ipicek.jamb.network.protocol.PlayerMove;
import hr.ipicek.jamb.network.socket.GameClient;
import hr.ipicek.jamb.network.socket.GameServer;
import hr.ipicek.jamb.util.Logger;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;


 // Wrapper oko GameEngine-a koji dodaje mrežnu funkcionalnost. Upravlja sinkronizacijom stanja igre preko mreže.
public class NetworkGameEngine {

    private GameEngine gameEngine;
    private final boolean isHost;
    private final String localPlayerName;

    // Network komponente
    private GameServer server;
    private GameClient client;

    // Move logging komponente
    private MoveLogger moveLogger;
    private MoveDisplay moveDisplay;
    private String gameId;

    // Properties za UI binding
    private final BooleanProperty connected = new SimpleBooleanProperty(false);
    private final BooleanProperty gameStarted = new SimpleBooleanProperty(false);
    private final StringProperty statusMessage = new SimpleStringProperty("");
    private final IntegerProperty playersInLobby = new SimpleIntegerProperty(0);

    // Client-side observables (za klijente koji nemaju GameEngine)
    private final ObservableList<Integer> clientDiceValues = FXCollections.observableArrayList(0, 0, 0, 0, 0);
    private final ObservableList<Boolean> clientDiceHeld = FXCollections.observableArrayList(false, false, false, false, false);
    private final IntegerProperty clientRollCount = new SimpleIntegerProperty(0);
    private final IntegerProperty clientCurrentPlayerIndex = new SimpleIntegerProperty(0);
    private final StringProperty clientCurrentPlayerName = new SimpleStringProperty("");
    private final ObservableMap<String, Integer> clientTotalScores = FXCollections.observableHashMap();

    // Callbacks
    private Consumer<String> chatMessageCallback;
    private Runnable onGameStartCallback;
    private Runnable onGameOverCallback;
    private Runnable onTurnChangeCallback;

    // Kontrola toka igre
    private final BooleanProperty isLocalPlayerTurn = new SimpleBooleanProperty(false);
    private final List<String> playerNames = new ArrayList<>();

   // host
    public NetworkGameEngine(String localPlayerName, int port) {
        this.isHost = true;
        this.localPlayerName = localPlayerName;
        this.playerNames.add(localPlayerName);
        initializeAsHost(port);
    }

    // client
    public NetworkGameEngine(String localPlayerName, String serverHost, int serverPort) {
        this.isHost = false;
        this.localPlayerName = localPlayerName;
        this.playerNames.add(localPlayerName);
        initializeAsClient(localPlayerName, serverHost, serverPort);
    }


    private void initializeAsHost(int port) {
        server = new GameServer(port);

        server.setStatusCallback(status -> {
            Platform.runLater(() -> statusMessage.set(status));
        });

        server.setMessageCallback(this::handleMessage);

        server.start();
        connected.set(true);
        playersInLobby.set(1); // Host je već u lobby-u

        updateStatus("Server pokrenut. Čekanje igrača...");
    }

    private void initializeAsClient(String playerName, String host, int port) {
        client = new GameClient(playerName, host, port);

        client.setStatusCallback(status -> {
            Platform.runLater(() -> statusMessage.set(status));
        });

        client.setMessageCallback(this::handleMessage);

        client.setOnConnectedCallback(() -> {
            Platform.runLater(() -> {
                connected.set(true);
                updateStatus("Povezan na server!");
            });
        });

        client.setOnDisconnectedCallback(() -> {
            Platform.runLater(() -> {
                connected.set(false);
                updateStatus("Odspojen sa servera");
            });
        });

        client.start();
    }


    private void handleMessage(GameMessage message) {
        Platform.runLater(() -> {
            switch (message.getType()) {
                case PLAYER_JOINED -> handlePlayerJoined(message);
                case GAME_START -> handleGameStart(message);
                case GAME_STATE_UPDATE -> handleGameStateUpdate(message);
                case ROLL_REQUEST -> handleRollRequest(message);
                case DICE_ROLLED -> handleDiceRolled(message);
                case DICE_HOLD_TOGGLE -> handleDiceHoldToggle(message);
                case SCORE_APPLY_REQUEST -> handleScoreApplyRequest(message);
                case SCORE_APPLIED -> handleScoreApplied(message);
                case TURN_CHANGE -> handleTurnChange(message);
                case GAME_OVER -> handleGameOver(message);
                case CHAT_MESSAGE -> handleChatMessage(message);
                case DISCONNECT -> handleDisconnect(message);
                default -> Logger.Game.warn("Nepoznat tip poruke: " + message.getType());
            }
        });
    }

    private void handlePlayerJoined(GameMessage message) {
        String playerName = message.getSenderName();
        Logger.Game.debug("handlePlayerJoined: " + playerName + ", trenutna lista: " + playerNames);

        if (!playerNames.contains(playerName)) {
            playerNames.add(playerName);
            playersInLobby.set(playerNames.size());
            updateStatus("Igrač " + playerName + " se pridružio");

            Logger.Game.debug("Dodao igrača. Nova lista: " + playerNames);

            // Ako sam host i novi igrač se pridružio, pošalji mu moju PLAYER_JOINED poruku
            // (jer host nema klijenta koji bi automatski poslao to)
            if (isHost && !playerName.equals(localPlayerName)) {
                broadcastMessage(GameMessage.playerJoined(localPlayerName));
            }

            // Ako je host i ima 2 igrača, pokreni igru
            if (isHost && playerNames.size() == 2) {
                startGame();
            }
        }
    }

    private void handleGameStart(GameMessage message) {
        if (!gameStarted.get()) {
            Logger.Game.debug("handleGameStart pozvan. playerNames: " + playerNames + ", size: " + playerNames.size());

            gameStarted.set(true);

            // Dohvati redoslijed igrača iz poruke (server odlučuje redoslijed)
            @SuppressWarnings("unchecked")
            List<String> orderedPlayerNames = message.getPayloadAs(List.class);

            if (orderedPlayerNames != null && orderedPlayerNames.size() == 2) {
                // Koristi redoslijed koji je server poslao
                Logger.Game.debug("Koristim server redoslijed: " + orderedPlayerNames);
                gameEngine = new GameEngine(new ArrayList<>(orderedPlayerNames));
            } else {
                // Fallback na lokalnu listu
                Logger.Game.debug("Fallback na lokalnu listu: " + playerNames);
                gameEngine = new GameEngine(new ArrayList<>(playerNames));
            }

            // Inicijaliziraj move logging (samo za HOST)
            if (isHost) {
                initializeMoveLogging();
            }

            // Provjeri čiji je turn
            isLocalPlayerTurn.set(gameEngine.getCurrentPlayer().getName().equals(localPlayerName));

            Logger.Game.debug("Prvi igrač: " + gameEngine.getCurrentPlayer().getName() +
                    ", isLocalPlayerTurn: " + isLocalPlayerTurn + " (localPlayerName: " + localPlayerName + ")");

            updateStatus("Igra je počela!");

            if (onGameStartCallback != null) {
                onGameStartCallback.run();
            }
        }
    }

    private void handleGameStateUpdate(GameMessage message) {
        GameStateUpdate state = message.getPayloadAs(GameStateUpdate.class);
        if (state == null) {
            Logger.Game.debug("Primio null GameStateUpdate!");
            return;
        }

        Logger.Game.debug("handleGameStateUpdate: " + state);

        // Update client-side observables
        clientDiceValues.setAll(state.diceValues());
        clientDiceHeld.setAll(state.diceHeld());
        clientRollCount.set(state.rollCount());
        clientCurrentPlayerIndex.set(state.currentPlayerIndex());
        clientCurrentPlayerName.set(state.currentPlayerName());

        // Update total scores
        clientTotalScores.clear();
        clientTotalScores.putAll(state.totalScores());

        // Update isLocalPlayerTurn based on current player
        isLocalPlayerTurn.set(state.currentPlayerName().equals(localPlayerName));

        // Update GameEngine if we have one (CRITICAL for GUI binding)
        if (gameEngine != null) {
            // Update dice
            var dice = gameEngine.getDiceSet().getDice();
            for (int i = 0; i < state.diceValues().size() && i < dice.size(); i++) {
                dice.get(i).setValue(state.diceValues().get(i));
                dice.get(i).heldProperty().set(state.diceHeld().get(i));
            }

            // Update roll count
            gameEngine.rollCountProperty().set(state.rollCount());

            // Update current player index
            gameEngine.currentPlayerIndexProperty().set(state.currentPlayerIndex());

            // Update scores in GameEngine
            Map<String, Map<ScoreCategory, Integer>> scoreSheets = state.scoreSheets();
            for (Player player : gameEngine.getPlayers()) {
                String playerName = player.getName();
                if (scoreSheets.containsKey(playerName)) {
                    Map<ScoreCategory, Integer> scores = scoreSheets.get(playerName);
                    ScoreSheet sheet = player.getSheet();
                    for (Map.Entry<ScoreCategory, Integer> entry : scores.entrySet()) {
                        if (!sheet.filledProperty(entry.getKey()).get()) {
                            sheet.setScore(entry.getKey(), entry.getValue());
                        }
                    }
                }
            }
        }

        // Check for game over
        if (state.gameOver() && onGameOverCallback != null) {
            updateStatus("Igra završena! Pobjednik: " + state.winnerName());
            onGameOverCallback.run();
        } else {
            updateStatus(isLocalPlayerTurn.get() ? "Vaš potez!" : "Potez igrača: " + state.currentPlayerName());
        }
    }

    private void handleRollRequest(GameMessage message) {
        Logger.Game.debug("handleRollRequest pozvan. isHost: " + isHost + ", gameEngine: " + (gameEngine != null));

        if (isHost && gameEngine != null) {
            Logger.Game.debug("Izvršavam roll za igrača: " + message.getSenderName());

            // Server izvršava roll preko GameEngine-a
            gameEngine.roll();

            Logger.Game.debug("Roll count nakon roll(): " + gameEngine.rollCountProperty().get());

            // LOG MOVE - Bacio kockice
            String diceValues = gameEngine.getDiceSet().getDice().stream()
                    .map(die -> String.valueOf(die.valueProperty().get()))
                    .collect(Collectors.joining(", "));
            logMove(new Move(gameEngine.getCurrentPlayer().getName(), diceValues));

            // Kreiraj i broadcast game state update
            GameStateUpdate stateUpdate = createGameStateUpdate();
            if (stateUpdate != null) {
                Logger.Game.debug("Broadcast-am GAME_STATE_UPDATE");
                broadcastAndHandleLocally(GameMessage.gameStateUpdate(stateUpdate));
            } else {
                Logger.Game.error("GREŠKA: createGameStateUpdate vratio null!");
            }
        } else {
            Logger.Game.warn("SKIP - isHost: " + isHost + ", gameEngine null: " + (gameEngine == null));
        }
    }

    private void handleDiceRolled(GameMessage message) {
        if (gameEngine != null) {
            int[] diceValues = (int[]) message.getPayload();
            if (diceValues != null) {
                // Svi igrači updataju svoje kockice na iste vrijednosti
                var dice = gameEngine.getDiceSet().getDice();
                for (int i = 0; i < diceValues.length && i < dice.size(); i++) {
                    dice.get(i).setValue(diceValues[i]);
                }
            }
        }
    }

    private void handleDiceHoldToggle(GameMessage message) {
        Logger.Game.debug("handleDiceHoldToggle pozvan. isHost: " + isHost + ", gameEngine: " + (gameEngine != null));

        if (isHost && gameEngine != null) {
            // Server procesira toggle
            var data = message.getPayloadAs(GameMessage.DiceHoldData.class);
            if (data != null) {
                Logger.Game.debug("Procesam DICE_HOLD_TOGGLE: dieIndex=" + data.dieIndex() + ", held=" + data.held());

                var dice = gameEngine.getDiceSet().getDice();
                if (data.dieIndex() >= 0 && data.dieIndex() < dice.size()) {
                    dice.get(data.dieIndex()).heldProperty().set(data.held());

                    Logger.Game.debug("Postavio held. Sad broadcast-am GAME_STATE_UPDATE");

                    // Broadcast game state update
                    GameStateUpdate stateUpdate = createGameStateUpdate();
                    if (stateUpdate != null) {
                        broadcastAndHandleLocally(GameMessage.gameStateUpdate(stateUpdate));
                        Logger.Game.debug("GAME_STATE_UPDATE poslan!");
                    } else {
                        Logger.Game.error("GREŠKA: createGameStateUpdate vratio null!");
                    }
                } else {
                    Logger.Game.error("GREŠKA: dieIndex izvan ranga: " + data.dieIndex());
                }
            } else {
                Logger.Game.error("GREŠKA: data je null!");
            }
        } else {
            Logger.Game.warn("SKIP handleDiceHoldToggle - isHost: " + isHost + ", gameEngine null: " + (gameEngine == null));
        }
    }

    private void handleScoreApplyRequest(GameMessage message) {
        if (isHost && gameEngine != null) {
            PlayerMove move = message.getPayloadAs(PlayerMove.class);
            if (move != null) {
                // Prvo dohvati score prije nego što apply-aš (jer apply mijenja state)
                int score = gameEngine.previewScore(move.category());
                String playerName = gameEngine.getCurrentPlayer().getName();

                // Server primjenjuje score
                gameEngine.applyScore(move.category());

                // LOG MOVE - Upisao rezultat
                logMove(new Move(playerName, move.category(), score));

                // Broadcast game state update (uključuje score, turn change, sve!)
                GameStateUpdate stateUpdate = createGameStateUpdate();
                if (stateUpdate != null) {
                    broadcastAndHandleLocally(GameMessage.gameStateUpdate(stateUpdate));
                }

                // Provjeri je li igra gotova (game over je već u state update-u)
                if (gameEngine.gameOverProperty().get() && onGameOverCallback != null) {
                    Platform.runLater(onGameOverCallback);
                }
            }
        }
    }

    private void handleScoreApplied(GameMessage message) {
        if (gameEngine != null && !isHost) {
            PlayerMove move = message.getPayloadAs(PlayerMove.class);
            if (move != null) {
                // Client primjenjuje score lokalno
                gameEngine.applyScore(move.category());
            }
        }
    }

    private void handleTurnChange(GameMessage message) {
        if (gameEngine != null) {
            String nextPlayerName = message.getPayloadAsString();
            isLocalPlayerTurn.set(nextPlayerName.equals(localPlayerName));

            updateStatus(isLocalPlayerTurn.get() ?
                    "Vaš potez!" :
                    "Potez igrača: " + nextPlayerName);
        }
    }

    private void handleGameOver(GameMessage message) {
        gameStarted.set(false);
        var data = message.getPayloadAs(GameMessage.GameOverData.class);
        if (data != null) {
            updateStatus("Igra završena! Pobjednik: " + data.winnerName() +
                    " (" + data.score() + " bodova)");
        }

        if (onGameOverCallback != null) {
            onGameOverCallback.run();
        }
    }

    private void handleChatMessage(GameMessage message) {
        String chatMsg = message.getSenderName() + ": " + message.getPayloadAsString();
        if (chatMessageCallback != null) {
            chatMessageCallback.accept(chatMsg);
        }
    }

    private void handleDisconnect(GameMessage message) {
        String disconnectedPlayer = message.getSenderName();
        updateStatus("Igrač " + disconnectedPlayer + " se odspojio");

        // Možda završi igru ako je u tijeku
        if (gameStarted.get()) {
            updateStatus("Igra prekinuta zbog gubitka veze");
            gameStarted.set(false);
        }
    }



     // Pokreće igru (samo host može)
    public void startGame() {
        if (!isHost) {
            updateStatus("Samo host može pokrenuti igru!");
            return;
        }

        if (playerNames.size() < 2) {
            updateStatus("Potrebno je minimalno 2 igrača!");
            Logger.Game.debug("Ne mogu pokrenuti igru - ima samo " + playerNames.size() + " igrača: " + playerNames);
            return;
        }

        Logger.Game.debug("Pokrećem igru sa igračima: " + playerNames);

        // Broadcast GAME_START svima sa listom igrača (da svi imaju isti redoslijed)
        GameMessage gameStartMsg = new GameMessage(
                MessageType.GAME_START,
                "SYSTEM",
                new ArrayList<>(playerNames) // Pošalji listu imena
        );
        broadcastMessage(gameStartMsg);

        // Inicijaliziraj lokalnu igru
        handleGameStart(gameStartMsg);
    }

    // lokalni player baca kockice
    public void rollDice() {
        Logger.Game.debug("rollDice pozvan. isLocalPlayerTurn: " + isLocalPlayerTurn.get());

        if (!isLocalPlayerTurn.get()) {
            updateStatus("Nije vaš potez!");
            Logger.Game.warn("ODBIJENO - nije turn");
            return;
        }

        if (gameEngine == null) {
            updateStatus("Igra nije započela!");
            Logger.Game.warn("ODBIJENO - gameEngine je null");
            return;
        }

        if (gameEngine.rollCountProperty().get() >= GameEngine.MAX_ROLLS) {
            updateStatus("Iskoristili ste sva bacanja!");
            Logger.Game.warn("ODBIJENO - max rolls");
            return;
        }

        Logger.Game.debug("Šaljem ROLL_REQUEST");
        // Pošalji request serveru
        GameMessage rollRequest = new GameMessage(
                MessageType.ROLL_REQUEST,
                localPlayerName,
                null
        );
        sendMessage(rollRequest);
    }

    public void toggleDiceHold(int dieIndex) {
        if (!isLocalPlayerTurn.get()) {
            updateStatus("Nije vaš potez!");
            return;
        }

        if (gameEngine == null) return;

        var dice = gameEngine.getDiceSet().getDice();
        if (dieIndex < 0 || dieIndex >= dice.size()) return;

        Die die = dice.get(dieIndex);
        boolean newHeldState = !die.heldProperty().get();

        Logger.Game.debug("toggleDiceHold - dieIndex: " + dieIndex + ", newState: " + newHeldState + ", isHost: " + isHost);

        // Lokalno primijeni
        die.heldProperty().set(newHeldState);

        if (isHost) {
            // Host broadcast-a game state update
            GameStateUpdate stateUpdate = createGameStateUpdate();
            if (stateUpdate != null) {
                Logger.Game.debug("Host broadcast-a GAME_STATE_UPDATE zbog held toggle");
                broadcastAndHandleLocally(GameMessage.gameStateUpdate(stateUpdate));
            }
        } else {
            // Client šalje DICE_HOLD_TOGGLE serveru (koristi sendMessage, ne broadcastMessage!)
            Logger.Game.debug("Client šalje DICE_HOLD_TOGGLE serveru");
            sendMessage(GameMessage.diceHeld(localPlayerName, dieIndex, newHeldState));
        }
    }


    public void applyScore(ScoreCategory category) {
        if (!isLocalPlayerTurn.get()) {
            updateStatus("Nije vaš potez!");
            return;
        }

        if (gameEngine == null) return;

        // Kreiraj PlayerMove
        int score = gameEngine.previewScore(category);
        List<Integer> diceValues = gameEngine.getDiceSet().getDiceValues();
        int rollNumber = gameEngine.rollCountProperty().get();

        PlayerMove move = new PlayerMove(
                localPlayerName,
                category,
                score,
                diceValues,
                rollNumber
        );

        // Pošalji request serveru
        GameMessage request = new GameMessage(
                MessageType.SCORE_APPLY_REQUEST,
                localPlayerName,
                move
        );
        sendMessage(request);
    }


    public void sendChatMessage(String message) {
        if (!connected.get()) {
            updateStatus("Niste povezani!");
            return;
        }

        sendMessage(GameMessage.chatMessage(localPlayerName, message));
    }


    private void sendMessage(GameMessage message) {
        if (isHost && server != null) {
            // Host ne broadcast-a, već handlea lokalno (jer je on server)
            handleMessage(message);
        } else if (client != null) {
            client.sendMessage(message);
        }
    }

    private void broadcastMessage(GameMessage message) {
        if (isHost && server != null) {
            server.broadcast(message);
        }
    }

    private void broadcastAndHandleLocally(GameMessage message) {
        if (isHost && server != null) {
            server.broadcast(message);
            // Host također procesira poruku lokalno
            Platform.runLater(() -> handleMessage(message));
        }
    }

    private void announceGameOver() {
        if (gameEngine == null) return;

        // Nađi pobjednika
        Player winner = gameEngine.getPlayers().stream()
                .max((p1, p2) -> Integer.compare(p1.getSheet().total(), p2.getSheet().total()))
                .orElse(null);

        if (winner != null) {
            GameMessage gameOverMsg = GameMessage.gameOver(
                    winner.getName(),
                    winner.getSheet().total()
            );
            broadcastMessage(gameOverMsg);
        }
    }

    private void updateStatus(String status) {
        statusMessage.set(status);
        Logger.Game.debug(status);
    }


    private void initializeMoveLogging() {
        gameId = "game_" + System.currentTimeMillis();

        moveLogger = new MoveLogger(gameId);
        moveLogger.start();

        moveDisplay = new MoveDisplay(
                moveLogger.getLogFile(),
                moveLogger.getFileLock(),
                2  // Interval 2 sekunde
        );
        moveDisplay.start();

        Logger.info("MoveLogging", "Inicijalizirano za game ID: " + gameId);
    }

    private void logMove(Move move) {
        if (moveLogger != null && isHost) {
            moveLogger.logMove(move);
        }
    }

    public MoveDisplay getMoveDisplay() {
        return moveDisplay;
    }

    public void shutdown() {
        // Zaustavi move logging
        if (moveLogger != null) {
            moveLogger.shutdown();
        }
        if (moveDisplay != null) {
            moveDisplay.shutdown();
        }

        if (server != null) {
            server.shutdown();
        }
        if (client != null) {
            client.disconnect();
        }
        connected.set(false);
        gameStarted.set(false);
    }

    // ===== GETTERI I SETTERI =====

    public GameEngine getGameEngine() {
        return gameEngine;
    }

    public boolean isHost() {
        return isHost;
    }

    public String getLocalPlayerName() {
        return localPlayerName;
    }

    public boolean isLocalPlayerTurn() {
        return isLocalPlayerTurn.get();
    }

    public BooleanProperty isLocalPlayerTurnProperty() {
        return isLocalPlayerTurn;
    }

    // Properties za binding

    public BooleanProperty connectedProperty() {
        return connected;
    }

    public BooleanProperty gameStartedProperty() {
        return gameStarted;
    }

    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    public IntegerProperty playersInLobbyProperty() {
        return playersInLobby;
    }

    // Callbacks

    public void setChatMessageCallback(Consumer<String> callback) {
        this.chatMessageCallback = callback;
    }

    public void setOnGameStartCallback(Runnable callback) {
        this.onGameStartCallback = callback;
    }

    public void setOnGameOverCallback(Runnable callback) {
        this.onGameOverCallback = callback;
    }

    public void setOnTurnChangeCallback(Runnable callback) {
        this.onTurnChangeCallback = callback;
    }

    // ===== GAME STATE MANAGEMENT =====

    /**
     * Kreira GameStateUpdate sa trenutnim stanjem igre (samo host)
     */
    private GameStateUpdate createGameStateUpdate() {
        if (!isHost || gameEngine == null) {
            return null;
        }

        // Extract dice values and held states
        List<Integer> diceValues = gameEngine.getDiceSet().getDiceValues();
        List<Boolean> diceHeld = gameEngine.getDiceSet().getDice().stream()
                .map(die -> die.heldProperty().get())
                .collect(Collectors.toList());

        // Extract score sheets
        Map<String, Map<ScoreCategory, Integer>> scoreSheets = new HashMap<>();
        for (Player player : gameEngine.getPlayers()) {
            Map<ScoreCategory, Integer> playerScores = new HashMap<>();
            ScoreSheet sheet = player.getSheet();
            for (ScoreCategory category : ScoreCategory.values()) {
                if (sheet.filledProperty(category).get()) {
                    playerScores.put(category, sheet.scoreProperty(category).get());
                }
            }
            scoreSheets.put(player.getName(), playerScores);
        }

        // Extract total scores
        Map<String, Integer> totalScores = new HashMap<>();
        for (Player player : gameEngine.getPlayers()) {
            totalScores.put(player.getName(), player.getSheet().total());
        }

        // Winner name (if game over)
        String winnerName = null;
        if (gameEngine.gameOverProperty().get()) {
            Player winner = gameEngine.getPlayers().stream()
                    .max(Comparator.comparingInt(p -> p.getSheet().total()))
                    .orElse(null);
            if (winner != null) {
                winnerName = winner.getName();
            }
        }

        return new GameStateUpdate(
                diceValues,
                diceHeld,
                gameEngine.rollCountProperty().get(),
                gameEngine.currentPlayerIndexProperty().get(),
                gameEngine.getCurrentPlayer().getName(),
                scoreSheets,
                totalScores,
                gameEngine.gameOverProperty().get(),
                winnerName
        );
    }


     // Getteri za client-side observables
    public ObservableList<Integer> getClientDiceValues() {
        return clientDiceValues;
    }

    public ObservableList<Boolean> getClientDiceHeld() {
        return clientDiceHeld;
    }

    public IntegerProperty clientRollCountProperty() {
        return clientRollCount;
    }

    public IntegerProperty clientCurrentPlayerIndexProperty() {
        return clientCurrentPlayerIndex;
    }

    public StringProperty clientCurrentPlayerNameProperty() {
        return clientCurrentPlayerName;
    }

    public ObservableMap<String, Integer> getClientTotalScores() {
        return clientTotalScores;
    }
}