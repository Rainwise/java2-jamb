package hr.ipicek.jamb.network.rmi;
import hr.ipicek.jamb.util.Logger;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


 // Helper klasa za povezivanje klijenata na RMI servise koji koristi JNDI naming za lookup servisa.
public class RMIClient {

    private final String host;
    private final int port;
    private Registry registry;
    private LobbyService lobbyService;
    private ChatService chatService;

    public RMIClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public RMIClient(String host) {
        this(host, RMIRegistryServer.DEFAULT_RMI_PORT);
    }

    // povezivanje na rmi registry
    public void connect() throws RemoteException {
        Logger.Network.info("Povezivanje na " + host + ":" + port + "...");
        registry = LocateRegistry.getRegistry(host, port);
        System.out.println("[RMIClient] Uspješno povezan");
    }


    public LobbyService getLobbyService() throws RemoteException, NotBoundException {
        if (lobbyService == null) {
            if (registry == null) {
                connect();
            }
            lobbyService = (LobbyService) registry.lookup(RMIRegistryServer.LOBBY_SERVICE_NAME);
            System.out.println("[RMIClient] LobbyService lookup uspješan");
        }
        return lobbyService;
    }


    public ChatService getChatService() throws RemoteException, NotBoundException {
        if (chatService == null) {
            if (registry == null) {
                connect();
            }
            chatService = (ChatService) registry.lookup(RMIRegistryServer.CHAT_SERVICE_NAME);
            System.out.println("[RMIClient] ChatService lookup uspješan");
        }
        return chatService;
    }


    public boolean testConnection() {
        try {
            LobbyService lobby = getLobbyService();
            return lobby.ping();
        } catch (Exception e) {
            System.err.println("[RMIClient] Test konekcije neuspješan: " + e.getMessage());
            return false;
        }
    }

    public void reconnect() throws RemoteException {
        registry = null;
        lobbyService = null;
        chatService = null;
        connect();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }


    public void disconnect() {
        registry = null;
        lobbyService = null;
        chatService = null;
        System.out.println("[RMIClient] Server ugašen");
    }
}