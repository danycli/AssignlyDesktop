package com.assignly.service;

import com.assignly.database.DatabaseManager;
import com.assignly.model.UserPreferences;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PreferencesService {
    private final DatabaseManager databaseManager;

    public PreferencesService(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public UserPreferences loadPreferences() {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT theme, auto_login, notifications_enabled FROM preferences WHERE id = 1")) {
            ResultSet rs = statement.executeQuery();
            if (!rs.next()) {
                return new UserPreferences("LIGHT", true, true);
            }
            return new UserPreferences(
                rs.getString("theme"),
                rs.getInt("auto_login") == 1,
                rs.getInt("notifications_enabled") == 1
            );
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to load preferences.", ex);
        }
    }

    public void savePreferences(UserPreferences preferences) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                 UPDATE preferences
                 SET theme = ?, auto_login = ?, notifications_enabled = ?
                 WHERE id = 1
                 """)) {
            statement.setString(1, preferences.getTheme());
            statement.setInt(2, preferences.isAutoLogin() ? 1 : 0);
            statement.setInt(3, preferences.isNotificationsEnabled() ? 1 : 0);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to save preferences.", ex);
        }
    }
}
