package hr.ipicek.jamb.logging;

import hr.ipicek.jamb.model.Move;
import hr.ipicek.jamb.util.Logger;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.*;
import java.util.concurrent.locks.ReentrantLock;



// poseban thread koji periodicki cita potez iz binarne datoteke i stavlja ga na client ui
public class MoveDisplay extends Thread {

    private final File logFile;
    private final ReentrantLock fileLock;
    private final int intervalSeconds;
    private volatile boolean running;

    // Observable properties for UI binding
    private final StringProperty lastMovePlayer = new SimpleStringProperty("---");
    private final StringProperty lastMoveAction = new SimpleStringProperty("Čekanje na potez...");
    private final StringProperty lastMoveTime = new SimpleStringProperty("--:--:--");

    public MoveDisplay(File logFile, ReentrantLock fileLock, int intervalSeconds) {
        this.logFile = logFile;
        this.fileLock = fileLock;
        this.intervalSeconds = intervalSeconds;
        this.running = true;

        setName("MoveDisplay");
        setDaemon(true);

        Logger.info("MoveDisplay", "Inicijaliziran, interval: " + intervalSeconds + "s");
    }

    @Override
    public void run() {
        Logger.info("MoveDisplay", "Nit pokrenuta");

        while (running) {
            try {
                // Read and display last move
                readAndDisplayLastMove();

                // Sleep for interval
                Thread.sleep(intervalSeconds * 1000L);

            } catch (InterruptedException e) {
                Logger.warn("MoveDisplay", "Nit prekinuta");
                Thread.currentThread().interrupt();
                break;
            }
        }

        Logger.info("MoveDisplay", "Nit završila");
    }

    private void readAndDisplayLastMove() {
        // Acquire lock to synchronize with MoveLogger
        fileLock.lock();
        try {
            if (!logFile.exists() || logFile.length() == 0) {
                updateUIProperties(null);
                return;
            }

            Move lastMove = null;

            // Read all moves to get the last one
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(logFile))) {
                while (true) {
                    try {
                        lastMove = (Move) ois.readObject();
                    } catch (EOFException e) {
                        break; // End of file - lastMove now contains the last move
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                Logger.error("MoveDisplay", "Greška pri čitanju move file", e);
                return;
            }

            // Update UI on JavaFX thread
            if (lastMove != null) {
                updateUIProperties(lastMove);
                Logger.debug("MoveDisplay", "Prikazan potez: " + lastMove);
            }

        } finally {
            fileLock.unlock();
        }
    }

    // ažurira java fx ui
    private void updateUIProperties(Move move) {
        Platform.runLater(() -> {
            if (move == null) {
                lastMovePlayer.set("---");
                lastMoveAction.set("Čekanje na potez...");
                lastMoveTime.set("--:--:--");
            } else {
                lastMovePlayer.set(move.getPlayerName());
                lastMoveAction.set(move.getDescription());
                lastMoveTime.set(move.getFormattedTime());
            }
        });
    }


    // zaustavi display thread
    public void shutdown() {
        Logger.info("MoveDisplay", "Zaustavljanje...");
        running = false;
        interrupt(); // Wake up from sleep

        try {
            join(1000); // Wait max 1 second
        } catch (InterruptedException e) {
            Logger.warn("MoveDisplay", "Prekid čekanja na završetak");
            Thread.currentThread().interrupt();
        }
    }

    // Property getters for UI binding
    public StringProperty lastMovePlayerProperty() {
        return lastMovePlayer;
    }

    public StringProperty lastMoveActionProperty() {
        return lastMoveAction;
    }

    public StringProperty lastMoveTimeProperty() {
        return lastMoveTime;
    }

    public String getLastMovePlayer() {
        return lastMovePlayer.get();
    }

    public String getLastMoveAction() {
        return lastMoveAction.get();
    }

    public String getLastMoveTime() {
        return lastMoveTime.get();
    }
}