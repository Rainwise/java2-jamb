package hr.ipicek.jamb.network.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;



 // Upravlja listom dostupnih igara i omogućava igračima da ih pronađu.
public class LobbyServiceImpl extends UnicastRemoteObject implements LobbyService {

    private static final long serialVersionUID = 1L;

    // Thread-safe mapa igara
    private final Map<String, GameInfo> games;

    public LobbyServiceImpl() throws RemoteException {
        super();
        this.games = new ConcurrentHashMap<>();
        System.out.println("[LobbyService] Servis pokrenut");
    }

    @Override
    public void registerGame(GameInfo gameInfo) throws RemoteException {
        if (gameInfo == null) {
            throw new IllegalArgumentException("GameInfo ne može biti null");
        }

        games.put(gameInfo.getGameId(), gameInfo);
        System.out.println("[LobbyService] Registrirana nova igra: " + gameInfo);
    }

    @Override
    public List<GameInfo> getAvailableGames() throws RemoteException {
        // Vrati samo igre koje su joinable
        return games.values().stream()
                .filter(GameInfo::isJoinable)
                .sorted(Comparator.comparing(GameInfo::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public GameInfo getGameInfo(String gameId) throws RemoteException {
        return games.get(gameId);
    }

    @Override
    public boolean joinGame(String gameId, String playerName) throws RemoteException {
        GameInfo game = games.get(gameId);

        if (game == null) {
            System.out.println("[LobbyService] Igra ne postoji: " + gameId);
            return false;
        }

        if (!game.isJoinable()) {
            System.out.println("[LobbyService] Igra nije joinable: " + gameId);
            return false;
        }

        System.out.println("[LobbyService] Igrač " + playerName + " se pridružio igri " + gameId);

        // Update broj igrača (ovo će server kasnije ažurirati)
        return true;
    }

    @Override
    public void updateGameStatus(String gameId, int currentPlayers, GameInfo.GameStatus status)
            throws RemoteException {
        GameInfo oldInfo = games.get(gameId);

        if (oldInfo == null) {
            System.out.println("[LobbyService] Pokušaj ažuriranja nepostojeće igre: " + gameId);
            return;
        }

        // Kreiraj novi GameInfo sa ažuriranim podacima
        GameInfo newInfo = new GameInfo(
                oldInfo.getGameId(),
                oldInfo.getHostName(),
                oldInfo.getServerAddress(),
                oldInfo.getServerPort(),
                currentPlayers,
                oldInfo.getMaxPlayers(),
                status
        );

        games.put(gameId, newInfo);
        System.out.println("[LobbyService] Ažurirana igra: " + newInfo);

        // Ukloni igru ako je završena
        if (status == GameInfo.GameStatus.FINISHED) {
            removeGame(gameId);
        }
    }

    @Override
    public void removeGame(String gameId) throws RemoteException {
        GameInfo removed = games.remove(gameId);
        if (removed != null) {
            System.out.println("[LobbyService] Uklonjena igra: " + removed);
        }
    }

    @Override
    public boolean ping() throws RemoteException {
        return true;
    }

    public void cleanupOldGames() {
        // Ukloni igre starije od 1 sat
        games.entrySet().removeIf(entry -> {
            GameInfo game = entry.getValue();
            return java.time.Duration.between(game.getCreatedAt(),
                    java.time.LocalDateTime.now()).toHours() > 1;
        });
    }

    public String getStatistics() {
        return String.format("Ukupno igara: %d, Dostupnih: %d, U tijeku: %d",
                games.size(),
                games.values().stream().filter(GameInfo::isJoinable).count(),
                games.values().stream().filter(g -> g.getStatus() == GameInfo.GameStatus.IN_PROGRESS).count()
        );
    }
}