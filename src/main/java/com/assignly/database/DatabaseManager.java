package com.assignly.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private final String jdbcUrl;

    public DatabaseManager(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    public void initializeSchema() {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY CHECK (id = 1),
                    registration_no TEXT NOT NULL,
                    encrypted_password TEXT,
                    remember_me INTEGER NOT NULL DEFAULT 0,
                    updated_at TEXT NOT NULL
                )
                """);
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS assignments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    description TEXT,
                    due_date TEXT,
                    status TEXT,
                    updated_at TEXT NOT NULL
                )
                """);
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS announcements (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    content TEXT,
                    publish_date TEXT,
                    updated_at TEXT NOT NULL
                )
                """);
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS preferences (
                    id INTEGER PRIMARY KEY CHECK (id = 1),
                    theme TEXT NOT NULL DEFAULT 'LIGHT',
                    auto_login INTEGER NOT NULL DEFAULT 1,
                    notifications_enabled INTEGER NOT NULL DEFAULT 1,
                    zoom_level REAL NOT NULL DEFAULT 1.0,
                    dark_overlay INTEGER NOT NULL DEFAULT 0
                )
                """);
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS notifications (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    message TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    is_read INTEGER NOT NULL DEFAULT 0
                )
                """);
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS academic_cache (
                    category TEXT PRIMARY KEY,
                    value TEXT NOT NULL,
                    captured_at TEXT NOT NULL
                )
                """);
            statement.executeUpdate("""
                INSERT INTO preferences (id, theme, auto_login, notifications_enabled, zoom_level, dark_overlay)
                VALUES (1, 'LIGHT', 1, 1, 1.0, 0)
                ON CONFLICT(id) DO NOTHING
                """);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to initialize database schema.", ex);
        }
    }

    public void shutdown() {
        // No long-lived connection to close; method kept for lifecycle symmetry.
    }
}
