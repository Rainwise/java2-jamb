package hr.ipicek.jamb.logging;

import hr.ipicek.jamb.model.Move;
import hr.ipicek.jamb.util.Logger;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;  // ← DODANO


public class XMLMoveLogger extends Thread {

    private final BlockingQueue<Move> moveQueue;
    private final File xmlFile;
    private final String gameId;
    private volatile boolean running;
    private final ReentrantLock fileLock;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public XMLMoveLogger(String gameId) {
        this.gameId = gameId;
        this.moveQueue = new LinkedBlockingQueue<>();
        this.running = true;
        this.fileLock = new ReentrantLock();  // ← DODANO
        this.setDaemon(true);

        File logsDir = new File("logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }

        this.xmlFile = new File(logsDir, "moves_" + gameId + ".xml");
        initializeXMLFile();

        Logger.info("XMLMoveLogger", "Initialized for game: " + gameId);
    }

    private void initializeXMLFile() {
        if (xmlFile.exists()) {
            Logger.info("XMLMoveLogger", "XML file already exists: " + xmlFile.getName());
            return;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element root = doc.createElement("GameMoves");
            doc.appendChild(root);

            writeDocumentWithDTD(doc);

            Logger.info("XMLMoveLogger", "Initialized XML file with DTD reference");

        } catch (Exception e) {
            Logger.error("XMLMoveLogger", "Error initializing XML: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void logMove(Move move) {
        try {
            moveQueue.put(move);
        } catch (InterruptedException e) {
            Logger.error("XMLMoveLogger", "Error adding move to queue: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
        Logger.info("XMLMoveLogger", "Thread started");

        while (running || !moveQueue.isEmpty()) {
            try {
                Move move = moveQueue.poll(500, TimeUnit.MILLISECONDS);

                if (move != null) {
                    // ← DODANO: Lock prije pisanja
                    fileLock.lock();
                    try {
                        appendMoveToXML(move);
                    } finally {
                        fileLock.unlock();
                    }
                }

            } catch (InterruptedException e) {
                Logger.error("XMLMoveLogger", "Thread interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Logger.error("XMLMoveLogger", "Error processing move: " + e.getMessage());
                e.printStackTrace();
            }
        }

        Logger.info("XMLMoveLogger", "Thread finished");
    }

    private void appendMoveToXML(Move move) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);
        factory.setIgnoringElementContentWhitespace(true);

        DocumentBuilder builder = factory.newDocumentBuilder();

        builder.setErrorHandler(new org.xml.sax.ErrorHandler() {
            @Override
            public void warning(SAXParseException e) {
                Logger.warn("XMLMoveLogger", "DTD Warning: " + e.getMessage());
            }

            @Override
            public void error(SAXParseException e) {
                Logger.error("XMLMoveLogger", "DTD Error: " + e.getMessage());
            }

            @Override
            public void fatalError(SAXParseException e) throws SAXException {
                Logger.error("XMLMoveLogger", "DTD Fatal: " + e.getMessage());
                throw e;
            }
        });

        Document doc = builder.parse(xmlFile);
        doc.getDocumentElement().normalize();

        Element gameMoveElement = doc.createElement("GameMove");

        Element playerName = doc.createElement("PlayerName");
        playerName.setTextContent(move.getPlayerName());
        gameMoveElement.appendChild(playerName);

        Element action = doc.createElement("Action");
        action.setTextContent(move.getType().name());
        gameMoveElement.appendChild(action);

        Element details = doc.createElement("Details");
        details.setTextContent(move.getDescription());
        gameMoveElement.appendChild(details);

        Element timestamp = doc.createElement("Timestamp");
        timestamp.setTextContent(move.getFormattedTime());
        gameMoveElement.appendChild(timestamp);

        Element root = doc.getDocumentElement();
        root.appendChild(gameMoveElement);

        writeDocumentWithDTD(doc);

        Logger.debug("XMLMoveLogger", "Added move: " + move.getDescription());
    }

    private void writeDocumentWithDTD(Document doc) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "dtd/gameMoves.dtd");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(xmlFile);

        transformer.transform(source, result);
    }

    public void shutdown() {
        running = false;
        try {
            this.join(2000);
            Logger.info("XMLMoveLogger", "Shutdown complete");
        } catch (InterruptedException e) {
            Logger.error("XMLMoveLogger", "Error during shutdown: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    public File getXmlFile() {
        return xmlFile;
    }

    public ReentrantLock getFileLock() {
        return fileLock;
    }
}