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
                 "SELECT theme, auto_login, notifications_enabled, dark_overlay, zoom_level, auto_check_updates, notify_prereleases, open_website_first, dismissed_version FROM preferences WHERE id = 1")) {
            ResultSet rs = statement.executeQuery();
            if (!rs.next()) {
                return new UserPreferences("LIGHT", true, true, false, 1.0, true, false, true, null);
            }
            
            // Handle missing columns gracefully for existing DBs before migration finishes
            boolean autoCheck = true;
            boolean notifyPre = false;
            boolean openWeb = true;
            String dismissed = null;
            
            try {
                autoCheck = rs.getInt("auto_check_updates") == 1;
                notifyPre = rs.getInt("notify_prereleases") == 1;
                openWeb = rs.getInt("open_website_first") == 1;
                dismissed = rs.getString("dismissed_version");
            } catch (SQLException ignored) {
                // Ignore missing columns on first run
            }

            return new UserPreferences(
                rs.getString("theme"),
                rs.getInt("auto_login") == 1,
                rs.getInt("notifications_enabled") == 1,
                rs.getInt("dark_overlay") == 1,
                rs.getDouble("zoom_level"),
                autoCheck,
                notifyPre,
                openWeb,
                dismissed
            );
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to load preferences.", ex);
        }
    }

    public void savePreferences(UserPreferences preferences) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                 UPDATE preferences
                 SET theme = ?, auto_login = ?, notifications_enabled = ?, dark_overlay = ?, zoom_level = ?, auto_check_updates = ?, notify_prereleases = ?, open_website_first = ?, dismissed_version = ?
                 WHERE id = 1
                 """)) {
            statement.setString(1, preferences.getTheme());
            statement.setInt(2, preferences.isAutoLogin() ? 1 : 0);
            statement.setInt(3, preferences.isNotificationsEnabled() ? 1 : 0);
            statement.setInt(4, preferences.isDarkOverlay() ? 1 : 0);
            statement.setDouble(5, preferences.getZoomLevel());
            statement.setInt(6, preferences.isAutoCheckUpdates() ? 1 : 0);
            statement.setInt(7, preferences.isNotifyPreReleases() ? 1 : 0);
            statement.setInt(8, preferences.isOpenWebsiteFirst() ? 1 : 0);
            statement.setString(9, preferences.getDismissedVersion());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to save preferences.", ex);
        }
    }
}
