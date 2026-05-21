package com.assignly.service;

import com.assignly.database.DatabaseManager;
import com.assignly.model.User;
import com.assignly.security.EncryptionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

public class CredentialManager {
    private final DatabaseManager databaseManager;

    public CredentialManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Optional<User> getStoredUser() {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT id, registration_no, encrypted_password, remember_me FROM users WHERE id = 1")) {
            ResultSet rs = statement.executeQuery();
            if (!rs.next()) {
                return Optional.empty();
            }
            return Optional.of(new User(
                rs.getLong("id"),
                rs.getString("registration_no"),
                rs.getString("encrypted_password"),
                rs.getInt("remember_me") == 1
            ));
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to read stored user.", ex);
        }
    }

    public Optional<String> getDecryptedPassword(User user) {
        if (user.getEncryptedPassword() == null || user.getEncryptedPassword().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(EncryptionUtil.decrypt(user.getEncryptedPassword()));
    }

    public void saveCredentials(String registrationNo, String rawPassword, boolean rememberMe) {
        if (registrationNo == null || registrationNo.isBlank()) {
            throw new IllegalArgumentException("Registration number is required.");
        }
        String encrypted = null;
        if (rememberMe) {
            encrypted = EncryptionUtil.encrypt(rawPassword);
        }
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                 INSERT INTO users (id, registration_no, encrypted_password, remember_me, updated_at)
                 VALUES (1, ?, ?, ?, ?)
                 ON CONFLICT(id) DO UPDATE SET
                    registration_no = excluded.registration_no,
                    encrypted_password = excluded.encrypted_password,
                    remember_me = excluded.remember_me,
                    updated_at = excluded.updated_at
                 """)) {
            statement.setString(1, registrationNo.trim());
            statement.setString(2, encrypted);
            statement.setInt(3, rememberMe ? 1 : 0);
            statement.setString(4, LocalDateTime.now().toString());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to save user credentials.", ex);
        }
    }

    public boolean hasRememberedCredentials() {
        Optional<User> user = getStoredUser();
        return user.filter(User::isRememberMe)
            .flatMap(this::getDecryptedPassword)
            .isPresent();
    }

    public void clearRememberMe() {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "UPDATE users SET remember_me = 0, encrypted_password = NULL, updated_at = ? WHERE id = 1")) {
            statement.setString(1, LocalDateTime.now().toString());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to clear remembered credentials.", ex);
        }
    }

    public void clearAllCredentials() {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM users WHERE id = 1")) {
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to clear credentials.", ex);
        }
    }
}
