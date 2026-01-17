package hr.ipicek.jamb.network.socket;

import hr.ipicek.jamb.network.protocol.GameMessage;
import hr.ipicek.jamb.util.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.function.Consumer;


 // TCP klijent koji se povezuje na GameServer, prima poruke od njega
public class GameClient extends Thread {

    private static final int DEFAULT_PORT = 8888;

    private final String serverHost;
    private final int serverPort;
    private final String playerName;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private Consumer<GameMessage> messageCallback;
    private Consumer<String> statusCallback;
    private Runnable onConnectedCallback;
    private Runnable onDisconnectedCallback;

    private volatile boolean running;
    private volatile boolean connected;

    public GameClient(String playerName, String serverHost) {
        this(playerName, serverHost, DEFAULT_PORT);
    }

    public GameClient(String playerName, String serverHost, int serverPort) {
        this.playerName = playerName;
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.running = false;
        this.connected = false;
        setDaemon(true);
        setName("GameClient-Thread-" + playerName);
    }

    @Override
    public void run() {
        try {
            notifyStatus("Povezivanje na server " + serverHost + ":" + serverPort + "...");
            socket = new Socket(serverHost, serverPort);

            // VAŽNO: Output stream PRIJE input stream-a
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            connected = true;
            running = true;
            notifyStatus("Uspješno povezan na server!");

            // Notify UI da smo povezani
            if (onConnectedCallback != null) {
                onConnectedCallback.run();
            }

            // Pošalji PLAYER_JOINED poruku
            sendMessage(GameMessage.playerJoined(playerName));

            // Glavna petlja - slušaj poruke od servera
            listenForMessages();

        } catch (IOException e) {
            notifyStatus("Greška pri povezivanju: " + e.getMessage());
            connected = false;
        } finally {
            disconnect();
        }
    }

    private void listenForMessages() {
        try {
            while (running && !isInterrupted()) {
                try {
                    Object obj = in.readObject();

                    if (obj instanceof GameMessage message) {
                        handleMessage(message);
                    }
                } catch (SocketException e) {
                    if (running) {
                        notifyStatus("Socket greška: " + e.getMessage());
                    }
                    break;
                } catch (EOFException e) {
                    notifyStatus("Server je zatvorio konekciju");
                    break;
                } catch (ClassNotFoundException e) {
                    notifyStatus("Nepoznata klasa poruke: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            if (running) {
                notifyStatus("I/O greška: " + e.getMessage());
            }
        }
    }

    private void handleMessage(GameMessage message) {
        Logger.Client.debug("Primljena poruka: " + message.getType());

        // Proslijedi poruku callback-u (UI)
        if (messageCallback != null) {
            messageCallback.accept(message);
        }

        // Specifični handling za određene tipove poruka
        switch (message.getType()) {
            case GAME_START -> notifyStatus("Igra počinje!");
            case GAME_OVER -> {
                notifyStatus("Igra je završila!");
                var data = message.getPayloadAs(GameMessage.GameOverData.class);
                if (data != null) {
                    notifyStatus("Pobjednik: " + data.winnerName() + " (" + data.score() + " bodova)");
                }
            }
            case DISCONNECT -> {
                String disconnectedPlayer = message.getPayloadAsString();
                notifyStatus("Igrač " + disconnectedPlayer + " se odspojio sa servera");
            }
            case ERROR -> {
                String error = message.getPayloadAsString();
                notifyStatus("GREŠKA: " + error);
            }
            default -> {
                // Ostale poruke proslijeđene su callback-u
            }
        }
    }


     // Šalje poruku serveru
    public boolean sendMessage(GameMessage message) {
        if (!connected || out == null) {
            notifyStatus("Nije povezan na server!");
            return false;
        }

        try {
            synchronized (out) {
                out.writeObject(message);
                out.flush();
                out.reset(); // Prevent memory leak
            }
            return true;
        } catch (IOException e) {
            notifyStatus("Greška pri slanju poruke: " + e.getMessage());
            return false;
        }
    }

    // Odspaja igrača sa servera
    public void disconnect() {
        if (connected) {
            // Pošalji disconnect poruku prije zatvaranja
            sendMessage(GameMessage.disconnect(playerName));
        }

        running = false;
        connected = false;

        try {
            if (in != null) in.close();
        } catch (IOException e) {
            // Ignore
        }

        try {
            if (out != null) out.close();
        } catch (IOException e) {
            // Ignore
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignore
        }

        notifyStatus("Diskonektovan od servera");

        // Notify UI
        if (onDisconnectedCallback != null) {
            onDisconnectedCallback.run();
        }
    }

    // Callback metode
    public void setMessageCallback(Consumer<GameMessage> callback) {
        this.messageCallback = callback;
    }

    public void setStatusCallback(Consumer<String> callback) {
        this.statusCallback = callback;
    }

    public void setOnConnectedCallback(Runnable callback) {
        this.onConnectedCallback = callback;
    }

    public void setOnDisconnectedCallback(Runnable callback) {
        this.onDisconnectedCallback = callback;
    }

    private void notifyStatus(String status) {
        Logger.Client.debug(status);
        if (statusCallback != null) {
            statusCallback.accept(status);
        }
    }

    // Getteri
    public boolean isConnected() {
        return connected;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getServerHost() {
        return serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }
}