package com.assignly.util;

import java.io.File;
import java.nio.file.Path;

public class AppDirectoryHelper {
    public static String getAppDataDir() {
        String os = System.getProperty("os.name").toLowerCase();
        String path;
        if (os.contains("win")) {
            String appData = System.getenv("LOCALAPPDATA");
            if (appData == null) {
                appData = System.getenv("APPDATA");
            }
            if (appData != null) {
                path = appData + "/Assignly";
            } else {
                path = System.getProperty("user.home") + "/AppData/Local/Assignly";
            }
        } else if (os.contains("mac")) {
            path = System.getProperty("user.home") + "/Library/Application Support/Assignly";
        } else {
            path = System.getProperty("user.home") + "/.assignly";
        }
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return path;
    }

    public static String getDatabaseUrl() {
        return "jdbc:sqlite:" + getAppDataDir() + "/assignly.db";
    }

    public static Path getLogPath() {
        return Path.of(getAppDataDir(), "assignly.log");
    }
}
