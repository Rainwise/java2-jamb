package hr.ipicek.jamb.network.rmi;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.rmi.RemoteException;


// standalone pokretanje rmi servera
public class RMIServerApp extends Application {

    private RMIRegistryServer server;
    private TextArea txtLog;
    private Button btnStart;
    private Button btnStop;
    private Label lblStatus;
    private TextField txtPort;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Jamb RMI Server");

        // Main layout
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        // Top - Title and controls
        VBox top = new VBox(15);
        top.setAlignment(Pos.CENTER);

        Label title = new Label("JAMB RMI REGISTRY SERVER");
        title.setFont(Font.font("System Bold", 20));

        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER);

        Label lblPort = new Label("Port:");
        txtPort = new TextField("1099");
        txtPort.setPrefWidth(80);

        btnStart = new Button("Pokreni Server");
        btnStart.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btnStart.setPrefWidth(130);
        btnStart.setOnAction(e -> startServer());

        btnStop = new Button("Zaustavi Server");
        btnStop.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
        btnStop.setPrefWidth(130);
        btnStop.setDisable(true);
        btnStop.setOnAction(e -> stopServer());

        controls.getChildren().addAll(lblPort, txtPort, btnStart, btnStop);

        lblStatus = new Label("● Server nije pokrenut");
        lblStatus.setStyle("-fx-font-size: 14px; -fx-text-fill: #e74c3c;");

        top.getChildren().addAll(title, controls, lblStatus);

        // Center - Log area
        VBox center = new VBox(10);

        Label lblLog = new Label("Server Log:");
        lblLog.setFont(Font.font("System Bold", 12));

        txtLog = new TextArea();
        txtLog.setEditable(false);
        txtLog.setWrapText(true);
        txtLog.setPrefHeight(300);
        txtLog.setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");

        center.getChildren().addAll(lblLog, txtLog);
        VBox.setVgrow(txtLog, Priority.ALWAYS);

        // Bottom - Info
        VBox bottom = new VBox(5);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(10, 0, 0, 0));

        Label info1 = new Label("ℹ️ Ovaj server mora biti pokrenut PRIJE Jamb aplikacije");
        info1.setStyle("-fx-font-style: italic; -fx-text-fill: #666;");

        Label info2 = new Label("Klijenti se mogu povezati na: rmi://localhost:" + txtPort.getText());
        info2.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        bottom.getChildren().addAll(info1, info2);

        root.setTop(top);
        root.setCenter(center);
        root.setBottom(bottom);

        Scene scene = new Scene(root, 600, 500);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            if (server != null && server.isRunning()) {
                stopServer();
            }
        });
        primaryStage.show();

        log("RMI Server aplikacija pokrenuta");
        log("Klikni 'Pokreni Server' za start\n");
    }

    private void startServer() {
        try {
            int port = Integer.parseInt(txtPort.getText());

            log("=================================");
            log("Pokretanje RMI Registry servera...");
            log("Port: " + port);

            server = new RMIRegistryServer(port);

            // Redirect server output to our log
            System.setOut(new java.io.PrintStream(new java.io.OutputStream() {
                private StringBuilder buffer = new StringBuilder();

                @Override
                public void write(int b) {
                    if (b == '\n') {
                        String line = buffer.toString();
                        javafx.application.Platform.runLater(() -> log(line));
                        buffer = new StringBuilder();
                    } else {
                        buffer.append((char) b);
                    }
                }
            }));

            server.start();

            lblStatus.setText("● Server pokrenut");
            lblStatus.setStyle("-fx-font-size: 14px; -fx-text-fill: #27ae60; -fx-font-weight: bold;");

            btnStart.setDisable(true);
            btnStop.setDisable(false);
            txtPort.setDisable(true);

            log("=================================");
            log("✓ Server uspješno pokrenut!");
            log("✓ LobbyService registriran");
            log("✓ ChatService registriran");
            log("");
            log("Klijenti se mogu povezati na:");
            log("  rmi://localhost:" + port);
            log("=================================\n");

        } catch (NumberFormatException e) {
            showError("Port mora biti broj!");
            log("ERROR: Nevažeći port broj\n");
        } catch (RemoteException e) {
            showError("Greška pri pokretanju servera:\n" + e.getMessage());
            log("ERROR: " + e.getMessage() + "\n");
        }
    }

    private void stopServer() {
        if (server != null) {
            log("Zaustavljanje servera...");
            server.stop();

            lblStatus.setText("● Server zaustavljen");
            lblStatus.setStyle("-fx-font-size: 14px; -fx-text-fill: #e74c3c;");

            btnStart.setDisable(false);
            btnStop.setDisable(true);
            txtPort.setDisable(false);

            log("Server zaustavljen\n");
        }
    }

    private void log(String message) {
        javafx.application.Platform.runLater(() -> {
            txtLog.appendText(message + "\n");
        });
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Greška");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}