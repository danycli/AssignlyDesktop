package com.assignly.service;

import com.assignly.database.DatabaseManager;
import com.assignly.model.Announcement;
import com.assignly.model.Assignment;
import com.assignly.model.PortalSnapshot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DataCacheService {
    private final DatabaseManager databaseManager;

    public DataCacheService(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public List<Assignment> getAssignments() {
        List<Assignment> assignments = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT id, title, description, due_date, status FROM assignments ORDER BY due_date ASC, id DESC");
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                assignments.add(new Assignment(
                    rs.getLong("id"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getString("due_date"),
                    rs.getString("status")
                ));
            }
            return assignments;
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to read assignments cache.", ex);
        }
    }

    public List<Announcement> getAnnouncements() {
        List<Announcement> announcements = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT id, title, content, publish_date FROM announcements ORDER BY id DESC");
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                announcements.add(new Announcement(
                    rs.getLong("id"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getString("publish_date")
                ));
            }
            return announcements;
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to read announcements cache.", ex);
        }
    }

    public List<String> getNotifications() {
        List<String> notifications = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT title, message FROM notifications ORDER BY id DESC LIMIT 25");
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                notifications.add(rs.getString("title") + " — " + rs.getString("message"));
            }
            return notifications;
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to read notifications cache.", ex);
        }
    }

    public record NotificationEntry(long id, String title, String message, String createdAt, boolean isRead) {}

    public List<NotificationEntry> getDetailedNotifications() {
        List<NotificationEntry> notifications = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT id, title, message, created_at, is_read FROM notifications ORDER BY id DESC LIMIT 50");
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                notifications.add(new NotificationEntry(
                    rs.getLong("id"),
                    rs.getString("title"),
                    rs.getString("message"),
                    rs.getString("created_at"),
                    rs.getInt("is_read") == 1
                ));
            }
            return notifications;
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to read detailed notifications cache.", ex);
        }
    }

    public void clearNotifications() {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM notifications")) {
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to clear notifications cache.", ex);
        }
    }

    public Optional<String> getAcademicValue(String category) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT value FROM academic_cache WHERE category = ?")) {
            statement.setString(1, category);
            ResultSet rs = statement.executeQuery();
            if (!rs.next()) {
                return Optional.empty();
            }
            return Optional.of(rs.getString("value"));
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to read academic cache value.", ex);
        }
    }

    public void cacheHtml(String url, String html) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                 INSERT INTO academic_cache (category, value, captured_at)
                 VALUES (?, ?, ?)
                 ON CONFLICT(category) DO UPDATE SET value = excluded.value, captured_at = excluded.captured_at
                 """)) {
            statement.setString(1, "html_" + url);
            statement.setString(2, html);
            statement.setString(3, LocalDateTime.now().toString());
            statement.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Failed to cache HTML for " + url);
        }
    }

    public Optional<String> getCachedHtml(String url) {
        return getAcademicValue("html_" + url);
    }

    public Optional<java.time.LocalDateTime> getCacheTimestamp(String url) {
        String query = "SELECT captured_at FROM academic_cache WHERE category = ?";
        try (java.sql.Connection connection = databaseManager.getConnection();
             java.sql.PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, "html_" + url);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String capturedAt = rs.getString("captured_at");
                    if (capturedAt != null) {
                        return Optional.of(java.time.LocalDateTime.parse(capturedAt));
                    }
                }
            }
        } catch (java.sql.SQLException e) {
            System.err.println("Failed to read cache timestamp for " + url + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<java.time.LocalDateTime> getAnnouncementsLastUpdated() {
        String query = "SELECT MAX(updated_at) as max_val FROM announcements";
        try (java.sql.Connection connection = databaseManager.getConnection();
             java.sql.PreparedStatement pstmt = connection.prepareStatement(query);
             java.sql.ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                String maxVal = rs.getString("max_val");
                if (maxVal != null && !maxVal.isBlank()) {
                    try {
                        return Optional.of(java.time.LocalDateTime.parse(maxVal));
                    } catch (Exception ignored) {}
                }
            }
        } catch (java.sql.SQLException e) {
            System.err.println("Failed to read announcements last updated: " + e.getMessage());
        }
        
        // Fallbacks
        Optional<java.time.LocalDateTime> ts = getCacheTimestamp("Dashboard.aspx");
        if (ts.isPresent()) return ts;
        return getCacheTimestamp("Summary.aspx");
    }

    public void saveSnapshot(PortalSnapshot snapshot) {
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement deleteAssignments = connection.prepareStatement("DELETE FROM assignments");
                 PreparedStatement deleteAnnouncements = connection.prepareStatement("DELETE FROM announcements");
                 PreparedStatement insertAssignment = connection.prepareStatement("""
                     INSERT INTO assignments (title, description, due_date, status, updated_at)
                     VALUES (?, ?, ?, ?, ?)
                     """);
                 PreparedStatement insertAnnouncement = connection.prepareStatement("""
                     INSERT INTO announcements (title, content, publish_date, updated_at)
                     VALUES (?, ?, ?, ?)
                     """);
                 PreparedStatement insertNotification = connection.prepareStatement("""
                     INSERT INTO notifications (title, message, created_at)
                     VALUES (?, ?, ?)
                     """);
                 PreparedStatement upsertAcademic = connection.prepareStatement("""
                     INSERT INTO academic_cache (category, value, captured_at)
                     VALUES (?, ?, ?)
                     ON CONFLICT(category) DO UPDATE SET value = excluded.value, captured_at = excluded.captured_at
                     """)) {

                deleteAssignments.executeUpdate();
                deleteAnnouncements.executeUpdate();

                String now = LocalDateTime.now().toString();
                for (Assignment assignment : snapshot.toAssignments()) {
                    insertAssignment.setString(1, normalize(assignment.getTitle(), "Untitled assignment"));
                    insertAssignment.setString(2, normalize(assignment.getDescription(), ""));
                    insertAssignment.setString(3, normalize(assignment.getDueDate(), "N/A"));
                    insertAssignment.setString(4, normalize(assignment.getStatus(), "Pending"));
                    insertAssignment.setString(5, now);
                    insertAssignment.addBatch();
                }
                insertAssignment.executeBatch();

                for (Announcement announcement : snapshot.toAnnouncements()) {
                    insertAnnouncement.setString(1, normalize(announcement.getTitle(), "Announcement"));
                    insertAnnouncement.setString(2, normalize(announcement.getContent(), ""));
                    insertAnnouncement.setString(3, normalize(announcement.getPublishDate(), now));
                    insertAnnouncement.setString(4, now);
                    insertAnnouncement.addBatch();
                }
                insertAnnouncement.executeBatch();

                for (String notification : snapshot.getNotifications()) {
                    insertNotification.setString(1, "Portal update");
                    insertNotification.setString(2, normalize(notification, "New update available in SIS."));
                    insertNotification.setString(3, now);
                    insertNotification.addBatch();
                }
                insertNotification.executeBatch();

                if (!snapshot.getAttendanceSummary().isBlank()) {
                    upsertAcademic.setString(1, "attendance");
                    upsertAcademic.setString(2, snapshot.getAttendanceSummary());
                    upsertAcademic.setString(3, now);
                    upsertAcademic.addBatch();
                }
                if (!snapshot.getResultSummary().isBlank()) {
                    upsertAcademic.setString(1, "results");
                    upsertAcademic.setString(2, snapshot.getResultSummary());
                    upsertAcademic.setString(3, now);
                    upsertAcademic.addBatch();
                }
                if (!snapshot.getTimetableSummary().isBlank()) {
                    upsertAcademic.setString(1, "timetable");
                    upsertAcademic.setString(2, snapshot.getTimetableSummary());
                    upsertAcademic.setString(3, now);
                    upsertAcademic.addBatch();
                }
                upsertAcademic.executeBatch();

                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to save portal snapshot to cache.", ex);
        }
    }

    public void seedDefaultDataIfEmpty(String registrationNo) {
        if (!getAssignments().isEmpty() || !getAnnouncements().isEmpty()) {
            return;
        }
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement assignment = connection.prepareStatement("""
                 INSERT INTO assignments (title, description, due_date, status, updated_at)
                 VALUES (?, ?, ?, ?, ?)
                 """);
             PreparedStatement announcement = connection.prepareStatement("""
                 INSERT INTO announcements (title, content, publish_date, updated_at)
                 VALUES (?, ?, ?, ?)
                 """);
             PreparedStatement notification = connection.prepareStatement("""
                 INSERT INTO notifications (title, message, created_at)
                 VALUES (?, ?, ?)
                 """)) {

            String now = LocalDateTime.now().toString();

            assignment.setString(1, "Programming Fundamentals Lab");
            assignment.setString(2, "Prepare linked list implementation with complexity analysis.");
            assignment.setString(3, "This week");
            assignment.setString(4, "Pending");
            assignment.setString(5, now);
            assignment.addBatch();

            assignment.setString(1, "Discrete Structures Quiz");
            assignment.setString(2, "Review recurrence relations and proof by induction.");
            assignment.setString(3, "Next week");
            assignment.setString(4, "Upcoming");
            assignment.setString(5, now);
            assignment.addBatch();
            assignment.executeBatch();

            announcement.setString(1, "Welcome to Assignly Desktop");
            announcement.setString(2, "Hi " + registrationNo + ", your dashboard is ready. Open SIS and use Cache Snapshot to sync real data.");
            announcement.setString(3, now);
            announcement.setString(4, now);
            announcement.executeUpdate();

            notification.setString(1, "Setup completed");
            notification.setString(2, "Credentials secured and workspace initialized.");
            notification.setString(3, now);
            notification.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to seed initial dashboard data.", ex);
        }
    }

    public void clearAllCaches() {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement clearAssignments = connection.prepareStatement("DELETE FROM assignments");
             PreparedStatement clearAnnouncements = connection.prepareStatement("DELETE FROM announcements");
             PreparedStatement clearNotifications = connection.prepareStatement("DELETE FROM notifications");
             PreparedStatement clearAcademic = connection.prepareStatement("DELETE FROM academic_cache")) {
            clearAssignments.executeUpdate();
            clearAnnouncements.executeUpdate();
            clearNotifications.executeUpdate();
            clearAcademic.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to clear local cache.", ex);
        }

        try {
            java.io.File logo = new java.io.File(com.assignly.util.AppDirectoryHelper.getAppDataDir(), "cui_logo.png");
            if (logo.exists()) {
                logo.delete();
            }
        } catch (Exception ignored) {}
    }

    private String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
