package hr.ipicek.jamb.logging;

import hr.ipicek.jamb.model.Move;
import hr.ipicek.jamb.util.Logger;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

// XML DOM API imports
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.*;
import java.util.concurrent.locks.ReentrantLock;


public class MoveDisplay extends Thread {

    private final File xmlFile;  // Changed: now XML file!
    private final ReentrantLock fileLock;
    private final int intervalSeconds;
    private volatile boolean running;

    // Observable properties for UI binding
    private final StringProperty lastMovePlayer = new SimpleStringProperty("---");
    private final StringProperty lastMoveAction = new SimpleStringProperty("Čekanje na potez...");
    private final StringProperty lastMoveTime = new SimpleStringProperty("--:--:--");

    public MoveDisplay(File xmlFile, ReentrantLock fileLock, int intervalSeconds) {
        this.xmlFile = xmlFile;
        this.fileLock = fileLock;
        this.intervalSeconds = intervalSeconds;
        this.running = true;

        setName("MoveDisplay-XML");
        setDaemon(true);

        Logger.info("MoveDisplay", "Initialized (XML mode), interval: " + intervalSeconds + "s");
        Logger.info("MoveDisplay", "Reading from: " + xmlFile.getName());
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
        fileLock.lock();
        try {
            if (!xmlFile.exists() || xmlFile.length() == 0) {
                updateUIProperties(null);
                return;
            }

            Move lastMove = readLastMoveFromXML();

            if (lastMove != null) {
                updateUIProperties(lastMove);
                Logger.debug("MoveDisplay", "Displayed move from XML: " + lastMove);
            }

        } finally {
            fileLock.unlock();
        }
    }

    private Move readLastMoveFromXML() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList moveNodes = doc.getElementsByTagName("GameMove");

            if (moveNodes.getLength() == 0) {
                Logger.debug("MoveDisplay", "XML is empty");
                return null;
            }

            // Get LAST element
            Element lastMoveElement = (Element) moveNodes.item(moveNodes.getLength() - 1);

            // Extract data
            String playerName = getElementText(lastMoveElement, "PlayerName");
            String details = getElementText(lastMoveElement, "Details");
            String timestamp = getElementText(lastMoveElement, "Timestamp");

            Logger.debug("MoveDisplay", String.format("Read: %s - %s - %s", playerName, details, timestamp));

            return new Move(playerName, details, timestamp);

        } catch (Exception e) {
            Logger.error("MoveDisplay", "Error reading XML: " + e.getMessage());
            return null;
        }
    }

    private String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return "";
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