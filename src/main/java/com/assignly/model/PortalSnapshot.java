package com.assignly.model;

import java.util.ArrayList;
import java.util.List;

public class PortalSnapshot {
    private List<PortalAssignment> assignments = new ArrayList<>();
    private List<PortalAnnouncement> announcements = new ArrayList<>();
    private List<String> notifications = new ArrayList<>();
    private String attendanceSummary = "";
    private String resultSummary = "";
    private String timetableSummary = "";

    public List<PortalAssignment> getAssignments() {
        return assignments;
    }

    public List<PortalAnnouncement> getAnnouncements() {
        return announcements;
    }

    public List<String> getNotifications() {
        return notifications;
    }

    public String getAttendanceSummary() {
        return attendanceSummary;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public String getTimetableSummary() {
        return timetableSummary;
    }

    public List<Assignment> toAssignments() {
        List<Assignment> list = new ArrayList<>();
        for (PortalAssignment assignment : assignments) {
            list.add(new Assignment(0, assignment.title, assignment.description, assignment.dueDate, assignment.status));
        }
        return list;
    }

    public List<Announcement> toAnnouncements() {
        List<Announcement> list = new ArrayList<>();
        for (PortalAnnouncement announcement : announcements) {
            list.add(new Announcement(0, announcement.title, announcement.content, announcement.publishDate));
        }
        return list;
    }

    public static class PortalAssignment {
        private String title = "";
        private String description = "";
        private String dueDate = "";
        private String status = "Pending";
    }

    public static class PortalAnnouncement {
        private String title = "";
        private String content = "";
        private String publishDate = "";
    }
}
