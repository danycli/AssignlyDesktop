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
                    dark_overlay INTEGER NOT NULL DEFAULT 0,
                    auto_check_updates INTEGER NOT NULL DEFAULT 1,
                    notify_prereleases INTEGER NOT NULL DEFAULT 0,
                    open_website_first INTEGER NOT NULL DEFAULT 1,
                    dismissed_version TEXT
                )
                """);
            
            // Migrations for existing local databases
            try { statement.executeUpdate("ALTER TABLE preferences ADD COLUMN auto_check_updates INTEGER NOT NULL DEFAULT 1"); } catch (SQLException ignored) {}
            try { statement.executeUpdate("ALTER TABLE preferences ADD COLUMN notify_prereleases INTEGER NOT NULL DEFAULT 0"); } catch (SQLException ignored) {}
            try { statement.executeUpdate("ALTER TABLE preferences ADD COLUMN open_website_first INTEGER NOT NULL DEFAULT 1"); } catch (SQLException ignored) {}
            try { statement.executeUpdate("ALTER TABLE preferences ADD COLUMN dismissed_version TEXT"); } catch (SQLException ignored) {}

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
                CREATE TABLE IF NOT EXISTS session_cookies (
                    name TEXT NOT NULL,
                    value TEXT NOT NULL,
                    domain TEXT NOT NULL,
                    path TEXT NOT NULL,
                    expires_at INTEGER NOT NULL,
                    secure INTEGER NOT NULL,
                    http_only INTEGER NOT NULL,
                    PRIMARY KEY (name, domain, path)
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
