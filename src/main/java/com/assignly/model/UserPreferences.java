package com.assignly.model;

public class UserPreferences {
    private String theme;
    private boolean autoLogin;
    private boolean notificationsEnabled;
    public UserPreferences(String theme, boolean autoLogin, boolean notificationsEnabled) {
        this.theme = theme;
        this.autoLogin = autoLogin;
        this.notificationsEnabled = notificationsEnabled;
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


}
