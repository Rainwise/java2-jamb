package hr.ipicek.jamb.network.rmi;

import hr.ipicek.jamb.util.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;


// za pristup rmi servisima preko jndi naming apija
public class JNDIClient {

    private final String host;
    private final int port;
    private Context context;
    private LobbyService lobbyService;
    private ChatService chatService;

    public JNDIClient(String host, int port) {
        this.host = host;
        this.port = port;
    }


    public JNDIClient(String host) {
        this(host, RMIRegistryServer.DEFAULT_RMI_PORT);
    }


    // inicijalizacija za jndi context i povezivanje na RMI registry
    public void connect() throws NamingException {
        Logger.Network.info("JNDI: Povezivanje na " + host + ":" + port + "...");

        // Postavi JNDI environment properties za RMI
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.rmi.registry.RegistryContextFactory");
        env.put(Context.PROVIDER_URL, "rmi://" + host + ":" + port);

        // Kreiraj InitialContext
        context = new InitialContext(env);

        Logger.Network.info("JNDI: Uspješno povezan");
    }

    // jndi lookup za looby service
    public LobbyService getLobbyService() throws NamingException {
        if (lobbyService == null) {
            if (context == null) {
                connect();
            }

            // jndi lookup sa RMI url
            String jndiName = RMIRegistryServer.LOBBY_SERVICE_NAME;
            Logger.Network.info("JNDI: Lookup LobbyService (" + jndiName + ")...");

            lobbyService = (LobbyService) context.lookup(jndiName);

            Logger.Network.info("JNDI: LobbyService lookup uspješan");
        }
        return lobbyService;
    }


    // chat service preko jndi lookup
    public ChatService getChatService() throws NamingException {
        if (chatService == null) {
            if (context == null) {
                connect();
            }

            String jndiName = RMIRegistryServer.CHAT_SERVICE_NAME;
            Logger.Network.info("JNDI: Lookup ChatService (" + jndiName + ")...");

            chatService = (ChatService) context.lookup(jndiName);

            Logger.Network.info("JNDI: ChatService lookup uspješan");
        }
        return chatService;
    }

    public boolean testConnection() {
        try {
            getLobbyService();
            return true;
        } catch (NamingException e) {
            Logger.Network.error("JNDI: Test konekcije neuspješan: " + e.getMessage());
            return false;
        }
    }

    // zatvori context
    public void close() {
        if (context != null) {
            try {
                context.close();
                Logger.Network.info("JNDI: Context zatvoren");
            } catch (NamingException e) {
                Logger.Network.error("JNDI: Greška pri zatvaranju context: " + e.getMessage());
            }
        }
    }

    public String getHost() {
        return host;
    }
    public int getPort() {
        return port;
    }
}