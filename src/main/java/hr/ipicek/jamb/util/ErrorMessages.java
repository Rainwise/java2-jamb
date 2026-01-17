package hr.ipicek.jamb.util;


public final class ErrorMessages {

    private ErrorMessages() {}

    // Network Connection Errors
    public static final String NETWORK_CONNECTION_FAILED = "Neuspjelo spajanje na mrežu";
    public static final String NETWORK_CONNECTION_LOST = "Veza sa serverom prekinuta";
    public static final String NETWORK_TIMEOUT = "Vrijeme spajanja isteklo";
    public static final String NETWORK_HOST_UNREACHABLE = "Server nije dostupan";

    // RMI Errors
    public static final String RMI_CONNECTION_REFUSED = "RMI server nije pokrenut. Molimo pokrenite RMI server prvo.";
    public static final String RMI_REGISTRY_ERROR = "Greška pri spajanju na RMI registry";
    public static final String RMI_SERVICE_NOT_FOUND = "RMI servis nije pronađen";

    // Game Server Errors
    public static final String SERVER_START_FAILED = "Neuspjelo pokretanje servera";
    public static final String SERVER_PORT_IN_USE = "Port je već zauzet. Pokušajte drugi port.";
    public static final String SERVER_STOP_FAILED = "Greška pri zaustavljanju servera";

    // Game Client Errors
    public static final String CLIENT_CONNECTION_FAILED = "Neuspjelo spajanje na igru";
    public static final String CLIENT_DISCONNECT_ERROR = "Greška pri odvajanju";
    public static final String CLIENT_SEND_ERROR = "Greška pri slanju podataka";

    // Game Logic Errors
    public static final String GAME_NOT_INITIALIZED = "Igra nije inicijalizirana";
    public static final String INVALID_MOVE = "Neispravan potez";
    public static final String NOT_YOUR_TURN = "Nije vaš red";
    public static final String GAME_ALREADY_STARTED = "Igra je već pokrenuta";
    public static final String NOT_ENOUGH_PLAYERS = "Nedovoljno igrača za početak igre";

    // File Errors
    public static final String FILE_LOAD_ERROR = "Greška pri učitavanju datoteke";
    public static final String FILE_SAVE_ERROR = "Greška pri spremanju datoteke";
    public static final String FXML_LOAD_ERROR = "Greška pri učitavanju ekrana";

    // Generic Errors
    public static final String UNEXPECTED_ERROR = "Neočekivana greška";
    public static final String OPERATION_CANCELLED = "Operacija otkazana";

    // Success Messages
    public static final String CONNECTION_SUCCESSFUL = "Uspješno spojeno!";
    public static final String SERVER_STARTED = "Server pokrenut na portu %d";
    public static final String GAME_CREATED = "Igra uspješno kreirana";
    public static final String GAME_JOINED = "Pridružili ste se igri";

    // Helper method for formatted messages
    public static String format(String message, Object... args) {
        return String.format(message, args);
    }
}