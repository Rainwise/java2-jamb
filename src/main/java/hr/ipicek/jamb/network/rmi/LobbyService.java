package hr.ipicek.jamb.network.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;


public interface LobbyService extends Remote {

    void registerGame(GameInfo gameInfo) throws RemoteException;
    List<GameInfo> getAvailableGames() throws RemoteException;
    GameInfo getGameInfo(String gameId) throws RemoteException;
    boolean joinGame(String gameId, String playerName) throws RemoteException;
    void updateGameStatus(String gameId, int currentPlayers, GameInfo.GameStatus status)
            throws RemoteException;
    void removeGame(String gameId) throws RemoteException;
    boolean ping() throws RemoteException;
}