package com.onlinediscussion.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Reads the database connection settings.
 *
 * <p>The settings come from, in order of priority: a {@code db.properties} file in the
 * working directory, environment variables, and finally the built-in defaults. The defaults
 * match the settings that the existing Node.js server uses.
 */
public final class Config {

    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 3306;
    private static final String DEFAULT_DATABASE = "discussion_tool";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String password;

    private Config(String host, int port, String database, String user, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    /** Loads the configuration from {@code db.properties} and the environment. */
    public static Config load() {
        Properties properties = new Properties();
        Path file = Path.of("db.properties");
        if (Files.exists(file)) {
            try (var reader = Files.newBufferedReader(file)) {
                properties.load(reader);
            } catch (IOException ignored) {
                // Fall through to the environment and the defaults.
            }
        }
        String host = pick(properties.getProperty("db.host"), System.getenv("MYSQL_HOST"), DEFAULT_HOST);
        int port = parsePort(pick(properties.getProperty("db.port"), System.getenv("MYSQL_PORT"), null),
                DEFAULT_PORT);
        String database = pick(properties.getProperty("db.name"), System.getenv("MYSQL_DATABASE"), DEFAULT_DATABASE);
        String user = pick(properties.getProperty("db.user"), System.getenv("MYSQL_USER"), DEFAULT_USER);
        String password = pick(properties.getProperty("db.password"), System.getenv("MYSQL_PASSWORD"), DEFAULT_PASSWORD);
        return new Config(host, port, database, user, password);
    }

    private static String pick(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return "";
    }

    private static int parsePort(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Splits a command line into its arguments, honouring double quotes. */
    public static List<String> tokenize(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean started = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                started = true;
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (started) {
                    tokens.add(current.toString());
                    current.setLength(0);
                    started = false;
                }
            } else {
                current.append(c);
                started = true;
            }
        }
        if (started) {
            tokens.add(current.toString());
        }
        return tokens;
    }
}
