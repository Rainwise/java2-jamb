package hr.ipicek.jamb.controller;

import hr.ipicek.jamb.network.NetworkGameEngine;
import hr.ipicek.jamb.network.NetworkGameManager;
import hr.ipicek.jamb.util.DialogUtils;
import hr.ipicek.jamb.util.SceneUtils;
import hr.ipicek.jamb.util.ViewPaths;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.InetAddress;


// Host game kontroler za pokretanje i menadjiranje
public class HostGameController {

    @FXML private TextField txtServerPort;
    @FXML private Button btnStartServer;
    @FXML private Button btnStartGame;
    @FXML private Label lblGameStatus;
    @FXML private Label lblPlayersCount;
    @FXML private Label lblServerAddress;

    private NetworkGameManager gameManager;
    private NetworkGameEngine gameEngine;
    private String playerName;

    public void setGameManager(NetworkGameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    @FXML
    public void initialize() {
        lblGameStatus.setText("Server nije pokrenut");
        lblPlayersCount.setText("0 / 2");
    }

    @FXML
    private void handleStartServer() {
        String portStr = txtServerPort.getText().trim();

        if (portStr.isEmpty()) {
            showError("Unesite port!");
            return;
        }

        try {
            int port = Integer.parseInt(portStr);

            btnStartServer.setDisable(true);
            txtServerPort.setDisable(true);
            lblGameStatus.setText("Pokretanje servera...");

            // Pokreni server u background thread-u
            new Thread(() -> {
                try {
                    gameEngine = gameManager.createGame(playerName, port);

                    Platform.runLater(() -> {
                        lblGameStatus.setText("✓ Server pokrenut - čekanje igrača");
                        lblGameStatus.setStyle("-fx-text-fill: green;");

                        // Prikaži server adresu
                        try {
                            String address = InetAddress.getLocalHost().getHostAddress();
                            lblServerAddress.setText(address + ":" + port);
                        } catch (Exception e) {
                            lblServerAddress.setText("localhost:" + port);
                        }

                        // Bind players count
                        gameEngine.playersInLobbyProperty().addListener((obs, oldVal, newVal) -> {
                            Platform.runLater(() -> {
                                lblPlayersCount.setText(newVal + " / 2");

                                // Omogući Start Game button kad se pridruži drugi igrač
                                if (newVal.intValue() >= 2) {
                                    btnStartGame.setDisable(false);
                                    lblGameStatus.setText("✓ Svi igrači se pridružili!");
                                }
                            });
                        });

                        // Auto start game when all players joined
                        gameEngine.setOnGameStartCallback(() -> {
                            Platform.runLater(this::startGameView);
                        });

                        // Update initial count
                        lblPlayersCount.setText(gameEngine.playersInLobbyProperty().get() + " / 2");
                    });

                } catch (Exception e) {
                    Platform.runLater(() -> {
                        lblGameStatus.setText("✗ Greška pri pokretanju servera");
                        lblGameStatus.setStyle("-fx-text-fill: red;");
                        btnStartServer.setDisable(false);
                        txtServerPort.setDisable(false);
                        showError("Greška pri pokretanju servera:\n" + e.getMessage());
                    });
                }
            }).start();

        } catch (NumberFormatException e) {
            showError("Port mora biti broj!");
            btnStartServer.setDisable(false);
        }
    }

    @FXML
    private void handleStartGame() {
        if (gameEngine == null) {
            showError("Server nije pokrenut!");
            return;
        }

        // Pokreni igru (ovo će triggerati onGameStartCallback)
        gameEngine.startGame();
    }

    private void startGameView() {
        try {
            FXMLLoader loader = SceneUtils.loadFXML(ViewPaths.NETWORK_GAME);

            NetworkGameController controller = loader.getController();
            controller.init(gameEngine, gameManager);

            Scene scene = new Scene(loader.getRoot());
            Stage stage = (Stage) btnStartServer.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(ViewPaths.TITLE_NETWORK_GAME);

        } catch (IOException e) {
            showError("Greška pri učitavanju game ekrana:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        if (gameManager != null) {
            gameManager.shutdown();
        }

        try {
            SceneUtils.switchScene(btnStartServer, ViewPaths.NETWORK_LOBBY, "Jamb - Network Lobby");
        } catch (IOException e) {
            showError("Greška pri povratku:\n" + e.getMessage());
        }
    }

    private void showError(String message) {
        DialogUtils.showError(message);
    }
}