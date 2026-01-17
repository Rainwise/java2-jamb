package hr.ipicek.jamb.network.protocol;

public enum MessageType {
    PLAYER_JOINED,          // Igrač se pridružio lobby-u
    PLAYER_INFO,            // Informacije o igraču (ime, spremnost)
    PLAYER_READY,           // Igrač je spreman za početak igre
    PLAYER_NOT_READY,       // Igrač nije spreman
    LOBBY_UPDATE,           // Update liste igrača u lobby-u
    GAME_START,             // Igra počinje (server šalje svima)
    GAME_STATE_SYNC,        // Puna sinkronizacija stanja igre
    GAME_STATE_UPDATE,      // Incremental state update (nakon svake akcije)
    INITIAL_GAME_STATE,     // Početno stanje igre (ko prvi ide, itd.)
    ROLL_REQUEST,           // Igrač želi baciti kockice (request od klijenta)
    DICE_ROLLED,            // Kockice su bačene (response od servera sa rezultatom)
    DICE_HOLD_TOGGLE,       // Toggle hold stanja jedne kockice
    DICE_STATE_UPDATE,      // Update svih kockica (vrijednosti + held status)
    SCORE_PREVIEW_REQUEST,  // Request za preview bodova u kategoriji
    SCORE_PREVIEW_RESPONSE, // Response sa preview bodovima
    SCORE_APPLY_REQUEST,    // Igrač želi upisati rezultat
    SCORE_APPLIED,          // Rezultat je upisan (potvrda)
    SCORE_UPDATE,           // Update tablice rezultata
    TURN_START,             // Početak novog turn-a
    TURN_CHANGE,            // Promjena igrača na potezu
    NEXT_PLAYER,            // Sljedeći igrač na potezu
    GAME_OVER,              // Igra je završila
    WINNER_ANNOUNCEMENT,    // Objava pobjednika
    CHAT_MESSAGE,           // Chat poruka
    CHAT_NOTIFICATION,      // System notifikacija u chatu
    PING,                   // Keep-alive ping
    PONG,                   // Keep-alive odgovor
    DISCONNECT,             // Igrač se isključio
    RECONNECT_REQUEST,      // Pokušaj ponovnog spajanja
    ERROR,                  // Opća greška
    INVALID_MOVE,           // Nevažeći potez
    TIMEOUT,                // Timeout (igrač nije igrao na vrijeme)
    ACKNOWLEDGE,            // Potvrda primitka poruke
    SYNC_REQUEST,           // Request za sinkronizaciju
    SYNC_RESPONSE           // Response sa trenutnim stanjem
}