package hr.ipicek.jamb.network.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

// rmi interface za chat izmedju igraca
public interface ChatService extends Remote {


    // poruka svim igracima u igri
    void sendMessage(String gameId, ChatMessage message) throws RemoteException;
    List<ChatMessage> getMessages(String gameId) throws RemoteException;
    List<ChatMessage> getNewMessages(String gameId, java.time.LocalDateTime since) throws RemoteException;
    void clearMessages(String gameId) throws RemoteException;
    void registerChatListener(String gameId, String playerName, ChatListener callback)
            throws RemoteException;
    void unregisterChatListener(String gameId, String playerName) throws RemoteException;
}