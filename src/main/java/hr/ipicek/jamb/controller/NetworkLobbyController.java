package hr.ipicek.jamb.controller;

import hr.ipicek.jamb.JambApplication;
import hr.ipicek.jamb.network.NetworkGameManager;
import hr.ipicek.jamb.network.rmi.RMIRegistryServer;
import hr.ipicek.jamb.util.DialogUtils;
import hr.ipicek.jamb.util.SceneUtils;
import hr.ipicek.jamb.util.ViewPaths;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

// network lobby kontroler
public class NetworkLobbyController {

    @FXML private Label lblStatus;
    @FXML private Label lblInfo;
    @FXML private TextField txtRmiHost;
    @FXML private TextField txtRmiPort;
    @FXML private TextField txtPlayerName;
    @FXML private CheckBox chkUseJNDI;
    @FXML private Button btnConnect;
    @FXML private Button btnHost;
    @FXML private Button btnJoin;

    private NetworkGameManager gameManager;

    @FXML
    public void initialize() {
        lblInfo.setText("Prvo se povežite na RMI server, zatim odaberite Host ili Join.");
        chkUseJNDI.setSelected(true);
        chkUseJNDI.setDisable(true);
        // Default player name
        txtPlayerName.setText("Player" + (int)(Math.random() * 1000));
    }

    @FXML
    private void handleConnect() {
        String host = txtRmiHost.getText().trim();
        String portStr = txtRmiPort.getText().trim();

        if (host.isEmpty() || portStr.isEmpty()) {
            showError("Unesite host i port!");
            return;
        }

        try {
            int port = Integer.parseInt(portStr);
            boolean useJNDI = chkUseJNDI.isSelected();

            lblStatus.setText("Povezivanje...");
            btnConnect.setDisable(true);

            // Povezivanje u background thread-u
            new Thread(() -> {
                try {
                    gameManager = new NetworkGameManager(host, port, useJNDI);
                    gameManager.connect();

                    javafx.application.Platform.runLater(() -> {
                        String method = useJNDI ? "JNDI" : "RMI Registry";
                        lblStatus.setText("✓ Povezan (via " + method + ")");
                        lblStatus.setStyle("-fx-text-fill: green;");
                        btnHost.setDisable(false);
                        btnJoin.setDisable(false);
                        txtRmiHost.setDisable(true);
                        txtRmiPort.setDisable(true);
                        chkUseJNDI.setDisable(true);
                        lblInfo.setText("Odaberite Host za kreiranje nove igre ili Join za pridruživanje.");
                    });

                } catch (Exception e) {
                    javafx.application.Platform.runLater(() -> {
                        lblStatus.setText("✗ Greška pri povezivanju");
                        lblStatus.setStyle("-fx-text-fill: red;");
                        btnConnect.setDisable(false);
                        showError("Greška pri povezivanju na RMI server:\n" + e.getMessage());
                    });
                }
            }).start();

        } catch (NumberFormatException e) {
            showError("Port mora biti broj!");
            btnConnect.setDisable(false);
        }
    }

    @FXML
    private void handleHost() {
        String playerName = txtPlayerName.getText().trim();

        if (playerName.isEmpty()) {
            showError("Unesite vaše ime!");
            return;
        }

        if (gameManager == null) {
            showError("Prvo se povežite na RMI server!");
            return;
        }

        try {
            FXMLLoader loader = SceneUtils.loadFXML(ViewPaths.HOST_GAME);

            HostGameController controller = loader.getController();
            controller.setGameManager(gameManager);
            controller.setPlayerName(playerName);

            Scene scene = new Scene(loader.getRoot());
            Stage stage = (Stage) btnHost.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Jamb - Host Game");

        } catch (IOException e) {
            showError("Greška pri učitavanju Host ekrana:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleJoin() {
        String playerName = txtPlayerName.getText().trim();

        if (playerName.isEmpty()) {
            showError("Unesite vaše ime!");
            return;
        }

        if (gameManager == null) {
            showError("Prvo se povežite na RMI server!");
            return;
        }

        try {
            FXMLLoader loader = SceneUtils.loadFXML(ViewPaths.JOIN_GAME);

            JoinGameController controller = loader.getController();
            controller.setGameManager(gameManager);
            controller.setPlayerName(playerName);

            Scene scene = new Scene(loader.getRoot());
            Stage stage = (Stage) btnJoin.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Jamb - Join Game");

        } catch (IOException e) {
            showError("Greška pri učitavanju Join ekrana:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        if (gameManager != null) {
            gameManager.shutdown();
        }

        try {
            SceneUtils.switchScene(btnConnect, ViewPaths.MENU, ViewPaths.TITLE_MENU);
        } catch (IOException e) {
            showError("Greška pri povratku na meni:\n" + e.getMessage());
        }
    }

    private void showError(String message) {
        DialogUtils.showError(message);
    }
}