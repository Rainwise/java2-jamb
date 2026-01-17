package hr.ipicek.jamb.logging;

import hr.ipicek.jamb.model.Move;
import hr.ipicek.jamb.util.Logger;

import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;


// thread za serijalizaciju move objekta u binarnu datoteku
// blockingQuere za threadSafe komunikaciju, reentrantLock za sinkronizirani file access.
public class MoveLogger extends Thread {

    private final BlockingQueue<Move> moveQueue;
    private final File logFile;
    private final ReentrantLock fileLock;
    private volatile boolean running;

    public MoveLogger(String gameId) {
        this.moveQueue = new LinkedBlockingQueue<>();
        this.logFile = new File("logs/moves_" + gameId + ".dat");
        this.fileLock = new ReentrantLock();
        this.running = true;

        setName("MoveLogger-" + gameId);
        setDaemon(true);

        // Create logs directory if it doesn't exist
        File logsDir = new File("logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }

        Logger.info("MoveLogger", "Inicijaliziran za file: " + logFile.getAbsolutePath());
    }

    // dodaj potez u queue za serijalizaciju
    public void logMove(Move move) {
        try {
            moveQueue.put(move);
            Logger.debug("MoveLogger", "Move dodan u queue: " + move.getDescription());
        } catch (InterruptedException e) {
            Logger.error("MoveLogger", "Greška pri dodavanju move u queue", e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
        Logger.info("MoveLogger", "Nit pokrenuta");

        while (running || !moveQueue.isEmpty()) {
            try {
                // Wait for move with timeout to check running flag
                Move move = moveQueue.poll(500, TimeUnit.MILLISECONDS);

                if (move != null) {
                    serializeMove(move);
                }

            } catch (InterruptedException e) {
                Logger.warn("MoveLogger", "Nit prekinuta");
                Thread.currentThread().interrupt();
                break;
            }
        }

        Logger.info("MoveLogger", "Nit završila");
    }

    // serijaliziraj potez
    private void serializeMove(Move move) {
        fileLock.lock();
        try {
            // Read existing moves
            ObjectInputStream ois = null;
            var existingMoves = new java.util.ArrayList<Move>();

            if (logFile.exists() && logFile.length() > 0) {
                try {
                    ois = new ObjectInputStream(new FileInputStream(logFile));
                    while (true) {
                        try {
                            Move m = (Move) ois.readObject();
                            existingMoves.add(m);
                        } catch (EOFException e) {
                            break; // End of file
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    Logger.error("MoveLogger", "Greška pri čitanju postojećih moves", e);
                } finally {
                    if (ois != null) {
                        try {
                            ois.close();
                        } catch (IOException e) {
                            // Ignore
                        }
                    }
                }
            }

            // Add new move
            existingMoves.add(move);

            // Write all moves back to file
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(logFile))) {
                for (Move m : existingMoves) {
                    oos.writeObject(m);
                }
                oos.flush();
                Logger.debug("MoveLogger", "Move serijaliziran: " + move);
            }

        } catch (IOException e) {
            Logger.error("MoveLogger", "Greška pri serijalizaciji move", e);
        } finally {
            fileLock.unlock();
        }
    }


    public ReentrantLock getFileLock() {
        return fileLock;
    }
    public File getLogFile() {
        return logFile;
    }
    public void shutdown() {
        Logger.info("MoveLogger", "Zaustavljanje...");
        running = false;

        // Wait for thread to finish
        try {
            join(2000); // Wait max 2 seconds
        } catch (InterruptedException e) {
            Logger.warn("MoveLogger", "Prekid čekanja na završetak");
            Thread.currentThread().interrupt();
        }
    }
    public int getPendingMovesCount() {
        return moveQueue.size();
    }
}