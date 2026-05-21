package com.assignly.model;

public class UserPreferences {
    private String theme;
    private boolean autoLogin;
    private boolean notificationsEnabled;
    private double zoomLevel;
    private boolean darkOverlay;

    public UserPreferences(String theme, boolean autoLogin, boolean notificationsEnabled, double zoomLevel, boolean darkOverlay) {
        this.theme = theme;
        this.autoLogin = autoLogin;
        this.notificationsEnabled = notificationsEnabled;
        this.zoomLevel = zoomLevel;
        this.darkOverlay = darkOverlay;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public boolean isAutoLogin() {
        return autoLogin;
    }

    public void setAutoLogin(boolean autoLogin) {
        this.autoLogin = autoLogin;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public double getZoomLevel() {
        return zoomLevel;
    }

    public void setZoomLevel(double zoomLevel) {
        this.zoomLevel = zoomLevel;
    }

    public boolean isDarkOverlay() {
        return darkOverlay;
    }

    public void setDarkOverlay(boolean darkOverlay) {
        this.darkOverlay = darkOverlay;
    }
}
