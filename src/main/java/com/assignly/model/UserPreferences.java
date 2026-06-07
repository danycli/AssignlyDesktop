package com.assignly.model;

public class UserPreferences {
    private String theme;
    private boolean autoLogin;
    private boolean notificationsEnabled;
    private boolean darkOverlay;
    private double zoomLevel;
    private boolean autoCheckUpdates;
    private boolean notifyPreReleases;
    private boolean openWebsiteFirst;
    private String dismissedVersion;

    public UserPreferences(String theme, boolean autoLogin, boolean notificationsEnabled, boolean darkOverlay, double zoomLevel, 
                           boolean autoCheckUpdates, boolean notifyPreReleases, boolean openWebsiteFirst, String dismissedVersion) {
        this.theme = theme;
        this.autoLogin = autoLogin;
        this.notificationsEnabled = notificationsEnabled;
        this.darkOverlay = darkOverlay;
        this.zoomLevel = zoomLevel;
        this.autoCheckUpdates = autoCheckUpdates;
        this.notifyPreReleases = notifyPreReleases;
        this.openWebsiteFirst = openWebsiteFirst;
        this.dismissedVersion = dismissedVersion;
    }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public boolean isAutoLogin() { return autoLogin; }
    public void setAutoLogin(boolean autoLogin) { this.autoLogin = autoLogin; }

    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }

    public boolean isDarkOverlay() { return darkOverlay; }
    public void setDarkOverlay(boolean darkOverlay) { this.darkOverlay = darkOverlay; }

    public double getZoomLevel() { return zoomLevel; }
    public void setZoomLevel(double zoomLevel) { this.zoomLevel = zoomLevel; }

    public boolean isAutoCheckUpdates() { return autoCheckUpdates; }
    public void setAutoCheckUpdates(boolean autoCheckUpdates) { this.autoCheckUpdates = autoCheckUpdates; }

    public boolean isNotifyPreReleases() { return notifyPreReleases; }
    public void setNotifyPreReleases(boolean notifyPreReleases) { this.notifyPreReleases = notifyPreReleases; }

    public boolean isOpenWebsiteFirst() { return openWebsiteFirst; }
    public void setOpenWebsiteFirst(boolean openWebsiteFirst) { this.openWebsiteFirst = openWebsiteFirst; }

    public String getDismissedVersion() { return dismissedVersion; }
    public void setDismissedVersion(String dismissedVersion) { this.dismissedVersion = dismissedVersion; }
}
