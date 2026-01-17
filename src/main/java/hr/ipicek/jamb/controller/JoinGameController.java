package hr.ipicek.jamb.controller;

import hr.ipicek.jamb.network.NetworkGameEngine;
import hr.ipicek.jamb.network.NetworkGameManager;
import hr.ipicek.jamb.network.rmi.GameInfo;
import hr.ipicek.jamb.util.DialogUtils;
import hr.ipicek.jamb.util.SceneUtils;
import hr.ipicek.jamb.util.ViewPaths;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

// join game kontroler za pregled i pridruživanje igrama
public class JoinGameController {

    @FXML private TableView<GameInfo> tblGames;
    @FXML private TableColumn<GameInfo, String> colHost;
    @FXML private TableColumn<GameInfo, String> colPlayers;
    @FXML private TableColumn<GameInfo, String> colAddress;
    @FXML private TableColumn<GameInfo, String> colPort;
    @FXML private TableColumn<GameInfo, String> colStatus;
    @FXML private Button btnRefresh;
    @FXML private Button btnJoin;
    @FXML private Label lblStatus;
    @FXML private Label lblSelectedGame;

    private NetworkGameManager gameManager;
    private String playerName;
    private GameInfo selectedGame;

    public void setGameManager(NetworkGameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    @FXML
    public void initialize() {
        // Setup table columns
        colHost.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getHostName()));

        colPlayers.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCurrentPlayers() + " / " +
                        data.getValue().getMaxPlayers()));

        colAddress.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getServerAddress()));

        colPort.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().getServerPort())));

        colStatus.setCellValueFactory(data -> {
            GameInfo.GameStatus status = data.getValue().getStatus();
            String statusText = switch (status) {
                case WAITING -> "Čeka igrače";
                case FULL -> "Puna";
                case IN_PROGRESS -> "U tijeku";
                case FINISHED -> "Završena";
            };
            return new SimpleStringProperty(statusText);
        });

        // Selection listener
        tblGames.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedGame = newVal;
            if (newVal != null) {
                lblSelectedGame.setText(newVal.getDisplayName());
                btnJoin.setDisable(!newVal.isJoinable());
            } else {
                lblSelectedGame.setText("Nije odabrana");
                btnJoin.setDisable(true);
            }
        });

        // Auto-refresh
        Platform.runLater(this::handleRefresh);
    }

    @FXML
    private void handleRefresh() {
        if (gameManager == null) {
            showError("Game manager nije postavljen!");
            return;
        }

        lblStatus.setText("Učitavanje...");
        btnRefresh.setDisable(true);

        // Učitaj igre u background thread-u
        new Thread(() -> {
            try {
                List<GameInfo> games = gameManager.getAvailableGames();

                Platform.runLater(() -> {
                    tblGames.setItems(FXCollections.observableArrayList(games));
                    lblStatus.setText("Pronađeno " + games.size() + " igara");
                    btnRefresh.setDisable(false);

                    if (games.isEmpty()) {
                        lblStatus.setText("Nema dostupnih igara");
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("Greška pri učitavanju");
                    lblStatus.setStyle("-fx-text-fill: red;");
                    btnRefresh.setDisable(false);
                    showError("Greška pri učitavanju igara:\n" + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleJoin() {
        if (selectedGame == null) {
            showError("Odaberite igru!");
            return;
        }

        if (!selectedGame.isJoinable()) {
            showError("Igra nije dostupna za pridruživanje!");
            return;
        }

        btnJoin.setDisable(true);
        lblStatus.setText("Pridruživanje...");

        // Join u background thread-u
        new Thread(() -> {
            try {
                NetworkGameEngine gameEngine = gameManager.joinGame(selectedGame, playerName);

                Platform.runLater(() -> {
                    try {
                        FXMLLoader loader = SceneUtils.loadFXML(ViewPaths.NETWORK_GAME);

                        NetworkGameController controller = loader.getController();
                        controller.init(gameEngine, gameManager);

                        Scene scene = new Scene(loader.getRoot());
                        Stage stage = (Stage) btnJoin.getScene().getWindow();
                        stage.setScene(scene);
                        stage.setTitle(ViewPaths.TITLE_NETWORK_GAME);

                    } catch (IOException e) {
                        showError("Greška pri učitavanju game ekrana:\n" + e.getMessage());
                        btnJoin.setDisable(false);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("Greška pri pridruživanju");
                    lblStatus.setStyle("-fx-text-fill: red;");
                    btnJoin.setDisable(false);
                    showError("Greška pri pridruživanju igri:\n" + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleBack() {
        try {
            SceneUtils.switchScene(btnJoin, ViewPaths.NETWORK_LOBBY, "Jamb - Network Lobby");
        } catch (IOException e) {
            showError("Greška pri povratku:\n" + e.getMessage());
        }
    }

    private void showError(String message) {
        DialogUtils.showError(message);
    }
}