package hr.ipicek.jamb.controller;

import hr.ipicek.jamb.model.Die;
import hr.ipicek.jamb.model.GameEngine;
import hr.ipicek.jamb.model.Player;
import hr.ipicek.jamb.model.ScoreCategory;
import hr.ipicek.jamb.network.NetworkGameEngine;
import hr.ipicek.jamb.network.NetworkGameManager;
import hr.ipicek.jamb.network.rmi.ChatMessage;
import hr.ipicek.jamb.util.DialogUtils;
import hr.ipicek.jamb.util.SceneUtils;
import hr.ipicek.jamb.util.ViewPaths;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.io.IOException;

// kontroler za main view igre sa chatom
public class NetworkGameController {

    // Game UI
    @FXML private Label lblCurrentPlayer;
    @FXML private Label lblYourTurn;
    @FXML private Label lblRolls;
    @FXML private Label lblNetworkStatus;
    @FXML private Label lblTotal;
    @FXML private Button btnRoll;
    @FXML private ImageView imgDie1, imgDie2, imgDie3, imgDie4, imgDie5;
    @FXML private TableView<Row> tblScores;
    @FXML private TableColumn<Row, String> colCategory;
    @FXML private TableColumn<Row, Number> colPlayer1;
    @FXML private TableColumn<Row, Number> colPlayer2;

    // Move Display UI
    @FXML private Label lblMovePlayer;
    @FXML private Label lblMoveAction;
    @FXML private Label lblMoveTime;

    // Chat UI
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatBox;
    @FXML private TextField txtChatInput;

    private NetworkGameEngine networkEngine;
    private NetworkGameManager gameManager;
    private GameEngine gameEngine;

    public void init(NetworkGameEngine networkEngine, NetworkGameManager gameManager) {
        this.networkEngine = networkEngine;
        this.gameManager = gameManager;
        this.gameEngine = networkEngine.getGameEngine();

        if (gameEngine == null) {
            // Igra još nije počela - čekamo
            lblCurrentPlayer.setText("Čekanje početka igre...");
            btnRoll.setDisable(true);

            // Postavi listener za kad igra počne
            networkEngine.gameStartedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    Platform.runLater(() -> {
                        this.gameEngine = networkEngine.getGameEngine();
                        if (gameEngine != null) {
                            setupGame();
                        } else {
                            showError("Game engine još nije dostupan!");
                        }
                    });
                }
            });
        } else {
            // GameEngine već postoji
            setupGame();
        }

        setupChat();
    }

    private void setupGame() {
        // Bind player info
        lblCurrentPlayer.textProperty().bind(Bindings.createStringBinding(
                () -> "Potez: " + gameEngine.getCurrentPlayer().getName(),
                gameEngine.currentPlayerIndexProperty()
        ));

        // Bind rolls
        lblRolls.textProperty().bind(Bindings.concat("Bacanja: ",
                gameEngine.rollCountProperty().asString(), "/",
                String.valueOf(GameEngine.MAX_ROLLS)));

        // Roll button
        // Disable button kad je max rolls ILI kad nije turn lokalnog igrača
        var maxRollsReached = gameEngine.rollCountProperty().greaterThanOrEqualTo(GameEngine.MAX_ROLLS);
        var notMyTurn = Bindings.not(networkEngine.isLocalPlayerTurnProperty());
        btnRoll.disableProperty().bind(maxRollsReached.or(notMyTurn));

        // Setup dice
        var dice = gameEngine.getDiceSet().getDice();
        var diceImages = gameEngine.getDiceImagePaths();

        bindDie(imgDie1, dice.get(0), diceImages.get(0), 0);
        bindDie(imgDie2, dice.get(1), diceImages.get(1), 1);
        bindDie(imgDie3, dice.get(2), diceImages.get(2), 2);
        bindDie(imgDie4, dice.get(3), diceImages.get(3), 3);
        bindDie(imgDie5, dice.get(4), diceImages.get(4), 4);

        // Setup score table
        refreshScoreTable();

        // Turn indicator - bind direktno na property (automatski update bez listenera)
        lblYourTurn.visibleProperty().bind(networkEngine.isLocalPlayerTurnProperty());

        // Game over handler
        networkEngine.setOnGameOverCallback(() -> {
            Platform.runLater(this::handleGameOver);
        });

        // Network status
        networkEngine.connectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                lblNetworkStatus.setText("● Povezan");
                lblNetworkStatus.setStyle("-fx-text-fill: #2ecc71;");
            } else {
                lblNetworkStatus.setText("● Odspojen");
                lblNetworkStatus.setStyle("-fx-text-fill: #e74c3c;");
            }
        });

        // Move Display binding (samo ako je HOST, jer samo HOST ima MoveDisplay)
        if (networkEngine.isHost() && networkEngine.getMoveDisplay() != null) {
            var moveDisplay = networkEngine.getMoveDisplay();
            lblMovePlayer.textProperty().bind(moveDisplay.lastMovePlayerProperty());
            lblMoveAction.textProperty().bind(moveDisplay.lastMoveActionProperty());
            lblMoveTime.textProperty().bind(moveDisplay.lastMoveTimeProperty());
        } else {
            // Client ne prikazuje move display (može se dodati sync later ako treba)
            lblMovePlayer.setText("N/A");
            lblMoveAction.setText("(Dostupno samo na serveru)");
            lblMoveTime.setText("--:--:--");
        }
    }

    private void bindDie(ImageView img, Die die, StringProperty imagePath, int index) {
        img.setFitWidth(72);
        img.setFitHeight(72);
        img.setPreserveRatio(true);

        imagePath.addListener((obs, old, newPath) -> updateImage(img, newPath));
        updateImage(img, imagePath.get());

        die.heldProperty().addListener((obs, oldVal, held) ->
                img.setOpacity(Boolean.TRUE.equals(held) ? 0.6 : 1.0)
        );
        img.setOpacity(die.heldProperty().get() ? 0.6 : 1.0);

        // Click to hold/unhold
        img.setOnMouseClicked(e -> {
            if (networkEngine.isLocalPlayerTurn()) {
                networkEngine.toggleDiceHold(index);
            }
        });

        img.setOnMouseEntered(e -> {
            if (networkEngine.isLocalPlayerTurn()) {
                img.setScaleX(1.08);
                img.setScaleY(1.08);
            }
        });
        img.setOnMouseExited(e -> {
            img.setScaleX(1.0);
            img.setScaleY(1.0);
        });
    }

    private void updateImage(ImageView img, String path) {
        var url = getClass().getResource(path);
        if (url != null) {
            img.setImage(new Image(url.toExternalForm()));
        }
    }

    private void refreshScoreTable() {
        var rows = FXCollections.<Row>observableArrayList();
        var players = gameEngine.getPlayers();
        var p1 = players.get(0);
        var p2 = players.size() > 1 ? players.get(1) : p1;

        for (var c : ScoreCategory.values()) {
            rows.add(new Row(c, p1, p2));
        }

        tblScores.setItems(rows);
        colCategory.setCellValueFactory(data -> data.getValue().nameProperty());
        colPlayer1.setCellValueFactory(data -> data.getValue().score1Property());
        colPlayer2.setCellValueFactory(data -> data.getValue().score2Property());

        // Setup click handlers for applying score
        tblScores.setRowFactory(tv -> {
            TableRow<Row> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && networkEngine.isLocalPlayerTurn()) {
                    Row clickedRow = row.getItem();
                    handleScoreClick(clickedRow.category());
                }
            });
            return row;
        });

        // Bind total - prikaži score LOKALNOG igrača, ne trenutnog na potezu
        Player localPlayer = gameEngine.getPlayers().stream()
                .filter(p -> p.getName().equals(networkEngine.getLocalPlayerName()))
                .findFirst()
                .orElse(gameEngine.getPlayers().get(0)); // fallback

        var sheet = localPlayer.getSheet();
        lblTotal.textProperty().bind(Bindings.createStringBinding(
                () -> String.valueOf(sheet.total()),
                sheet.scoreProperty(ScoreCategory.ONES),
                sheet.scoreProperty(ScoreCategory.TWOS),
                sheet.scoreProperty(ScoreCategory.THREES),
                sheet.scoreProperty(ScoreCategory.FOURS),
                sheet.scoreProperty(ScoreCategory.FIVES),
                sheet.scoreProperty(ScoreCategory.SIXES),
                sheet.scoreProperty(ScoreCategory.THREE_OF_A_KIND),
                sheet.scoreProperty(ScoreCategory.FOUR_OF_A_KIND),
                sheet.scoreProperty(ScoreCategory.FULL_HOUSE),
                sheet.scoreProperty(ScoreCategory.SMALL_STRAIGHT),
                sheet.scoreProperty(ScoreCategory.LARGE_STRAIGHT),
                sheet.scoreProperty(ScoreCategory.YAHTZEE),
                sheet.scoreProperty(ScoreCategory.CHANCE)
        ));
    }

    private void handleScoreClick(ScoreCategory category) {
        var sheet = gameEngine.getCurrentPlayer().getSheet();

        if (sheet.filledProperty(category).get()) {
            return; // Already filled
        }

        int score = gameEngine.previewScore(category);
        int rolls = gameEngine.rollCountProperty().get();

        if (DialogUtils.showScoreConfirmation(category, score, rolls)) {
            networkEngine.applyScore(category);
        }
    }

    @FXML
    private void handleRoll() {
        System.out.println("[NetworkGameController] handleRoll pozvan. isLocalPlayerTurn: " + networkEngine.isLocalPlayerTurn());
        networkEngine.rollDice();
    }


    // chat setup
    private void setupChat() {
        // Setup chat message callback
        gameManager.setOnChatMessageReceived(this::addChatMessage);

        // Load existing messages
        try {
            var messages = gameManager.getChatMessages();
            for (ChatMessage msg : messages) {
                addChatMessage(msg);
            }
        } catch (Exception e) {
            System.err.println("Greška pri učitavanju chat poruka: " + e.getMessage());
        }

        // Auto-scroll
        chatBox.heightProperty().addListener((obs, oldVal, newVal) -> {
            chatScrollPane.setVvalue(1.0);
        });
    }

    @FXML
    private void handleSendChat() {
        String message = txtChatInput.getText().trim();

        if (message.isEmpty()) {
            return;
        }

        try {
            gameManager.sendChatMessage(message);
            txtChatInput.clear();
        } catch (Exception e) {
            showError("Greška pri slanju poruke:\n" + e.getMessage());
        }
    }

    private void addChatMessage(ChatMessage message) {
        Platform.runLater(() -> {
            HBox messageBox = new HBox(5);
            messageBox.setPadding(new Insets(5));
            messageBox.setAlignment(Pos.CENTER_LEFT);

            // Style based on message type
            String style = switch (message.getType()) {
                case SYSTEM -> "-fx-background-color: #e8f5e9; -fx-background-radius: 5;";
                case GAME_EVENT -> "-fx-background-color: #fff3e0; -fx-background-radius: 5;";
                default -> "-fx-background-color: #f5f5f5; -fx-background-radius: 5;";
            };
            messageBox.setStyle(style);

            VBox content = new VBox(2);

            Label sender = new Label(message.getSender());
            sender.setFont(Font.font("System", 10));
            sender.setStyle("-fx-font-weight: bold;");

            Label text = new Label(message.getMessage());
            text.setWrapText(true);
            text.setMaxWidth(240);

            Label time = new Label(message.getFormattedTime());
            time.setFont(Font.font("System", 9));
            time.setStyle("-fx-text-fill: gray;");

            content.getChildren().addAll(sender, text, time);
            messageBox.getChildren().add(content);

            chatBox.getChildren().add(messageBox);
        });
    }


    private void handleGameOver() {
        var players = gameEngine.getPlayers();
        Player winner = players.stream()
                .max((p1, p2) -> Integer.compare(p1.getSheet().total(), p2.getSheet().total()))
                .orElse(null);

        if (winner != null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Igra završena!");
            alert.setHeaderText("Pobjednik: " + winner.getName());
            alert.setContentText("Ukupno bodova: " + winner.getSheet().total());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleQuit() {
        if (!DialogUtils.showConfirmation("Potvrda", "Napustiti igru?",
                "Jeste li sigurni da želite napustiti?")) {
            return;
        }

        if (gameManager != null) {
            gameManager.shutdown();
        }

        try {
            SceneUtils.switchScene(btnRoll, ViewPaths.MENU, ViewPaths.TITLE_MENU);
        } catch (IOException e) {
            showError("Greška pri povratku na meni:\n" + e.getMessage());
        }
    }

    private void showError(String message) {
        DialogUtils.showError(message);
    }

    // Row class for table
    public static class Row {
        private final ScoreCategory category;
        private final StringProperty name;
        private final IntegerProperty score1;
        private final IntegerProperty score2;

        public Row(ScoreCategory c, Player p1, Player p2) {
            this.category = c;
            this.name = new SimpleStringProperty(c.displayName());
            this.score1 = p1.getSheet().scoreProperty(c);
            this.score2 = p2.getSheet().scoreProperty(c);
        }

        public ScoreCategory category() { return category; }
        public StringProperty nameProperty() { return name; }
        public IntegerProperty score1Property() { return score1; }
        public IntegerProperty score2Property() { return score2; }
    }
}