package hr.ipicek.jamb.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public final class Logger {

    private Logger() {}

    public enum Level {
        DEBUG,   // Detailed information for debugging
        INFO,    // General informational messages
        WARNING, // Warning messages
        ERROR    // Error messages
    }

    // Configuration
    private static Level currentLevel = Level.INFO;
    private static boolean enabled = true;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");


    public static void setLevel(Level level) {
        currentLevel = level;
    }
    public static void setEnabled(boolean enable) {
        enabled = enable;
    }

    private static boolean shouldLog(Level level) {
        if (!enabled) return false;
        return level.ordinal() >= currentLevel.ordinal();
    }

    private static String format(Level level, String tag, String message) {
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        return String.format("[%s] [%s] [%s] %s", timestamp, level, tag, message);
    }

    public static void debug(String tag, String message) {
        if (shouldLog(Level.DEBUG)) {
            System.out.println(format(Level.DEBUG, tag, message));
        }
    }

    public static void debug(String tag, String format, Object... args) {
        debug(tag, String.format(format, args));
    }

    public static void info(String tag, String message) {
        if (shouldLog(Level.INFO)) {
            System.out.println(format(Level.INFO, tag, message));
        }
    }

    public static void info(String tag, String format, Object... args) {
        info(tag, String.format(format, args));
    }

    // WARNING level logging
    public static void warn(String tag, String message) {
        if (shouldLog(Level.WARNING)) {
            System.err.println(format(Level.WARNING, tag, message));
        }
    }

    public static void warn(String tag, String format, Object... args) {
        warn(tag, String.format(format, args));
    }

    // ERROR level logging
    public static void error(String tag, String message) {
        if (shouldLog(Level.ERROR)) {
            System.err.println(format(Level.ERROR, tag, message));
        }
    }

    public static void error(String tag, String message, Throwable throwable) {
        if (shouldLog(Level.ERROR)) {
            System.err.println(format(Level.ERROR, tag, message));
            if (throwable != null) {
                throwable.printStackTrace(System.err);
            }
        }
    }

    public static void error(String tag, String format, Object... args) {
        error(tag, String.format(format, args));
    }

    // Convenience methods for common tags
    public static class Network {
        private static final String TAG = "Network";

        public static void debug(String message) { Logger.debug(TAG, message); }
        public static void info(String message) { Logger.info(TAG, message); }
        public static void warn(String message) { Logger.warn(TAG, message); }
        public static void error(String message) { Logger.error(TAG, message); }
        public static void error(String message, Throwable t) { Logger.error(TAG, message, t); }
    }

    public static class Server {
        private static final String TAG = "Server";

        public static void debug(String message) { Logger.debug(TAG, message); }
        public static void info(String message) { Logger.info(TAG, message); }
        public static void warn(String message) { Logger.warn(TAG, message); }
        public static void error(String message) { Logger.error(TAG, message); }
        public static void error(String message, Throwable t) { Logger.error(TAG, message, t); }
    }

    public static class Client {
        private static final String TAG = "Client";

        public static void debug(String message) { Logger.debug(TAG, message); }
        public static void info(String message) { Logger.info(TAG, message); }
        public static void warn(String message) { Logger.warn(TAG, message); }
        public static void error(String message) { Logger.error(TAG, message); }
        public static void error(String message, Throwable t) { Logger.error(TAG, message, t); }
    }

    public static class Game {
        private static final String TAG = "Game";

        public static void debug(String message) { Logger.debug(TAG, message); }
        public static void info(String message) { Logger.info(TAG, message); }
        public static void warn(String message) { Logger.warn(TAG, message); }
        public static void error(String message) { Logger.error(TAG, message); }
        public static void error(String message, Throwable t) { Logger.error(TAG, message, t); }
    }
}