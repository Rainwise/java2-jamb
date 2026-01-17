package hr.ipicek.jamb.network.rmi;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


 // RMI Registry server koji pokreće i registrira RMI servise.
 // Koristi JNDI naming za registraciju servisa.
public class RMIRegistryServer {

    public static final String LOBBY_SERVICE_NAME = "JambLobbyService";
    public static final String CHAT_SERVICE_NAME = "JambChatService";
    public static final int DEFAULT_RMI_PORT = 1099;

    private final int port;
    private Registry registry;
    private LobbyServiceImpl lobbyService;
    private ChatServiceImpl chatService;
    private ScheduledExecutorService maintenanceExecutor;
    private boolean running = false;

    public RMIRegistryServer() {
        this(DEFAULT_RMI_PORT);
    }

    public RMIRegistryServer(int port) {
        this.port = port;
    }

    // start rmi registrija
    public void start() throws RemoteException {
        if (running) {
            System.out.println("[RMIRegistry] Server već pokrenut");
            return;
        }

        System.out.println("[RMIRegistry] Pokretanje RMI Registry na portu " + port + "...");

        try {
            // Kreiraj RMI registry
            registry = LocateRegistry.createRegistry(port);
            System.out.println("[RMIRegistry] Registry kreiran na portu " + port);

            // Kreiraj i registriraj LobbyService
            lobbyService = new LobbyServiceImpl();
            registry.rebind(LOBBY_SERVICE_NAME, lobbyService);
            System.out.println("[RMIRegistry] " + LOBBY_SERVICE_NAME + " registriran");

            // Kreiraj i registriraj ChatService
            chatService = new ChatServiceImpl();
            registry.rebind(CHAT_SERVICE_NAME, chatService);
            System.out.println("[RMIRegistry] " + CHAT_SERVICE_NAME + " registriran");

            running = true;

            // Pokreni maintenance thread za čišćenje
            startMaintenanceThread();

            System.out.println("[RMIRegistry] Server uspješno pokrenut!");
            System.out.println("[RMIRegistry] Klijenti se mogu povezati na rmi://localhost:" + port);

        } catch (RemoteException e) {
            System.err.println("[RMIRegistry] Greška pri pokretanju: " + e.getMessage());
            throw e;
        }
    }

    private void startMaintenanceThread() {
        maintenanceExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "RMI-Maintenance-Thread");
            t.setDaemon(true);
            return t;
        });

        // Čisti stare podatke svakih 30 minuta
        maintenanceExecutor.scheduleAtFixedRate(() -> {
            try {
                System.out.println("[RMIRegistry] Maintenance: čišćenje starih podataka...");

                if (lobbyService != null) {
                    lobbyService.cleanupOldGames();
                    System.out.println("[RMIRegistry] " + lobbyService.getStatistics());
                }

                if (chatService != null) {
                    chatService.cleanupOldChats();
                    System.out.println("[RMIRegistry] " + chatService.getStatistics());
                }

            } catch (Exception e) {
                System.err.println("[RMIRegistry] Maintenance greška: " + e.getMessage());
            }
        }, 30, 30, TimeUnit.MINUTES);
    }

    public void stop() {
        if (!running) {
            System.out.println("[RMIRegistry] Server nije pokrenut");
            return;
        }

        System.out.println("[RMIRegistry] Zaustavljanje servera...");

        // Zaustavi maintenance thread
        if (maintenanceExecutor != null) {
            maintenanceExecutor.shutdown();
            try {
                maintenanceExecutor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                maintenanceExecutor.shutdownNow();
            }
        }

        // Unbind servise
        try {
            if (registry != null) {
                registry.unbind(LOBBY_SERVICE_NAME);
                registry.unbind(CHAT_SERVICE_NAME);
                System.out.println("[RMIRegistry] Servisi deregistrirani");
            }
        } catch (Exception e) {
            System.err.println("[RMIRegistry] Greška pri unbind-u: " + e.getMessage());
        }

        running = false;
        System.out.println("[RMIRegistry] Server zaustavljen");
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }


    // statistika servera
    public String getStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== RMI Registry Statistics ===\n");
        sb.append("Port: ").append(port).append("\n");
        sb.append("Running: ").append(running).append("\n");

        if (lobbyService != null) {
            sb.append("\nLobby Service:\n");
            sb.append("  ").append(lobbyService.getStatistics()).append("\n");
        }

        if (chatService != null) {
            sb.append("\nChat Service:\n");
            sb.append("  ").append(chatService.getStatistics()).append("\n");
        }

        return sb.toString();
    }

    // main metoda za pokretanje servera
    public static void main(String[] args) {
        int port = DEFAULT_RMI_PORT;

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Nevažeći port: " + args[0]);
                System.err.println("Korištenje: java RMIRegistryServer [port]");
                System.exit(1);
            }
        }

        RMIRegistryServer server = new RMIRegistryServer(port);

        try {
            server.start();

            // Dodaj shutdown hook za graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[RMIRegistry] Primljen shutdown signal...");
                server.stop();
            }));

            System.out.println("[RMIRegistry] Server radi. Pritisni Ctrl+C za zaustavljanje.");

            // Drži server živim
            Thread.currentThread().join();

        } catch (RemoteException e) {
            System.err.println("Greška pri pokretanju RMI servera: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (InterruptedException e) {
            System.out.println("[RMIRegistry] Server prekinut");
            server.stop();
        }
    }
}