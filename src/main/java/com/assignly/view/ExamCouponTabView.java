package com.assignly.view;

import com.assignly.util.AppContext;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfWriter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Exam Entry Coupon view – fetches EntryCouponSelect.aspx,
 * checks if the coupon is available, auto-follows the print link,
 * and renders the actual coupon using a WebView exactly as the portal does.
 */
public class ExamCouponTabView {

    private final VBox root = new VBox();
    private final AppContext context;
    private final WebView webView = new WebView();
    private StackPane contentPane;

    public ExamCouponTabView(AppContext context) {
        this.context = context;
        buildShell();
        loadCoupon(false);
    }

    private void buildShell() {
        root.setFillWidth(true);
        contentPane = new StackPane();
        VBox.setVgrow(contentPane, Priority.ALWAYS);
        root.getChildren().add(contentPane);
    }

    public VBox getRoot() { return root; }

    // ==================== Loading ====================

    private void buildLoading() {
        contentPane.getChildren().clear();
        StackPane loading = new StackPane();
        loading.setStyle("-fx-background-color: #F0EDEC;");
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(28, 28);
        Label msg = new Label("Loading Exam Entry Coupon...");
        msg.setStyle("-fx-text-fill: #888888; -fx-font-size: 13px;");
        box.getChildren().addAll(spinner, msg);
        loading.getChildren().add(box);
        contentPane.getChildren().add(loading);
    }

    // ==================== Fetch ====================

    private void loadCoupon(boolean forceRefresh) {
        buildLoading();
        new Thread(() -> {
            try {
                boolean isOffline = false;

                // Step 1: Fetch the EntryCouponSelect.aspx page
                String selectHtml = null;
                if (!forceRefresh) selectHtml = context.dataCacheService().getCachedHtml("EntryCouponSelect.aspx").orElse(null);
                if (selectHtml == null) {
                    selectHtml = context.fetchAndCacheHtml("EntryCouponSelect.aspx");
                    if (selectHtml == null) {
                        selectHtml = context.dataCacheService().getCachedHtml("EntryCouponSelect.aspx").orElse(null);
                        isOffline = true;
                    }
                }

                if (selectHtml == null) {
                    showError("Unable to load Exam Entry Coupon",
                            "Failed to connect to the portal and no offline data available.");
                    return;
                }

                Document selectDoc = Jsoup.parse(selectHtml);

                // Step 2: Check if the "Click Here To Print Exam Entry Coupon" text is present
                boolean isCouponAvailable = false;
                for (Element link : selectDoc.select("a, span, div, b, strong, td")) {
                    String linkText = link.text().trim().toLowerCase();
                    if (linkText.contains("print") && linkText.contains("exam") && linkText.contains("coupon")) {
                        isCouponAvailable = true;
                        break;
                    }
                }

                if (!isCouponAvailable) {
                    boolean finalOffline1 = isOffline;
                    Platform.runLater(() -> {
                        contentPane.getChildren().clear();
                        VBox container = new VBox();
                        if (finalOffline1) container.getChildren().add(buildOfflineBanner());
                        container.getChildren().add(buildNoCouponView());
                        contentPane.getChildren().add(container);
                    });
                    return;
                }

                // Step 3: Coupon IS available — fetch EntryCouponWithQR.aspx
                String couponHtml = null;
                if (!forceRefresh) couponHtml = context.dataCacheService().getCachedHtml("EntryCouponWithQR.aspx").orElse(null);
                if (couponHtml == null) {
                    couponHtml = context.fetchAndCacheHtml("EntryCouponWithQR.aspx");
                    if (couponHtml == null) {
                        couponHtml = context.dataCacheService().getCachedHtml("EntryCouponWithQR.aspx").orElse(null);
                        isOffline = true;
                    }
                }
                
                if (couponHtml == null) {
                    showError("Unable to load Exam Coupon", "The coupon page could not be loaded and no offline data available.");
                    return;
                }

                // Inject a base tag so relative CSS/images load correctly in the WebView
                Document doc = Jsoup.parse(couponHtml, "https://sis.cuiatd.edu.pk/");
                doc.head().append("<base href=\"https://sis.cuiatd.edu.pk/\">");
                
                // Fetch images securely and embed as Base64 so WebView doesn't need cookies
                String refererUrl = "https://sis.cuiatd.edu.pk/EntryCouponWithQR.aspx";
                for (Element img : doc.select("img")) {
                    String absUrl = img.attr("abs:src");
                    if (!absUrl.isEmpty() && !absUrl.startsWith("data:")) {
                        // Pass the EntryCouponWithQR.aspx referer so QR codes generate correctly
                        byte[] imgBytes = context.portalRepository().fetchPhotoBytes(absUrl, refererUrl);
                        if (imgBytes != null && imgBytes.length > 0) {
                            String base64 = java.util.Base64.getEncoder().encodeToString(imgBytes);
                            // Detect mime type roughly
                            String mime = absUrl.toLowerCase().endsWith(".jpg") || absUrl.toLowerCase().endsWith(".jpeg") ? "image/jpeg" : "image/png";
                            img.attr("src", "data:" + mime + ";base64," + base64);
                        }
                    }
                }
                
                final String htmlWithBase = doc.outerHtml();
                final boolean finalOffline = isOffline;

                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    VBox container = new VBox();
                    if (finalOffline) container.getChildren().add(buildOfflineBanner());
                    container.getChildren().add(buildWebViewLayout(htmlWithBase));
                    contentPane.getChildren().add(container);
                });
            } catch (Exception e) {
                e.printStackTrace();
                showError("Error", "An unexpected error occurred: " + e.getMessage());
            }
        }).start();
    }

    // ==================== Web View Layout ====================

    private VBox buildWebViewLayout(String htmlContent) {
        VBox layout = new VBox(0);
        VBox.setVgrow(layout, Priority.ALWAYS);

        // Top bar
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(12, 24, 12, 24));
        topBar.setStyle("-fx-background-color: -color-bg-card;-fx-border-color: -color-border;-fx-border-width:0 0 1 0;");

        Label title = new Label("Exam Entry Coupon");
        title.setStyle("-fx-font-size:16px;-fx-font-weight:800;-fx-text-fill: -color-text-main;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("🔄");
        refreshBtn.setStyle("-fx-background-color:transparent;-fx-font-size:18px;-fx-cursor:hand;");
        refreshBtn.setOnAction(e -> {
            contentPane.getChildren().clear();
            loadCoupon(true);
        });

        Button printBtn = new Button("⬇ Save as PDF");
        printBtn.setCursor(javafx.scene.Cursor.HAND);
        printBtn.setStyle("-fx-background-color:#004643;-fx-text-fill:white;"
                + "-fx-font-size:12px;-fx-font-weight:bold;-fx-background-radius:6;-fx-padding:8 16;");
        printBtn.setOnAction(e -> {
            printBtn.setDisable(true);
            printBtn.setText("Generating...");
            saveAsPdf(() -> {
                printBtn.setDisable(false);
                printBtn.setText("⬇ Save as PDF");
            });
        });

        topBar.getChildren().addAll(title, spacer, refreshBtn, printBtn);

        // Web engine setup
        WebEngine engine = webView.getEngine();
        engine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        // Hide unwanted elements from the coupon page via JS (like sidebars if any)
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                // Remove navigation/header if they appear on the print page
                engine.executeScript(
                        "var nav = document.getElementById('navigation'); if(nav) nav.style.display = 'none';" +
                        "var header = document.getElementById('header'); if(header) header.style.display = 'none';"
                );
            }
        });

        // Load the raw HTML string directly, bypassing WebView network/cookie issues
        engine.loadContent(htmlContent);

        VBox.setVgrow(webView, Priority.ALWAYS);
        layout.getChildren().addAll(topBar, webView);

        return layout;
    }

    private void saveAsPdf(Runnable onComplete) {
        try {
            Printer pdfPrinter = null;
            for (Printer p : Printer.getAllPrinters()) {
                if (p.getName().toLowerCase().contains("pdf")) {
                    pdfPrinter = p;
                    break;
                }
            }

            if (pdfPrinter != null) {
                PrinterJob job = PrinterJob.createPrinterJob();
                if (job != null) {
                    job.setPrinter(pdfPrinter);
                    
                    // Force minimum margins so it fits on one page
                    PageLayout pageLayout = pdfPrinter.createPageLayout(Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.HARDWARE_MINIMUM);
                    job.getJobSettings().setPageLayout(pageLayout);
                    job.getJobSettings().setJobName("Exam_Coupon_" + context.getSessionRegistration().replaceAll("[^a-zA-Z0-9]", ""));
                    
                    // Inject CSS to scale down slightly for printing
                    webView.getEngine().executeScript(
                        "var style = document.createElement('style');" +
                        "style.type = 'text/css';" +
                        "style.innerHTML = '@media print { body { zoom: 0.85; margin: 0 !important; } }';" +
                        "document.head.appendChild(style);"
                    );
                    
                    // webView.getEngine().print(job) perfectly formats the HTML into pages and 
                    // pops up the native OS "Save Print Output As" dialog!
                    webView.getEngine().print(job);
                    job.endJob();
                    onComplete.run();
                    return; // Successfully saved (or cancelled by user natively)
                }
            }

            // Fallback if no PDF printer is installed
            onComplete.run();
            showErrorAlert("No PDF Printer", "Could not find 'Microsoft Print to PDF' or any native PDF printer on your system. Please ensure a PDF printer is installed.");

        } catch (Exception ex) {
            ex.printStackTrace();
            onComplete.run();
            showErrorAlert("Save Failed", "Could not initialize PDF save: " + ex.getMessage());
        }
    }

    // ==================== No Coupon View ====================

    private VBox buildNoCouponView() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(24, 28, 24, 28));
        content.setFillWidth(true);
        VBox.setVgrow(content, Priority.ALWAYS);

        Label title = new Label("Exam Entry Coupon");
        title.setStyle("-fx-font-size:18px;-fx-font-weight:800;-fx-text-fill: -color-text-main;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("🔄");
        refreshBtn.setStyle("-fx-background-color:transparent;-fx-font-size:18px;-fx-cursor:hand;");
        refreshBtn.setOnAction(e -> {
            contentPane.getChildren().clear();
            loadCoupon(true);
        });
        
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().addAll(title, spacer, refreshBtn);
        content.getChildren().add(header);

        // Not available message
        VBox noCouponCard = new VBox(12);
        noCouponCard.setAlignment(Pos.CENTER);
        noCouponCard.setPadding(new Insets(32));
        noCouponCard.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:8;"
                + "-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:8;"
                + "-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.02),10,0,0,2);");

        Label icon = new Label("📋");
        icon.setStyle("-fx-font-size:32px;");

        Label noCouponLabel = new Label("Exam Entry Coupon Not Available");
        noCouponLabel.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill: -color-text-muted;");

        Label noCouponDesc = new Label("The exam entry coupon is not currently available. "
                + "This may be due to pending fee payments, missing documents, "
                + "or the coupon not being released yet by the exam section.");
        noCouponDesc.setStyle("-fx-font-size:12px;-fx-text-fill: -color-text-muted;-fx-text-alignment:center;");
        noCouponDesc.setWrapText(true);

        noCouponCard.getChildren().addAll(icon, noCouponLabel, noCouponDesc);
        content.getChildren().add(noCouponCard);

        return content;
    }

    // ==================== Helpers ====================

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            contentPane.getChildren().clear();
            contentPane.getChildren().add(buildErrorView(title, message));
        });
    }

    private VBox buildErrorView(String title, String message) {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));
        VBox.setVgrow(box, Priority.ALWAYS);

        Label icon = new Label("⚠");
        icon.setStyle("-fx-font-size:28px;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill: -color-text-main;");

        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-font-size:12px;-fx-text-fill: -color-text-muted;-fx-text-alignment:center;");
        msgLabel.setWrapText(true);

        box.getChildren().addAll(icon, titleLabel, msgLabel);
        return box;
    }

    private void showInfoAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showErrorAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private HBox buildOfflineBanner() {
        HBox banner = new HBox(8);
        banner.setAlignment(Pos.CENTER);
        banner.setPadding(new Insets(8, 16, 8, 16));
        banner.setStyle("-fx-background-color:#FEF2F2;-fx-border-color:#FCA5A5;-fx-border-width:0 0 1 0;");
        
        Label icon = new Label("⚠");
        icon.setStyle("-fx-text-fill:#DC2626;-fx-font-size:14px;");
        Label text = new Label("Offline Mode: Displaying previously loaded data.");
        text.setStyle("-fx-text-fill:#991B1B;-fx-font-size:12px;-fx-font-weight:bold;");
        
        banner.getChildren().addAll(icon, text);
        return banner;
    }
}
