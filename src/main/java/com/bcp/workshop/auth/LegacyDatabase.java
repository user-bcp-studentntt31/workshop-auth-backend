package com.bcp.workshop.auth;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.mindrot.jbcrypt.BCrypt;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

@ApplicationScoped
public class LegacyDatabase {
    private static final String DB_URL = System.getenv().getOrDefault("AUTH_DB_URL", "jdbc:sqlite:./data/workshop.db");

    @PostConstruct
    void initialize() {
        try {
            Files.createDirectories(Path.of("data"));
            try (Connection connection = DriverManager.getConnection(DB_URL); Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS legacy_users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT NOT NULL, display_name TEXT NOT NULL, role TEXT NOT NULL)");
            }
            try (Connection connection = DriverManager.getConnection(DB_URL)) {
                seedUser(connection, "student", "Workshop123!", "Workshop Student", "STUDENT");
                seedUser(connection, "trainer", "Trainer123!", "Workshop Trainer", "TRAINER");
                migrateLegacyPasswords(connection);
            }
        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    public UserRecord findByCredentials(String username, String password) {
        String sql = "SELECT id, username, password, display_name, role FROM legacy_users WHERE username = ?";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                String passwordHash = result.getString("password");
                if (passwordHash != null && BCrypt.checkpw(password, passwordHash)) {
                    return new UserRecord(result.getLong("id"), result.getString("username"), result.getString("display_name"), result.getString("role"));
                }
            }
        } catch (Exception error) {
            error.printStackTrace();
        }
        return null;
    }

    private void seedUser(Connection connection, String username, String plainPassword, String displayName, String role) throws Exception {
        String sql = "INSERT OR IGNORE INTO legacy_users(username,password,display_name,role) VALUES (?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, BCrypt.hashpw(plainPassword, BCrypt.gensalt()));
            statement.setString(3, displayName);
            statement.setString(4, role);
            statement.executeUpdate();
        }
    }

    private void migrateLegacyPasswords(Connection connection) throws Exception {
        String selectSql = "SELECT id, password FROM legacy_users";
        String updateSql = "UPDATE legacy_users SET password = ? WHERE id = ?";
        try (Statement select = connection.createStatement();
             ResultSet result = select.executeQuery(selectSql);
             PreparedStatement update = connection.prepareStatement(updateSql)) {
            while (result.next()) {
                String currentPassword = result.getString("password");
                if (currentPassword != null && !currentPassword.startsWith("$2")) {
                    update.setString(1, BCrypt.hashpw(currentPassword, BCrypt.gensalt()));
                    update.setLong(2, result.getLong("id"));
                    update.addBatch();
                }
            }
            update.executeBatch();
        }
    }
}
