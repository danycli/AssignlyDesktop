package com.assignly.service;

import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.CefInitializationException;
import me.friwi.jcefmaven.UnsupportedPlatformException;
import me.friwi.jcefmaven.impl.progress.ConsoleProgressHandler;
import org.cef.CefApp;

import java.io.File;
import java.io.IOException;

public class JcefService {

    private static CefApp cefAppInstance = null;
    private static boolean isInitializing = false;

    /**
     * Initializes JCEF. If binaries are missing, jcefmaven will download them automatically.
     * This blocks the calling thread, so it should be called on a background thread if UI needs to stay responsive.
     */
    public static synchronized void initialize(String installDir) throws UnsupportedPlatformException, CefInitializationException, IOException, InterruptedException {
        if (cefAppInstance != null) {
            return;
        }
        if (isInitializing) {
            return;
        }
        isInitializing = true;

        try {
            CefAppBuilder builder = new CefAppBuilder();
            
            // Set the directory where native files will be extracted
            File dir = new File(installDir, "jcef-bundle");
            builder.setInstallDir(dir);
            
            // Add progress reporting for the download phase
            builder.setProgressHandler(new ConsoleProgressHandler());
            
            // Configure settings
            builder.getCefSettings().windowless_rendering_enabled = false;
            
            // Build the CefApp instance (thread-safe, handles download and extraction)
            cefAppInstance = builder.build();
        } finally {
            isInitializing = false;
        }
    }

    public static CefApp getApp() {
        return cefAppInstance;
    }

    /**
     * MUST be called during application shutdown to avoid jcef_helper zombie processes.
     */
    public static void dispose() {
        if (cefAppInstance != null) {
            cefAppInstance.dispose();
            cefAppInstance = null;
        }
    }
}
