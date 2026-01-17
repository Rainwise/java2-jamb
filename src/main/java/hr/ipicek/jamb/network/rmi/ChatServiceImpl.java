package hr.ipicek.jamb.network.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;


// chat servis koji upravlja svim chat porukama i notifikacijama za sve igre
public class ChatServiceImpl extends UnicastRemoteObject implements ChatService {

    private static final long serialVersionUID = 1L;
    private static final int MAX_MESSAGES_PER_GAME = 100;

    // mapiraj listu chat poruka na odredjeni game
    private final Map<String, List<ChatMessage>> chatHistory;
    private final Map<String, Map<String, ChatListener>> listeners;

    public ChatServiceImpl() throws RemoteException {
        super();
        this.chatHistory = new ConcurrentHashMap<>();
        this.listeners = new ConcurrentHashMap<>();
        System.out.println("[ChatService] Servis pokrenut");
    }

    @Override
    public void sendMessage(String gameId, ChatMessage message) throws RemoteException {
        if (gameId == null || message == null) {
            throw new IllegalArgumentException("GameId i message ne mogu biti null");
        }

        // Dodaj poruku u history
        chatHistory.computeIfAbsent(gameId, k -> new CopyOnWriteArrayList<>()).add(message);

        // Ograniči broj poruka
        List<ChatMessage> messages = chatHistory.get(gameId);
        if (messages.size() > MAX_MESSAGES_PER_GAME) {
            messages.remove(0);
        }
        System.out.println("[ChatService] Nova poruka u igri " + gameId + ": " + message);
        notifyListeners(gameId, message);
    }

    @Override
    public List<ChatMessage> getMessages(String gameId) throws RemoteException {
        List<ChatMessage> messages = chatHistory.get(gameId);
        return messages != null ? new ArrayList<>(messages) : Collections.emptyList();
    }

    @Override
    public List<ChatMessage> getNewMessages(String gameId, LocalDateTime since)
            throws RemoteException {
        List<ChatMessage> messages = chatHistory.get(gameId);

        if (messages == null) {
            return Collections.emptyList();
        }

        return messages.stream()
                .filter(msg -> msg.getTimestamp().isAfter(since))
                .collect(Collectors.toList());
    }

    @Override
    public void clearMessages(String gameId) throws RemoteException {
        chatHistory.remove(gameId);
        System.out.println("[ChatService] Očišćene poruke za igru: " + gameId);
    }

    @Override
    public void registerChatListener(String gameId, String playerName, ChatListener callback)
            throws RemoteException {
        listeners.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
                .put(playerName, callback);

        System.out.println("[ChatService] Registriran listener: " + playerName + " za igru " + gameId);

        // Pošalji welcome poruku
        ChatMessage welcome = ChatMessage.system(playerName + " se pridružio chatu");
        sendMessage(gameId, welcome);
    }

    @Override
    public void unregisterChatListener(String gameId, String playerName) throws RemoteException {
        Map<String, ChatListener> gameListeners = listeners.get(gameId);

        if (gameListeners != null) {
            gameListeners.remove(playerName);
            System.out.println("[ChatService] Deregistriran listener: " + playerName);

            // Pošalji goodbye poruku
            ChatMessage goodbye = ChatMessage.system(playerName + " je napustio chat");
            sendMessage(gameId, goodbye);

            // Ukloni cijelu game entry ako nema više listenera
            if (gameListeners.isEmpty()) {
                listeners.remove(gameId);
            }
        }
    }

    // obavjestava sve o novoj poruci
    private void notifyListeners(String gameId, ChatMessage message) {
        Map<String, ChatListener> gameListeners = listeners.get(gameId);

        if (gameListeners == null) {
            return;
        }

        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, ChatListener> entry : gameListeners.entrySet()) {
            try {
                entry.getValue().onMessageReceived(message);
            } catch (RemoteException e) {
                System.err.println("[ChatService] Listener nedostupan: " + entry.getKey());
                toRemove.add(entry.getKey());
            }
        }
        toRemove.forEach(gameListeners::remove);
    }

    // cleanup za stari chat
    public void cleanupOldChats() {
        chatHistory.entrySet().removeIf(entry -> {
            List<ChatMessage> messages = entry.getValue();
            if (messages.isEmpty()) {
                return true;
            }

            // Ukloni chat ako je posljednja poruka starija od 2 sata
            ChatMessage lastMessage = messages.get(messages.size() - 1);
            return java.time.Duration.between(lastMessage.getTimestamp(),
                    LocalDateTime.now()).toHours() > 2;
        });
    }


    // stats za test
    public String getStatistics() {
        int totalMessages = chatHistory.values().stream()
                .mapToInt(List::size)
                .sum();
        int totalListeners = listeners.values().stream()
                .mapToInt(Map::size)
                .sum();

        return String.format("Aktivnih chatova: %d, Ukupno poruka: %d, Aktivnih listenera: %d",
                chatHistory.size(), totalMessages, totalListeners);
    }
}