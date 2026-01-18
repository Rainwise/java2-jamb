package hr.ipicek.jamb.network.socket;

import hr.ipicek.jamb.network.protocol.GameMessage;
import hr.ipicek.jamb.network.protocol.MessageType;
import hr.ipicek.jamb.util.Logger;
import hr.ipicek.jamb.util.NetworkConstants;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;


 // TCP Server koji prima konekcije igrača i upravlja mrežnom igrom. Radi u vlasitiom threadu.
public class GameServer extends Thread {

    private final int port;
    private ServerSocket serverSocket;
    private final List<ClientHandler> connectedClients;
    private Consumer<GameMessage> messageCallback;
    private Consumer<String> statusCallback;
    private volatile boolean running;
    private volatile boolean gameStarted;

    public GameServer() {
        this(NetworkConstants.DEFAULT_GAME_SERVER_PORT);
    }

    public GameServer(int port) {
        this.port = port;
        this.connectedClients = new CopyOnWriteArrayList<>();
        this.running = false;
        this.gameStarted = false;
        setDaemon(true);
        setName("GameServer-Thread");
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            notifyStatus("Server pokrenut na portu " + port);

            while (running && !isInterrupted()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    handleNewClient(clientSocket);
                } catch (SocketException e) {
                    if (running) {
                        notifyStatus("Socket greška: " + e.getMessage());
                    }
                    // Ako nije running, normalno gašenje
                } catch (IOException e) {
                    notifyStatus("Greška pri prihvaćanju klijenta: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            notifyStatus("Greška pri pokretanju servera: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    private void handleNewClient(Socket clientSocket) {
        if (connectedClients.size() >= NetworkConstants.MAX_PLAYERS) {
            notifyStatus("Odbijen novi klijent - već ima " + NetworkConstants.MAX_PLAYERS + " igrača");
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignore
            }
            return;
        }

        try {
            ClientHandler handler = new ClientHandler(clientSocket, this);
            connectedClients.add(handler);
            handler.start();
            notifyStatus("Novi igrač povezan. Ukupno: " + connectedClients.size() + "/" + NetworkConstants.MAX_PLAYERS);

            // Ako su se svi igrači povezali, pokreni igru
            if (connectedClients.size() == NetworkConstants.MAX_PLAYERS && !gameStarted) {
                startGame();
            }
        } catch (IOException e) {
            notifyStatus("Greška pri kreiranju handler-a: " + e.getMessage());
        }
    }

    private void startGame() {
        gameStarted = true;
        notifyStatus("Svi igrači povezani! Igra počinje...");
        broadcast(GameMessage.gameStart());
    }


     // Šalje poruku svim povezanim klijentima
    public void broadcast(GameMessage message) {
        List<ClientHandler> disconnected = new ArrayList<>();

        for (ClientHandler client : connectedClients) {
            if (!client.sendMessage(message)) {
                disconnected.add(client);
            }
        }

        // Ukloni disconnected klijente
        disconnected.forEach(this::removeClient);
    }


     // Šalje poruku svim klijentima osim pošiljatelja
    public void broadcastExcept(GameMessage message, ClientHandler sender) {
        for (ClientHandler client : connectedClients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }


     // Callback kad server primi poruku od klijenta
    void onMessageReceived(GameMessage message, ClientHandler sender) {
        Logger.Server.debug("Primljena poruka: " + message.getType() + " od " + message.getSenderName());

        // Proslijedi poruku callback-u (UI) - to je HOST
        if (messageCallback != null) {
            messageCallback.accept(message);
        }

        // Odluči treba li broadcast-ati i kako
        if (shouldBroadcastToAll(message.getType())) {
            Logger.Server.debug("Broadcast svima: " + message.getType());
            // Pošalji svima uključujući pošiljatelja
            broadcast(message);
        } else if (shouldBroadcast(message.getType())) {
            Logger.Server.debug("Broadcast osim pošiljatelja: " + message.getType());
            // Pošalji svima osim pošiljatelja
            broadcastExcept(message, sender);
        } else {
            Logger.Server.debug("Ne broadcast-am: " + message.getType());
        }
    }

    private boolean shouldBroadcastToAll(MessageType type) {
        // Ove poruke trebaju svi igrači vidjeti (uključujući pošiljatelja)
        return type == MessageType.PLAYER_JOINED ||
                type == MessageType.GAME_START ||
                type == MessageType.GAME_STATE_UPDATE ||
                type == MessageType.TURN_CHANGE ||
                type == MessageType.GAME_OVER;
    }

    private boolean shouldBroadcast(MessageType type) {
        // Ne broadcast-aj request poruke - te handlea samo server
        if (type == MessageType.ROLL_REQUEST ||
                type == MessageType.SCORE_APPLY_REQUEST ||
                type == MessageType.SCORE_PREVIEW_REQUEST) {
            return false;
        }

        // Sve ostale poruke se broadcast-aju osim ERROR poruka
        return type != MessageType.ERROR;
    }

    void removeClient(ClientHandler client) {
        if (connectedClients.remove(client)) {
            notifyStatus("Igrač isključen. Preostalo: " + connectedClients.size());

            // Ako je igra u tijeku i igrač se isključio, notify ostale
            if (gameStarted) {
                broadcast(GameMessage.disconnect(client.getClientName()));
            }
        }
    }

    public void shutdown() {
        running = false;

        // Zatvori sve klijentske konekcije
        for (ClientHandler client : connectedClients) {
            client.close();
        }
        connectedClients.clear();

        // Zatvori server socket
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                notifyStatus("Greška pri zatvaranju servera: " + e.getMessage());
            }
        }

        notifyStatus("Server zaustavljen");
    }

    // Callback metode

    public void setMessageCallback(Consumer<GameMessage> callback) {
        this.messageCallback = callback;
    }

    public void setStatusCallback(Consumer<String> callback) {
        this.statusCallback = callback;
    }

    private void notifyStatus(String status) {
        System.out.println("[SERVER] " + status);
        if (statusCallback != null) {
            statusCallback.accept(status);
        }
    }

    // Getteri

    public int getPort() {
        return port;
    }

    public int getConnectedPlayersCount() {
        return connectedClients.size();
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }
}