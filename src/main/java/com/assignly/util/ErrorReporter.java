package com.assignly.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ErrorReporter {
    private static final Path LOG_PATH = AppDirectoryHelper.getLogPath();
    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ErrorReporter() {
    }

    public static void logError(String context, Throwable error) {
        if (error == null) {
            return;
        }
        String safeContext = (context == null || context.isBlank()) ? "Unknown" : context;
        String entry = formatEntry(safeContext, error);
        synchronized (ErrorReporter.class) {
            try {
                Files.writeString(LOG_PATH, entry, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException ioEx) {
                System.err.println("[ErrorReporter] Failed to write log: " + ioEx.getMessage());
            }
        }
        System.err.print(entry);
    }

    public static void notify(AppContext appContext, String userMessage, String context, Throwable error) {
        logError(context, error);
        if (appContext != null && userMessage != null && !userMessage.isBlank()) {
            appContext.notificationService().showError(userMessage);
        }
    }

    private static String formatEntry(String context, Throwable error) {
        String timestamp = LocalDateTime.now().format(TS_FORMAT);
        String message = error.getMessage();
        StringBuilder builder = new StringBuilder();
        builder.append("[").append(timestamp).append("] ").append(context);
        builder.append(" - ").append(error.getClass().getSimpleName());
        if (message != null && !message.isBlank()) {
            builder.append(": ").append(message);
        }
        builder.append(System.lineSeparator());
        StringWriter sw = new StringWriter();
        error.printStackTrace(new PrintWriter(sw));
        builder.append(sw.toString()).append(System.lineSeparator());
        return builder.toString();
    }
}
