package com.assignly.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AnalyticsManager {
    private static final String PREFS_FILE = "analytics.properties";
    private static final String KEY_UUID = "installation_id";
    private static final String KEY_FIRST_INSTALL_TIME = "first_install_time";
    private static final String KEY_IS_REGISTERED = "is_registered";

    private static final String BASE_URL = "http://localhost:3000"; // Placeholder URL
    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static void initialize() {
        Properties props = loadProperties();

        String installationId = props.getProperty(KEY_UUID);
        if (installationId == null) {
            installationId = UUID.randomUUID().toString();
            props.setProperty(KEY_UUID, installationId);
        }

        String firstInstallTimeStr = props.getProperty(KEY_FIRST_INSTALL_TIME);
        if (firstInstallTimeStr == null) {
            firstInstallTimeStr = String.valueOf(System.currentTimeMillis());
            props.setProperty(KEY_FIRST_INSTALL_TIME, firstInstallTimeStr);
        }

        saveProperties(props);

        final String finalInstallId = installationId;
        final long finalFirstInstallTime = Long.parseLong(firstInstallTimeStr);
        final String version = "1.0.0"; // Get from pom.xml or manifest later

        scheduler.scheduleWithFixedDelay(() -> {
            try {
                Properties currentProps = loadProperties();
                boolean isRegistered = Boolean.parseBoolean(currentProps.getProperty(KEY_IS_REGISTERED, "false"));

                if (!isRegistered) {
                    // Register
                    String payload = String.format("{\"installationId\":\"%s\",\"platform\":\"desktop\",\"version\":\"%s\",\"firstInstallTime\":%d}",
                            finalInstallId, version, finalFirstInstallTime);

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(BASE_URL + "/api/install"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(payload))
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200 || response.statusCode() == 409) {
                        currentProps.setProperty(KEY_IS_REGISTERED, "true");
                        saveProperties(currentProps);
                    } else {
                        System.err.println("Analytics registration failed: " + response.statusCode());
                        return; // Retry next time
                    }
                }

                // Heartbeat
                String hbPayload = String.format("{\"installationId\":\"%s\",\"version\":\"%s\"}", finalInstallId, version);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/api/heartbeat"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(hbPayload))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    System.err.println("Analytics heartbeat failed: " + response.statusCode());
                }

            } catch (Exception e) {
                System.err.println("Analytics error: " + e.getMessage());
            }
        }, 0, 24, TimeUnit.HOURS);
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        File file = new File(AppDirectoryHelper.getAppDataDir(), PREFS_FILE);
        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file)) {
                props.load(in);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return props;
    }

    private static void saveProperties(Properties props) {
        File file = new File(AppDirectoryHelper.getAppDataDir(), PREFS_FILE);
        try (FileOutputStream out = new FileOutputStream(file)) {
            props.store(out, "Assignly Analytics Configuration");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
