package com.assignly.model;

public class UserPreferences {
    private String theme;
    private boolean autoLogin;
    private boolean notificationsEnabled;
    private boolean darkOverlay;
    private double zoomLevel;

    public UserPreferences(String theme, boolean autoLogin, boolean notificationsEnabled, boolean darkOverlay, double zoomLevel) {
        this.theme = theme;
        this.autoLogin = autoLogin;
        this.notificationsEnabled = notificationsEnabled;
        this.darkOverlay = darkOverlay;
        this.zoomLevel = zoomLevel;
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

    public boolean isDarkOverlay() {
        return darkOverlay;
    }

    public void setDarkOverlay(boolean darkOverlay) {
        this.darkOverlay = darkOverlay;
    }

    public double getZoomLevel() {
        return zoomLevel;
    }

    public void setZoomLevel(double zoomLevel) {
        this.zoomLevel = zoomLevel;
    }
}
