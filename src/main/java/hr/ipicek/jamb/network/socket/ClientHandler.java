package hr.ipicek.jamb.network.socket;

import hr.ipicek.jamb.network.protocol.GameMessage;
import hr.ipicek.jamb.network.protocol.MessageType;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;


 // Thread koji rukuje komunikacijom sa jednim klijentom na serveru.
class ClientHandler extends Thread {

    private final Socket socket;
    private final GameServer server;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private String clientName;
    private volatile boolean running;

    public ClientHandler(Socket socket, GameServer server) throws IOException {
        this.socket = socket;
        this.server = server;
        this.running = true;
        this.clientName = "Player-" + socket.getPort(); // Privremeno ime

        // VAŽNO: Output stream mora biti kreiran PRIJE input stream-a
        // zbog ObjectInputStream konstruktora koji čita header
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());

        setDaemon(true);
        setName("ClientHandler-" + clientName);
    }

    @Override
    public void run() {
        try {
            while (running && !isInterrupted()) {
                try {
                    Object obj = in.readObject();

                    if (obj instanceof GameMessage message) {
                        handleMessage(message);
                    }
                } catch (SocketException e) {
                    if (running) {
                        System.err.println("[ClientHandler] Socket greška: " + e.getMessage());
                    }
                    break;
                } catch (EOFException e) {
                    System.out.println("[ClientHandler] Klijent " + clientName + " se odspojio sa servera");
                    break;
                } catch (ClassNotFoundException e) {
                    System.err.println("[ClientHandler] Nepoznata klasa: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("[ClientHandler] I/O greška: " + e.getMessage());
            }
        } finally {
            close();
            server.removeClient(this);
        }
    }

    private void handleMessage(GameMessage message) {
        // Ako je ovo prva poruka, postavi ime klijenta
        if (message.getType() == MessageType.PLAYER_JOINED) {
            this.clientName = message.getSenderName();
            System.out.println("[ClientHandler] Igrač identificiran: " + clientName);
        }

        System.out.println("[ClientHandler] Primljena poruka od " + clientName + ": " + message.getType());

        // Proslijedi poruku serveru
        server.onMessageReceived(message, this);
    }


     // Šalje poruku ovom klijentu
    // @return true ako je uspješno poslano, false ako je greška
    public boolean sendMessage(GameMessage message) {
        try {
            synchronized (out) {
                out.writeObject(message);
                out.flush();
                out.reset(); // Prevent memory leak
            }
            return true;
        } catch (IOException e) {
            System.err.println("[ClientHandler] Greška pri slanju poruke klijentu " +
                    clientName + ": " + e.getMessage());
            return false;
        }
    }

    public void close() {
        running = false;

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
    }

    public String getClientName() {
        return clientName;
    }
}