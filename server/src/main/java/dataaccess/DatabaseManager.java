package dataaccess;

import java.sql.*;
import java.util.Properties;

public class DatabaseManager {
    private static String databaseName;
    private static String dbUsername;
    private static String dbPassword;
    private static String connectionUrl;

    static {
        loadPropertiesFromResources();
    }

    public static void createDatabase() throws DataAccessException {
        String urlNoDb = String.format("jdbc:mysql://%s:%d/", getHost(), getPort());
        try (Connection conn = DriverManager.getConnection(urlNoDb, dbUsername, dbPassword);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + databaseName);
        } catch (SQLException ex) {
            throw new DataAccessException("failed to create database", ex);
        }
    }

    public static Connection getConnection() throws DataAccessException {
        try {
            return DriverManager.getConnection(connectionUrl, dbUsername, dbPassword);
        } catch (SQLException ex) {
            throw new DataAccessException("failed to get connection", ex);
        }
    }

    public static void loadProperties(Properties props) {
        databaseName = props.getProperty("db.name");
        dbUsername = props.getProperty("db.user");
        dbPassword = props.getProperty("db.password");
        String host = props.getProperty("db.host");
        int port = Integer.parseInt(props.getProperty("db.port"));
        connectionUrl = String.format("jdbc:mysql://%s:%d/%s", host, port, databaseName);
    }

    public static void loadPropertiesFromResources() {
        try (var propStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("db.properties")) {
            if (propStream == null) throw new Exception("db.properties not found");
            Properties props = new Properties();
            props.load(propStream);
            loadProperties(props);
        } catch (Exception ex) {
            throw new RuntimeException("unable to process db.properties", ex);
        }
    }

    private static String getHost() {
        try {
            String afterProtocol = connectionUrl.split("//", 2)[1];
            return afterProtocol.split(":")[0];
        } catch (Exception e) {
            return "localhost";
        }
    }

    private static int getPort() {
        try {
            String afterProtocol = connectionUrl.split("//", 2)[1];
            String portPart = afterProtocol.split(":")[1];
            return Integer.parseInt(portPart.split("/")[0]);
        } catch (Exception e) {
            return 3306;
        }
    }
}
