package com.assignly.view;

import com.assignly.util.AppContext;
import com.assignly.util.ErrorReporter;
import com.assignly.service.PortalRepository;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Clean multi-tab view for all the Course Portal sections, mirroring the CoursesTabView design.
 * Features beautifully civilized, native parsed grids for MCQ and Subjective tests, and scorecard details.
 */
public class CoursePortalTabView {
    private final VBox root = new VBox();
    private final AppContext context;
    private StackPane contentPane;
    private HBox tabBar;
    private String activeTab = "";
    private final List<Button> uploadButtons = new ArrayList<>();
    private final java.util.function.Consumer<Boolean> connectivityListener = this::onConnectivityChanged;

    public CoursePortalTabView(AppContext context) {
        this.context = context;
        buildShell();
        loadTab("portal_mcq");
        context.addConnectivityListener(connectivityListener);
    }

    private void buildShell() {
        root.setFillWidth(true);
        Label heading = new Label("Course Portal");
        heading.getStyleClass().add("heading-label");
        heading.setPadding(new Insets(24, 28, 0, 28));

        tabBar = new HBox(4);
        tabBar.setPadding(new Insets(12, 28, 0, 28));
        tabBar.getChildren().addAll(
            tabBtn("MCQ Test", "portal_mcq"),
            tabBtn("Subjective Test", "portal_subjective"),
            tabBtn("Course Contents", "portal_contents"),
            tabBtn("Assignments Summary", "portal_assign_summ"),
            tabBtn("Pending Assignments", "portal_pending")
        );

        contentPane = new StackPane();
        VBox.setVgrow(contentPane, Priority.ALWAYS);
        root.getChildren().addAll(heading, tabBar, contentPane);
    }

    private Button tabBtn(String label, String id) {
        Button b = new Button(label);
        b.setUserData(id);
        b.getStyleClass().add("custom-tab");
        b.setOnAction(e -> loadTab(id));
        return b;
    }

    private void loadTab(String tabKey) {
        if (activeTab.equals(tabKey)) return;
        activeTab = tabKey;

        for (var n : tabBar.getChildren()) {
            if (n instanceof Button b) {
                boolean isActive = tabKey.equals(b.getUserData());
                b.getStyleClass().remove("custom-tab-active");
                if (isActive) {
                    b.getStyleClass().add("custom-tab-active");
                }
            }
        }

        contentPane.getChildren().clear();
        uploadButtons.clear();
        switch (tabKey) {
            case "portal_mcq" -> loadMcqData();
            case "portal_subjective" -> loadSubjectiveData();
            case "portal_contents" -> loadCourseContentsData();
            case "portal_assign_summ" -> loadAssignmentsData();
            case "portal_pending" -> loadPendingAssignmentsData();
        }
    }

    private void showLoading(String msg) {
        Platform.runLater(() -> {
            contentPane.getChildren().clear();
            VBox box = new VBox(10); box.setAlignment(Pos.CENTER);
            ProgressIndicator sp = new ProgressIndicator(); sp.setMaxSize(28,28);
            Label l = new Label(msg); l.setStyle("-fx-text-fill: -color-text-muted;-fx-font-size:12px;");
            box.getChildren().addAll(sp, l);
            contentPane.getChildren().add(new StackPane(box));
        });
    }

    private VBox buildErrorState(String title, String message) {
        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40, 28, 40, 28));
        box.getStyleClass().add("panel-info-danger");
        box.setStyle("-fx-background-radius:12;-fx-border-radius:12;-fx-max-width:550;");

        Label icon = new Label("⚠️");
        icon.setStyle("-fx-font-size:32px;");

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("status-error");
        titleLbl.setStyle("-fx-font-size:16px;");

        Label msgLbl = new Label(message);
        msgLbl.getStyleClass().add("status-error");
        msgLbl.setStyle("-fx-font-size:12px;-fx-text-alignment:center;");
        msgLbl.setWrapText(true);

        box.getChildren().addAll(icon, titleLbl, msgLbl);
        
        VBox outer = new VBox(box);
        outer.setAlignment(Pos.CENTER);
        outer.setPadding(new Insets(40));
        return outer;
    }

    private void loadMcqData() {
        showLoading("Loading MCQ Tests...");
        new Thread(() -> {
            try {
                String html = context.portalRepository().fetchPageHtml("CTS/CTSdashboard.aspx");
                if (html == null) {
                    Platform.runLater(() -> {
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(buildErrorState(
                            "Unable to load MCQ Tests",
                            "Failed to connect to the portal database. Please check your internet connection and try again."
                        ));
                    });
                    return;
                }

                try {
                    java.nio.file.Files.writeString(java.nio.file.Paths.get("cts_dashboard_raw.html"), html);
                } catch (IOException ex) {
                    ErrorReporter.logError("CoursePortalTabView#loadMcqData write cts_dashboard_raw.html", ex);
                }

                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    try {
                        contentPane.getChildren().add(buildMcqView(html));
                    } catch (Exception ex) {
                        ErrorReporter.notify(context, "Failed to parse MCQ tests. Portal layout may have changed.", "CoursePortalTabView#loadMcqData", ex);
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(buildErrorState(
                            "Parsing Error",
                            "Failed to read the MCQ table layout cleanly. The portal database may be undergoing updates."
                        ));
                    }
                });
            } catch (Exception e) {
                ErrorReporter.notify(context, "Failed to load MCQ tests. Please try again.", "CoursePortalTabView#loadMcqData", e);
                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    contentPane.getChildren().add(buildErrorState(
                        "Loading Error",
                        "An unexpected error occurred while loading MCQ tests: " + e.getMessage()
                    ));
                });
            }
        }).start();
    }

    private ScrollPane buildMcqView(String html) {
        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");

        VBox content = new VBox(16);
        content.setPadding(new Insets(16, 28, 24, 28));
        content.setFillWidth(true);

        Document doc = Jsoup.parse(html);
        
        String headingText = "COMSATS University Islamabad Online Testing Service Dashboard";
        Element h3 = doc.select("h3").first();
        if (h3 != null && !h3.text().trim().isEmpty()) {
            headingText = h3.text().trim();
        }

        Label subHeading = new Label(headingText);
        subHeading.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill: -color-text-main;-fx-padding:0 0 4 0;");
        content.getChildren().add(subHeading);

        Element table = findMainGridTable(doc, "DataContent_gvCTSdashboard");

        if (table != null) {
            Elements rows = table.select("tr");
            if (rows.size() > 1) {
                VBox tableCard = buildNativeMcqTable(rows);
                content.getChildren().add(tableCard);
                content.setMinWidth(tableCard.getMinWidth() + 56);
            } else {
                renderEmptyState(content);
            }
        } else {
            renderEmptyState(content);
        }

        sp.setContent(content);
        return sp;
    }

    private void renderEmptyState(VBox content) {
        Label empty = new Label("No active or completed MCQ tests found.");
        empty.setStyle("-fx-text-fill: -color-text-muted;-fx-font-size:12px;-fx-padding:10 0;");
        content.getChildren().add(empty);
    }
    private VBox buildNativeMcqTable(Elements rows) {
        VBox card = new VBox(0);
        card.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:8;-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:8;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.02),10,0,0,2);");
        
        Element headerRow = rows.first();
        if (headerRow == null) return card;

        Elements headerCells = headerRow.select("th, td");
        List<String> headers = new ArrayList<>();
        for (Element hc : headerCells) {
            headers.add(hc.text().trim());
        }

        double totalWidth = 0;
        for (int i = 0; i < headers.size(); i++) {
            totalWidth += mcqColW(headers.get(i), i, headers.size());
        }
        card.setMinWidth(totalWidth);

        HBox headerBox = new HBox(0);
        headerBox.setStyle("-fx-background-color: -color-bg-main;-fx-padding:12 0;-fx-border-color: -color-border;-fx-border-width:0 0 1 0;");
        for (int i = 0; i < headers.size(); i++) {
            Label hl = new Label(headers.get(i));
            hl.setStyle("-fx-font-size:11px;-fx-font-weight:700;-fx-text-fill: -color-text-muted;-fx-padding:0 12;");
            double w = mcqColW(headers.get(i), i, headers.size());
            hl.setMinWidth(w);
            hl.setMaxWidth(w);
            headerBox.getChildren().add(hl);
        }
        card.getChildren().add(headerBox);

        for (int r = 1; r < rows.size(); r++) {
            Element row = rows.get(r);
            Elements cells = row.select("td");
            if (cells.isEmpty()) continue;

            HBox dataRow = new HBox(0);
            String bg = (r % 2 == 0) ? "-color-bg-card" : "-color-bg-main";
            dataRow.setStyle("-fx-background-color:" + bg + ";-fx-padding:12 0;-fx-border-color: -color-border;-fx-border-width:0 0 1 0;");

            for (int c = 0; c < cells.size(); c++) {
                if (c >= headers.size()) break;

                Element cell = cells.get(c);
                String txt = cell.text().trim().replaceAll("\\s+", " ");
                double w = mcqColW(headers.get(c), c, headers.size());
                Node cellNode;

                Element aTag = cell.select("a").first();
                if (aTag != null && !aTag.attr("href").isEmpty()) {
                    String linkText = aTag.text().trim();
                    String href = aTag.attr("href");

                    Button actBtn = new Button(linkText);
                    actBtn.setCursor(javafx.scene.Cursor.HAND);
                    actBtn.setStyle("-fx-background-color: -color-accent;-fx-text-fill: -color-bg-main;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:4;-fx-padding:4 10;");
                    
                    actBtn.setOnAction(e -> loadMcqPaperView(href, linkText));

                    HBox btnWrapper = new HBox(actBtn);
                    btnWrapper.setAlignment(Pos.CENTER_LEFT);
                    btnWrapper.setPadding(new Insets(0, 12, 0, 12));
                    btnWrapper.setMinWidth(w);
                    btnWrapper.setMaxWidth(w);
                    cellNode = btnWrapper;
                } else {
                    Label cl = new Label(txt);
                    cl.setWrapText(true);
                    cl.setStyle("-fx-font-size:12px;-fx-text-fill: -color-text-main;-fx-padding:0 12;");
                    cl.setMinWidth(w);
                    cl.setMaxWidth(w);
                    cellNode = cl;
                }

                dataRow.getChildren().add(cellNode);
            }
            card.getChildren().add(dataRow);
        }
        return card;
    }

    private double mcqColW(String h, int i, int total) {
        String l = h.toLowerCase();
        if (l.contains("#")) return 50;
        if (l.contains("test") || l.contains("title")) return 200;
        if (l.contains("course")) return 280;
        if (l.contains("date") || l.contains("time")) return 180;
        if (l.contains("action")) return 140;
        return 120;
    }

    private void loadMcqPaperView(String relativeUrl, String title) {
        String urlTemp = relativeUrl;
        if (!urlTemp.startsWith("CTS/") && !urlTemp.startsWith("/CTS/")) {
            urlTemp = "CTS/" + urlTemp;
        }
        final String pageUrl = urlTemp;

        showLoading("Loading MCQ Test details...");

        new Thread(() -> {
            try {
                String html = context.portalRepository().fetchPageHtml(pageUrl);
                if (html == null) {
                    Platform.runLater(() -> {
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(buildErrorState(
                            "Connection Error",
                            "Failed to load MCQ Test details. Please check your internet connection."
                        ));
                    });
                    return;
                }

                try {
                    java.nio.file.Files.writeString(java.nio.file.Paths.get("cts_paper_raw.html"), html);
                } catch (IOException ex) {
                    ErrorReporter.logError("CoursePortalTabView#loadMcqDetails write cts_paper_raw.html", ex);
                }

                Document doc = Jsoup.parse(html);
                boolean isCompleted = doc.select("#DataContent_dvStudentOnlineTestResult").first() != null
                        || html.contains("online test is completed")
                        || doc.getElementById("DataContent_lblObtainMarks") != null;

                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    if (isCompleted) {
                        try {
                            contentPane.getChildren().add(buildNativeResultView(doc, pageUrl));
                        } catch (Exception ex) {
                            ErrorReporter.logError("CoursePortalTabView#loadMcqDetails render native MCQ view", ex);
                            renderWebViewPaper(pageUrl, title);
                        }
                    } else {
                        renderWebViewPaper(pageUrl, title);
                    }
                });
            } catch (Exception e) {
                ErrorReporter.notify(context, "Failed to load MCQ details. Please try again.", "CoursePortalTabView#loadMcqDetails", e);
                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    contentPane.getChildren().add(buildErrorState(
                        "Loading Error",
                        "An unexpected error occurred while loading MCQ details: " + e.getMessage()
                    ));
                });
            }
        }).start();
    }

    private VBox buildNativeResultView(Document doc, String pageUrl) {
        VBox wrapper = new VBox(20);
        wrapper.setPadding(new Insets(16, 28, 24, 28));
        wrapper.setFillWidth(true);

        Button backBtn = new Button("← Back to MCQ Tests List");
        backBtn.setCursor(javafx.scene.Cursor.HAND);
        backBtn.setStyle("-fx-background-color: -color-bg-card;-fx-text-fill: -color-accent;-fx-border-color: -color-accent;-fx-border-width:1;-fx-border-radius:4;-fx-background-radius:4;-fx-font-size:12px;-fx-font-weight:bold;-fx-padding:6 12;");
        backBtn.setOnAction(e -> {
            activeTab = ""; // Force reload
            loadTab("portal_mcq");
        });

        HBox topBar = new HBox(backBtn);
        topBar.setStyle("-fx-padding:0 0 10 0;");
        wrapper.getChildren().add(topBar);

        String testTitle = "Multiple Choice Questions";
        Element titleEl = doc.getElementById("DataContent_lblTestTitleResult");
        if (titleEl != null) testTitle = titleEl.text().trim();

        String totalQ = "N/A";
        Element totalQEl = doc.getElementById("DataContent_lblTotalQuestion");
        if (totalQEl != null) totalQ = totalQEl.text().trim();

        String attempted = "N/A";
        Element attemptedEl = doc.getElementById("DataContent_lblTotalAttempted");
        if (attemptedEl != null) attempted = attemptedEl.text().trim();

        String totalMarks = "N/A";
        Element totalMarksEl = doc.getElementById("DataContent_lblTotalMarks");
        if (totalMarksEl != null) totalMarks = totalMarksEl.text().trim();

        String obtainMarks = "N/A";
        Element obtainMarksEl = doc.getElementById("DataContent_lblObtainMarks");
        if (obtainMarksEl == null) obtainMarksEl = doc.select("[id$=lblObtainMarks]").first();
        if (obtainMarksEl != null) obtainMarks = obtainMarksEl.text().trim();

        VBox scorecard = new VBox(20);
        scorecard.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:12;-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:12;-fx-padding:24;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.03),15,0,0,3);");
        
        HBox scoreHeader = new HBox(12);
        scoreHeader.setAlignment(Pos.CENTER_LEFT);
        
        Label statusBadge = new Label("COMPLETED");
        statusBadge.getStyleClass().add("badge-success");
        
        Label titleLabel = new Label("MCQ Test Scorecard");
        titleLabel.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill: -color-text-main;");
        
        scoreHeader.getChildren().addAll(titleLabel, statusBadge);
        scorecard.getChildren().add(scoreHeader);

        VBox detailBox = new VBox(0);
        detailBox.setStyle("-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:8;-fx-background-radius:8;-fx-background-color: -color-bg-main;");
        
        detailBox.getChildren().addAll(
            resultRow("Test Title", testTitle, true),
            resultRow("Total Questions", totalQ, false),
            resultRow("Total Attempted", attempted, true),
            resultRow("Total Marks", totalMarks, false),
            resultRow("Obtained Marks", obtainMarks, true)
        );

        HBox visualScoreContainer = new HBox(32);
        visualScoreContainer.setAlignment(Pos.CENTER);
        visualScoreContainer.setPadding(new Insets(16, 0, 8, 0));

        VBox bigScoreCircle = new VBox(4);
        bigScoreCircle.setAlignment(Pos.CENTER);
        bigScoreCircle.setStyle("-fx-background-color: -color-accent;-fx-background-radius:100;-fx-min-width:110;-fx-min-height:110;-fx-max-width:110;-fx-max-height:110;-fx-effect:dropshadow(three-pass-box,rgba(0,70,67,0.2),10,0,0,4);");
        
        Label obtainScoreLbl = new Label(obtainMarks);
        obtainScoreLbl.setStyle("-fx-text-fill:white;-fx-font-size:32px;-fx-font-weight:bold;");
        
        Label totalScoreLbl = new Label("out of " + totalMarks);
        totalScoreLbl.setStyle("-fx-text-fill:#a5d6a7;-fx-font-size:10px;-fx-font-weight:bold;");
        
        bigScoreCircle.getChildren().addAll(obtainScoreLbl, totalScoreLbl);

        VBox percentageSection = new VBox(6);
        percentageSection.setAlignment(Pos.CENTER_LEFT);
        
        double pctVal = 0.0;
        try {
            double o = Double.parseDouble(obtainMarks);
            double t = Double.parseDouble(totalMarks);
            if (t > 0) pctVal = (o / t) * 100.0;
        } catch (NumberFormatException ex) {
            ErrorReporter.logError("CoursePortalTabView#buildNativeResultView parse score", ex);
        }
        
        Label pctLbl = new Label(String.format("%.1f%% Score", pctVal));
        pctLbl.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill: -color-accent;");
        
        Label feedbackLbl = new Label();
        if (pctVal >= 90) {
            feedbackLbl.setText("Outstanding achievement!");
            feedbackLbl.setStyle("-fx-text-fill:#15803d;-fx-font-weight:600;-fx-font-size:12px;");
        } else if (pctVal >= 80) {
            feedbackLbl.setText("Excellent effort!");
            feedbackLbl.setStyle("-fx-text-fill:#166534;-fx-font-weight:600;-fx-font-size:12px;");
        } else if (pctVal >= 60) {
            feedbackLbl.setText("Good score, keep it up.");
            feedbackLbl.setStyle("-fx-text-fill:#b45309;-fx-font-weight:600;-fx-font-size:12px;");
        } else {
            feedbackLbl.setText("Review recommended.");
            feedbackLbl.setStyle("-fx-text-fill:#b91c1c;-fx-font-weight:600;-fx-font-size:12px;");
        }
        
        percentageSection.getChildren().addAll(pctLbl, feedbackLbl);
        visualScoreContainer.getChildren().addAll(bigScoreCircle, percentageSection);
        
        scorecard.getChildren().addAll(visualScoreContainer, detailBox);
        wrapper.getChildren().add(scorecard);

        return wrapper;
    }

    private HBox resultRow(String key, String val, boolean bg) {
        HBox r = new HBox();
        r.setStyle("-fx-padding:12 16;-fx-border-color: -color-border;-fx-border-width:0 0 1 0;" + (bg ? "-fx-background-color: -color-bg-card;" : "-fx-background-color: -color-bg-main;"));
        
        Label kl = new Label(key);
        kl.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill: -color-text-muted;");
        kl.setMinWidth(150);

        Label vl = new Label(val);
        vl.setStyle("-fx-font-size:12px;-fx-text-fill: -color-text-main;-fx-font-weight:500;");
        vl.setWrapText(true);
        HBox.setHgrow(vl, Priority.ALWAYS);

        r.getChildren().addAll(kl, vl);
        return r;
    }

    private void renderWebViewPaper(String pageUrl, String title) {
        contentPane.getChildren().clear();
        VBox wrapper = new VBox(10);
        wrapper.setPadding(new Insets(16, 28, 16, 28));

        Button backBtn = new Button("← Back to MCQ Tests List");
        backBtn.setCursor(javafx.scene.Cursor.HAND);
        backBtn.setStyle("-fx-background-color: -color-bg-card;-fx-text-fill: -color-accent;-fx-border-color: -color-accent;-fx-border-width:1;-fx-border-radius:4;-fx-background-radius:4;-fx-font-size:12px;-fx-font-weight:bold;-fx-padding:6 12;");
        backBtn.setOnAction(e -> {
            activeTab = ""; // Force reload
            loadTab("portal_mcq");
        });

        HBox topBar = new HBox(backBtn);
        topBar.setStyle("-fx-padding:0 0 10 0;");

        WebPortalTabView webView = new WebPortalTabView(context, pageUrl, title);
        VBox.setVgrow(webView.getRoot(), Priority.ALWAYS);

        wrapper.getChildren().addAll(topBar, webView.getRoot());
        contentPane.getChildren().add(wrapper);
    }

    private void loadSubjectiveData() {
        showLoading("Loading Subjective Tests...");
        new Thread(() -> {
            try {
                String html = context.portalRepository().fetchPageHtml("CoursePortal.aspx?isTest=1");
                if (html == null) {
                    Platform.runLater(() -> {
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(buildErrorState(
                            "Unable to load Subjective Tests", 
                            "Failed to connect to the portal database. Please check your internet connection and try again."
                        ));
                    });
                    return;
                }

                try {
                    java.nio.file.Files.writeString(java.nio.file.Paths.get("cts_subjective_raw.html"), html);
                } catch (IOException ex) {
                    ErrorReporter.logError("CoursePortalTabView#loadSubjectiveData write cts_subjective_raw.html", ex);
                }

                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    try {
                        contentPane.getChildren().add(buildSubjectiveView(html));
                    } catch (Exception ex) {
                        ErrorReporter.notify(context, "Failed to parse Subjective Tests. Portal layout may have changed.", "CoursePortalTabView#loadSubjectiveData", ex);
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(buildErrorState(
                            "Parsing Error", 
                            "Failed to read the Subjective Tests table. The portal layout might have changed."
                        ));
                    }
                });
            } catch (Exception e) {
                ErrorReporter.notify(context, "Failed to load Subjective Tests. Please try again.", "CoursePortalTabView#loadSubjectiveData", e);
                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    contentPane.getChildren().add(buildErrorState(
                        "Loading Error",
                        "An unexpected error occurred while loading Subjective tests: " + e.getMessage()
                    ));
                });
            }
        }).start();
    }

    private ScrollPane buildSubjectiveView(String html) {
        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");

        VBox content = new VBox(16);
        content.setPadding(new Insets(16, 28, 24, 28));
        content.setFillWidth(true);

        Document doc = Jsoup.parse(html);
        
        String headingText = "Subjective Tests Summary";
        Element h3 = doc.select("h3").first();
        if (h3 != null && !h3.text().trim().isEmpty()) {
            headingText = h3.text().trim();
        }

        Label subHeading = new Label(headingText);
        subHeading.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill: -color-text-main;-fx-padding:0 0 4 0;");
        content.getChildren().add(subHeading);

        Element table = findMainGridTable(doc, "DataContent_gvPortalSummary");

        if (table != null) {
            String tableText = table.text().trim();
            if (tableText.contains("No Assignment Found") || tableText.contains("No Subjective Test")) {
                renderEmptySubjectiveState(content);
            } else {
                Elements rows = table.select("tr");
                if (rows.size() > 1) {
                    VBox tableCard = buildNativeSubjectiveTable(rows);
                    content.getChildren().add(tableCard);
                    content.setMinWidth(tableCard.getMinWidth() + 56);
                } else {
                    renderEmptySubjectiveState(content);
                }
            }
        } else {
            renderEmptySubjectiveState(content);
        }

        sp.setContent(content);
        return sp;
    }

    private void renderEmptySubjectiveState(VBox content) {
        VBox emptyCard = new VBox(12);
        emptyCard.setAlignment(Pos.CENTER);
        emptyCard.setPadding(new Insets(32, 24, 32, 24));
        emptyCard.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:8;-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:8;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.02),10,0,0,2);");

        Label icon = new Label("📝");
        icon.setStyle("-fx-font-size:28px;");

        Label label = new Label("No Subjective Tests Found");
        label.setStyle("-fx-text-fill: -color-text-muted;-fx-font-size:13px;-fx-font-weight:bold;");

        Label desc = new Label("There are currently no active or completed subjective tests/assignments listed for your registered courses.");
        desc.setStyle("-fx-text-fill: -color-text-muted;-fx-font-size:11px;-fx-text-alignment:center;");
        desc.setWrapText(true);

        emptyCard.getChildren().addAll(icon, label, desc);
        content.getChildren().add(emptyCard);
    }

    private VBox buildNativeSubjectiveTable(Elements rows) {
        VBox card = new VBox(0);
        card.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:8;-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:8;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.02),10,0,0,2);");
        
        Element headerRow = rows.first();
        if (headerRow == null) return card;

        Elements headerCells = headerRow.select("th, td");
        List<String> headers = new ArrayList<>();
        for (Element hc : headerCells) {
            headers.add(hc.text().trim());
        }

        double totalWidth = 0;
        for (int i = 0; i < headers.size(); i++) {
            totalWidth += subjectiveColW(headers.get(i), i, headers.size());
        }
        card.setMinWidth(totalWidth);

        HBox headerBox = new HBox(0);
        headerBox.setStyle("-fx-background-color: -color-bg-main;-fx-padding:12 0;-fx-border-color: -color-border;-fx-border-width:0 0 1 0;");
        for (int i = 0; i < headers.size(); i++) {
            Label hl = new Label(headers.get(i));
            hl.setStyle("-fx-font-size:11px;-fx-font-weight:700;-fx-text-fill: -color-text-muted;-fx-padding:0 12;");
            double w = subjectiveColW(headers.get(i), i, headers.size());
            hl.setMinWidth(w);
            hl.setMaxWidth(w);
            headerBox.getChildren().add(hl);
        }
        card.getChildren().add(headerBox);

        for (int r = 1; r < rows.size(); r++) {
            Element row = rows.get(r);
            Elements cells = row.select("td");
            if (cells.isEmpty()) continue;

            HBox dataRow = new HBox(0);
            String bg = (r % 2 == 0) ? "-color-bg-card" : "-color-bg-main";
            dataRow.setStyle("-fx-background-color:" + bg + ";-fx-padding:12 0;-fx-border-color: -color-border;-fx-border-width:0 0 1 0;");

            for (int c = 0; c < cells.size(); c++) {
                if (c >= headers.size()) break;

                Element cell = cells.get(c);
                String txt = cell.text().trim().replaceAll("\\s+", " ");
                double w = subjectiveColW(headers.get(c), c, headers.size());
                Node cellNode;

                Element aTag = cell.select("a").first();
                if (aTag != null && !aTag.attr("href").isEmpty()) {
                    String linkText = aTag.text().trim();
                    String href = aTag.attr("href");

                    Button actBtn = new Button(linkText);
                    actBtn.setCursor(javafx.scene.Cursor.HAND);
                    actBtn.setStyle("-fx-background-color: -color-accent;-fx-text-fill: -color-bg-main;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:4;-fx-padding:4 10;");
                    
                    actBtn.setOnAction(e -> loadSubjectivePaperView(href, linkText));

                    HBox btnWrapper = new HBox(actBtn);
                    btnWrapper.setAlignment(Pos.CENTER_LEFT);
                    btnWrapper.setPadding(new Insets(0, 12, 0, 12));
                    btnWrapper.setMinWidth(w);
                    btnWrapper.setMaxWidth(w);
                    cellNode = btnWrapper;
                } else {
                    Label cl = new Label(txt);
                    cl.setWrapText(true);
                    cl.setStyle("-fx-font-size:12px;-fx-text-fill: -color-text-main;-fx-padding:0 12;");
                    cl.setMinWidth(w);
                    cl.setMaxWidth(w);
                    cellNode = cl;
                }

                dataRow.getChildren().add(cellNode);
            }
        }
        return card;
    }

    private double subjectiveColW(String h, int i, int total) {
        String l = h.toLowerCase();
        if (l.contains("#")) return 50;
        if (l.contains("test") || l.contains("title") || l.contains("assignment")) return 200;
        if (l.contains("course")) return 280;
        if (l.contains("date") || l.contains("time") || l.contains("due")) return 180;
        if (l.contains("action") || l.contains("status")) return 140;
        return 120;
    }

    private void loadSubjectivePaperView(String relativeUrl, String title) {
        showLoading("Loading Subjective Test details...");

        new Thread(() -> {
            try {
                String html;
                if (context.portalRepository().isPostBackDownloadLink(relativeUrl)) {
                    PortalRepository.PostBackLink postBackLink = context.portalRepository().extractPostBackLinkFromLink(relativeUrl);
                    if (postBackLink == null) {
                        throw new RuntimeException("Invalid postback link: " + relativeUrl);
                    }
                    String sourcePageUrl = postBackLink.sourcePageUrl();
                    if (sourcePageUrl == null || sourcePageUrl.isBlank()) {
                        sourcePageUrl = "CoursePortal.aspx?isTest=1";
                    }
                    html = context.portalRepository().postbackEvent(sourcePageUrl, postBackLink.info().target());
                } else {
                    String urlTemp = relativeUrl;
                    if (!urlTemp.startsWith("CTS/") && !urlTemp.startsWith("/CTS/")) {
                        urlTemp = "CTS/" + urlTemp;
                    }
                    html = context.portalRepository().fetchPageHtml(urlTemp);
                }

                if (html == null) {
                    Platform.runLater(() -> {
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(buildErrorState(
                            "Connection Error",
                            "Failed to load Subjective Test details. Please check your internet connection."
                        ));
                    });
                    return;
                }

                Document doc = Jsoup.parse(html);
                
                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    try {
                        contentPane.getChildren().add(buildNativeSubjectiveDetailView(doc, relativeUrl, title));
                    } catch (Exception ex) {
                        ErrorReporter.logError("CoursePortalTabView#loadSubjectivePaperView native render error, falling back to WebView", ex);
                        renderWebViewPaper(relativeUrl, title);
                    }
                });
            } catch (Exception e) {
                ErrorReporter.notify(context, "Failed to load Subjective Test details. Please try again.", "CoursePortalTabView#loadSubjectiveDetails", e);
                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    contentPane.getChildren().add(buildErrorState(
                        "Loading Error",
                        "An unexpected error occurred while loading Subjective details: " + e.getMessage()
                    ));
                });
            }
        }).start();
    }

    private ScrollPane buildNativeSubjectiveDetailView(Document doc, String relativeUrl, String title) {
        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");

        VBox wrapper = new VBox(20);
        wrapper.setPadding(new Insets(16, 28, 24, 28));
        wrapper.setFillWidth(true);

        Button backBtn = new Button("← Back to Subjective Tests List");
        backBtn.setCursor(javafx.scene.Cursor.HAND);
        backBtn.setStyle("-fx-background-color: -color-bg-card;-fx-text-fill: -color-accent;-fx-border-color: -color-accent;-fx-border-width:1;-fx-border-radius:4;-fx-background-radius:4;-fx-font-size:12px;-fx-font-weight:bold;-fx-padding:6 12;");
        backBtn.setOnAction(e -> {
            activeTab = ""; // Force reload
            loadTab("portal_subjective");
        });

        HBox topBar = new HBox(backBtn);
        topBar.setStyle("-fx-padding:0 0 4 0;");
        wrapper.getChildren().add(topBar);

        String headingText = title;
        Element h3 = doc.select("h3").first();
        if (h3 != null && !h3.text().trim().isEmpty()) {
            headingText = h3.text().trim();
        }

        Label titleLabel = new Label(headingText);
        titleLabel.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill: -color-text-main;");

        java.util.Map<String, String> metadata = new java.util.LinkedHashMap<>();
        
        record AttachmentInfo(String filename, String href) {}
        List<AttachmentInfo> teacherAttachments = new ArrayList<>();
        
        String submittedFilename = null;
        String submissionDate = null;
        String obtainedMarks = null;

        // Parse key-value metadata rows from tables
        for (Element table : doc.select("table")) {
            String tableId = table.id().toLowerCase();
            if (tableId.contains("sidebar") || tableId.contains("menu")) continue;
            
            for (Element row : table.select("tr")) {
                Elements cells = row.select("td, th");
                if (cells.size() == 2) {
                    String key = cells.get(0).text().trim().replaceAll(":$", "").trim();
                    Element valCell = cells.get(1);
                    String val = valCell.text().trim();
                    
                    if (!key.isEmpty() && !val.isEmpty()) {
                        val = val.replaceAll("\\s+", " ");
                        
                        Elements anchors = valCell.select("a");
                        for (Element a : anchors) {
                            String href = a.attr("href");
                            String onClick = a.attr("onclick");
                            String aText = a.text().trim();
                            
                            if (href.toLowerCase().contains("download") || href.toLowerCase().contains("file") || 
                                onClick.toLowerCase().contains("download") || onClick.toLowerCase().contains("file") ||
                                href.toLowerCase().contains("assignmentfiles.aspx") || href.toLowerCase().contains("courseportalassignmentfiles.aspx")) {
                                
                                String finalHref = href;
                                if (finalHref.isEmpty() || finalHref.equals("#")) {
                                    PortalRepository.PostBackInfo postBackInfo = context.portalRepository().extractPostBackInfo(onClick);
                                    if (postBackInfo != null) {
                                        finalHref = context.portalRepository().toPostBackDownloadLink(postBackInfo, null);
                                    }
                                }
                                if (!finalHref.isEmpty() && !finalHref.equals("#")) {
                                    teacherAttachments.add(new AttachmentInfo(aText.isEmpty() ? "Download Instruction File" : aText, finalHref));
                                }
                            }
                        }
                        
                        String keyLower = key.toLowerCase();
                        if (keyLower.contains("submitted file") || keyLower.contains("submission file")) {
                            submittedFilename = val;
                        } else if (keyLower.contains("submission date") || keyLower.contains("submitted date")) {
                            submissionDate = val;
                        } else if (keyLower.contains("obtain") || keyLower.contains("grade") || keyLower.contains("score")) {
                            obtainedMarks = val;
                        }
                        
                        metadata.put(key, val);
                    }
                }
            }
        }

        // Fallback: search for explicit label tags or spans on the page with details
        for (Element span : doc.select("span[id*='lbl'], span[id*='lbl_']")) {
            String id = span.id().toLowerCase();
            String val = span.text().trim();
            if (val.isEmpty()) continue;
            
            if (id.contains("lblcourse") && !metadata.containsKey("Course")) {
                metadata.put("Course", val);
            } else if (id.contains("lbltitle") && !metadata.containsKey("Title")) {
                metadata.put("Title", val);
            } else if (id.contains("lbltotalmarks") && !metadata.containsKey("Total Marks")) {
                metadata.put("Total Marks", val);
            } else if (id.contains("lbldeadline") && !metadata.containsKey("Deadline")) {
                metadata.put("Deadline", val);
            } else if (id.contains("lblstatus") && !metadata.containsKey("Status")) {
                metadata.put("Status", val);
            } else if (id.contains("lblsubmittedfile") && !metadata.containsKey("Submitted File")) {
                submittedFilename = val;
                metadata.put("Submitted File", val);
            } else if (id.contains("lblsubmissiondate") && !metadata.containsKey("Submission Date")) {
                submissionDate = val;
                metadata.put("Submission Date", val);
            }
        }

        // Search in all anchors for potential download files
        for (Element a : doc.select("a[href]")) {
            String href = a.attr("href");
            String aText = a.text().trim();
            if (href.toLowerCase().contains("assignmentfiles.aspx") || href.toLowerCase().contains("courseportalassignmentfiles.aspx")) {
                boolean exists = false;
                for (AttachmentInfo exist : teacherAttachments) {
                    if (exist.href().equals(href)) { exists = true; break; }
                }
                if (!exists) {
                    teacherAttachments.add(new AttachmentInfo(aText.isEmpty() ? "Download Assignment File" : aText, href));
                }
            }
        }

        if (metadata.isEmpty()) {
            throw new RuntimeException("Could not parse test details from page.");
        }

        // 1. Info Card
        VBox scorecard = new VBox(20);
        scorecard.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:12;-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:12;-fx-padding:24;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.03),15,0,0,3);");
        
        HBox scoreHeader = new HBox(12);
        scoreHeader.setAlignment(Pos.CENTER_LEFT);
        
        Label statusBadge = new Label("SUBJECTIVE TEST");
        statusBadge.getStyleClass().add("badge-info");
        
        scoreHeader.getChildren().addAll(titleLabel, statusBadge);
        scorecard.getChildren().add(scoreHeader);

        VBox detailBox = new VBox(0);
        detailBox.setStyle("-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:8;-fx-background-radius:8;-fx-background-color: -color-bg-main;");
        
        int rowIndex = 0;
        for (java.util.Map.Entry<String, String> entry : metadata.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            
            if (key.length() > 50 || val.length() > 300) continue;
            if (key.toLowerCase().contains("validation") || key.toLowerCase().contains("error")) continue;
            
            boolean bg = (rowIndex % 2 == 0);
            detailBox.getChildren().add(resultRow(key, val, bg));
            rowIndex++;
        }
        
        scorecard.getChildren().add(detailBox);
        wrapper.getChildren().add(scorecard);

        // 2. Attachments Card
        if (!teacherAttachments.isEmpty()) {
            VBox attachmentsCard = new VBox(12);
            attachmentsCard.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:12;-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:12;-fx-padding:20;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.02),10,0,0,2);");
            
            Label attachTitle = new Label("Attachments / Instructions Files");
            attachTitle.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill: -color-text-main;");
            attachmentsCard.getChildren().add(attachTitle);
            
            VBox fileList = new VBox(8);
            for (AttachmentInfo attach : teacherAttachments) {
                HBox fileRow = new HBox(12);
                fileRow.setAlignment(Pos.CENTER_LEFT);
                fileRow.setStyle("-fx-padding:8 12;-fx-background-color: -color-bg-main;-fx-background-radius:6;-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:6;");
                
                Label fileIcon = new Label("📄");
                fileIcon.setStyle("-fx-font-size:16px;");
                
                Label fileName = new Label(attach.filename());
                fileName.setStyle("-fx-font-size:12px;-fx-text-fill: -color-text-main;-fx-font-weight:bold;");
                HBox.setHgrow(fileName, Priority.ALWAYS);
                
                Button dlBtn = new Button("Download");
                dlBtn.setCursor(javafx.scene.Cursor.HAND);
                dlBtn.setStyle("-fx-background-color: -color-accent;-fx-text-fill: -color-bg-main;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:4;-fx-padding:4 10;");
                
                final String finalCourse = metadata.getOrDefault("Course", "Subjective");
                dlBtn.setOnAction(e -> triggerAssignmentDownload(attach.href(), attach.filename(), finalCourse, dlBtn));
                
                fileRow.getChildren().addAll(fileIcon, fileName, dlBtn);
                fileList.getChildren().add(fileRow);
            }
            attachmentsCard.getChildren().add(fileList);
            wrapper.getChildren().add(attachmentsCard);
        }

        // 3. Submission Card
        VBox submissionCard = new VBox(14);
        submissionCard.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:12;-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:12;-fx-padding:20;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.02),10,0,0,2);");
        
        Label subTitle = new Label("Your Submission");
        subTitle.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill: -color-text-main;");
        submissionCard.getChildren().add(subTitle);
        
        boolean hasSubmitted = (submittedFilename != null && !submittedFilename.isEmpty() && !submittedFilename.equals("-") && !submittedFilename.toLowerCase().contains("not submitted"));
        
        if (hasSubmitted) {
            VBox subInfo = new VBox(8);
            subInfo.getStyleClass().add("panel-info-success");
            
            HBox statusRow = new HBox(8);
            statusRow.setAlignment(Pos.CENTER_LEFT);
            Label checkIcon = new Label("✅");
            checkIcon.setStyle("-fx-font-size:16px;");
            Label statusLbl = new Label("Submitted Successfully");
            statusLbl.getStyleClass().add("status-success");
            statusLbl.setStyle("-fx-font-size:13px;");
            statusRow.getChildren().addAll(checkIcon, statusLbl);
            
            Label fileLbl = new Label("File: " + submittedFilename);
            fileLbl.setStyle("-fx-font-size:12px;-fx-text-fill: -color-text-main;-fx-font-weight:bold;");
            
            subInfo.getChildren().addAll(statusRow, fileLbl);
            if (submissionDate != null && !submissionDate.isEmpty()) {
                Label dateLbl = new Label("Submitted on: " + submissionDate);
                dateLbl.getStyleClass().add("status-success");
                dateLbl.setStyle("-fx-font-size:11px;");
                subInfo.getChildren().add(dateLbl);
            }
            submissionCard.getChildren().add(subInfo);
        } else {
            VBox noSubInfo = new VBox(8);
            noSubInfo.getStyleClass().add("panel-info-danger");
            
            HBox statusRow = new HBox(8);
            statusRow.setAlignment(Pos.CENTER_LEFT);
            Label warningIcon = new Label("⚠️");
            warningIcon.setStyle("-fx-font-size:16px;");
            Label statusLbl = new Label("Not Submitted");
            statusLbl.getStyleClass().add("status-error");
            statusLbl.setStyle("-fx-font-size:13px;");
            statusRow.getChildren().addAll(warningIcon, statusLbl);
            
            Label descLbl = new Label("You have not uploaded any solution file for this test yet.");
            descLbl.getStyleClass().add("status-error");
            descLbl.setStyle("-fx-font-size:11px;");
            
            noSubInfo.getChildren().addAll(statusRow, descLbl);
            submissionCard.getChildren().add(noSubInfo);
        }
        
        Element fileInput = doc.select("input[type=file]").first();
        boolean openForUpload = (fileInput != null);
        
        if (openForUpload) {
            VBox uploadForm = new VBox(10);
            uploadForm.setStyle("-fx-padding:16 0 0 0;-fx-border-color: -color-border;-fx-border-width:1 0 0 0;");
            
            Label selectFileLbl = new Label(hasSubmitted ? "Update Submission File:" : "Upload Submission File:");
            selectFileLbl.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill: -color-text-main;");
            
            Label selectedFileLabel = new Label("No file chosen");
            selectedFileLabel.setStyle("-fx-font-size:11px;-fx-text-fill: -color-text-muted;");
            
            Button browseBtn = new Button("📁 Choose File...");
            browseBtn.setCursor(javafx.scene.Cursor.HAND);
            browseBtn.setStyle("-fx-background-color: -color-bg-main;-fx-text-fill: -color-text-main;-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:4;-fx-background-radius:4;-fx-font-size:11px;-fx-padding:5 10;");
            
            final File[] selectedFileHolder = new File[1];
            browseBtn.setOnAction(e -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Select Solution File to Upload");
                fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("All Files", "*.*"),
                    new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                    new FileChooser.ExtensionFilter("ZIP Files", "*.zip"),
                    new FileChooser.ExtensionFilter("Word Documents", "*.doc", "*.docx")
                );
                File selected = fileChooser.showOpenDialog(context.stage());
                if (selected != null) {
                    selectedFileHolder[0] = selected;
                    selectedFileLabel.setText(selected.getName() + " (" + formatFileSize(selected.length()) + ")");
                    selectedFileLabel.setStyle("-fx-font-size:11px;-fx-text-fill:#15803d;-fx-font-weight:bold;");
                }
            });
            
            Button submitBtn = new Button(hasSubmitted ? "Change File Submission" : "Submit File");
            submitBtn.setCursor(javafx.scene.Cursor.HAND);
            submitBtn.setStyle("-fx-background-color: -color-accent;-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:bold;-fx-background-radius:4;-fx-padding:6 14;");
            submitBtn.setDisable(true);
            
            selectedFileLabel.textProperty().addListener((o, oldVal, newVal) -> {
                submitBtn.setDisable(selectedFileHolder[0] == null);
            });
            
            submitBtn.setOnAction(e -> {
                if (selectedFileHolder[0] != null) {
                    triggerNativeSubjectiveUpload(relativeUrl, selectedFileHolder[0], submitBtn, title);
                }
            });
            
            HBox browseRow = new HBox(8, browseBtn, selectedFileLabel);
            browseRow.setAlignment(Pos.CENTER_LEFT);
            
            HBox submitRow = new HBox(submitBtn);
            submitRow.setStyle("-fx-padding:8 0 0 0;");
            
            uploadForm.getChildren().addAll(selectFileLbl, browseRow, submitRow);
            submissionCard.getChildren().add(uploadForm);
            
            uploadButtons.add(submitBtn);
            applyOfflineStateIfOffline(submitBtn, submitBtn.getText());
        } else {
            Label closedLbl = new Label("🔒 Test submission is closed.");
            closedLbl.setStyle("-fx-text-fill:#b91c1c;-fx-font-size:12px;-fx-font-weight:bold;-fx-padding:10 0 0 0;");
            submissionCard.getChildren().add(closedLbl);
        }
        
        wrapper.getChildren().add(submissionCard);
        
        sp.setContent(wrapper);
        return sp;
    }

    private void triggerNativeSubjectiveUpload(String submitUrl, File file, Button btn, String title) {
        if (!context.isOnline()) {
            showErrorDialog("Upload Offline", "Cannot upload files in offline mode.");
            return;
        }
        
        if (btn != null) {
            Platform.runLater(() -> btn.setDisable(true));
        }
        showLoading("Uploading solution file...");
        
        new Thread(() -> {
            try {
                PortalRepository.UploadResult result = context.portalRepository().uploadAssignment(submitUrl, file);
                
                Platform.runLater(() -> {
                    if (btn != null) btn.setDisable(false);
                    
                    if (result instanceof PortalRepository.UploadResult.Success) {
                        showSuccessDialog("Upload Complete", "Subjective solution uploaded successfully!");
                        loadSubjectivePaperView(submitUrl, title);
                    } else {
                        loadSubjectivePaperView(submitUrl, title);
                        
                        if (result instanceof PortalRepository.UploadResult.NetworkError) {
                            showErrorDialog("Upload Failed", "Network connection error. Please try again.");
                        } else if (result instanceof PortalRepository.UploadResult.Timeout) {
                            showErrorDialog("Upload Failed", "Request timed out. The server might be slow or file might be too large.");
                        } else if (result instanceof PortalRepository.UploadResult.Rejected rejected) {
                            showErrorDialog("Upload Rejected", rejected.reason());
                        } else if (result instanceof PortalRepository.UploadResult.Error err) {
                            showErrorDialog("Upload Failed", err.message());
                        }
                    }
                });
            } catch (Exception ex) {
                ErrorReporter.logError("CoursePortalTabView#triggerNativeSubjectiveUpload", ex);
                Platform.runLater(() -> {
                    if (btn != null) btn.setDisable(false);
                    loadSubjectivePaperView(submitUrl, title);
                    showErrorDialog("Upload Error", "An unexpected error occurred: " + ex.getMessage());
                });
            }
        }).start();
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private void loadAssignmentsData() {
        showLoading("Loading Assignments Summary...");
        new Thread(() -> {
            try {
                String html = context.portalRepository().fetchPageHtml("CoursePortal.aspx");
                if (html == null) {
                    Platform.runLater(() -> {
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(buildErrorState(
                            "Unable to load Assignments Summary",
                            "Failed to connect to the portal database. Please check your internet connection and try again."
                        ));
                    });
                    return;
                }

                try {
                    java.nio.file.Files.writeString(java.nio.file.Paths.get("cts_assignments_raw.html"), html);
                } catch (IOException ex) {
                    ErrorReporter.logError("CoursePortalTabView#loadAssignmentsData write cts_assignments_raw.html", ex);
                }

                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    try {
                        contentPane.getChildren().add(buildAssignmentsView(html));
                    } catch (Exception ex) {
                        ErrorReporter.notify(context, "Failed to parse Assignments Summary. Portal layout may have changed.", "CoursePortalTabView#loadAssignmentsData", ex);
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(buildErrorState(
                            "Parsing Error",
                            "Failed to read the Assignments Summary table cleanly."
                        ));
                    }
                });
            } catch (Exception e) {
                ErrorReporter.notify(context, "Failed to load Assignments Summary. Please try again.", "CoursePortalTabView#loadAssignmentsData", e);
                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    contentPane.getChildren().add(buildErrorState(
                        "Loading Error",
                        "An unexpected error occurred while loading assignments: " + e.getMessage()
                    ));
                });
            }
        }).start();
    }

    private ScrollPane buildAssignmentsView(String html) {
        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");

        VBox content = new VBox(16);
        content.setPadding(new Insets(16, 28, 24, 28));
        content.setFillWidth(true);

        Document doc = Jsoup.parse(html);
        
        String headingText = "Course Portal Summary";
        Element h3 = doc.select("h3").first();
        if (h3 != null && !h3.text().trim().isEmpty()) {
            headingText = h3.text().trim();
        }

        Label subHeading = new Label(headingText);
        subHeading.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill: -color-text-main;-fx-padding:0 0 4 0;");
        content.getChildren().add(subHeading);

        Element table = findMainGridTable(doc, "DataContent_gvPortalSummary");

        if (table != null) {
            String tableText = table.text().trim();
            if (tableText.contains("No Assignment Found")) {
                renderEmptyAssignmentsState(content);
            } else {
                Elements rows = table.select("tr");
                if (rows.size() > 1) {
                    VBox tableCard = buildNativeAssignmentsTable(rows);
                    content.getChildren().add(tableCard);
                    content.setMinWidth(tableCard.getMinWidth() + 56);
                } else {
                    renderEmptyAssignmentsState(content);
                }
            }
        } else {
            renderEmptyAssignmentsState(content);
        }

        sp.setContent(content);
        return sp;
    }

    private void renderEmptyAssignmentsState(VBox content) {
        VBox emptyCard = new VBox(12);
        emptyCard.setAlignment(Pos.CENTER);
        emptyCard.setPadding(new Insets(32, 24, 32, 24));
        emptyCard.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:8;-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:8;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.02),10,0,0,2);");

        Label icon = new Label("📋");
        icon.setStyle("-fx-font-size:28px;");

        Label label = new Label("No Assignments Found");
        label.setStyle("-fx-text-fill: -color-text-muted;-fx-font-size:13px;-fx-font-weight:bold;");

        Label desc = new Label("There are currently no active or closed assignments listed in your course portal.");
        desc.setStyle("-fx-text-fill: -color-text-muted;-fx-font-size:11px;-fx-text-alignment:center;");
        desc.setWrapText(true);

        emptyCard.getChildren().addAll(icon, label, desc);
        content.getChildren().add(emptyCard);
    }

    private VBox buildNativeAssignmentsTable(Elements rows) {
        VBox card = new VBox(0);
        card.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:8;-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:8;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.02),10,0,0,2);");
        
        Element headerRow = rows.first();
        if (headerRow == null) return card;

        Elements headerCells = headerRow.select("th, td");
        List<String> headers = new ArrayList<>();
        for (Element hc : headerCells) {
            headers.add(hc.text().trim());
        }

        double totalWidth = 0;
        for (int i = 0; i < headers.size(); i++) {
            totalWidth += assignmentsColW(headers.get(i), i, headers.size());
        }
        card.setMinWidth(totalWidth);

        HBox headerBox = new HBox(0);
        headerBox.setStyle("-fx-background-color: -color-bg-main;-fx-padding:12 0;-fx-border-color: -color-border;-fx-border-width:0 0 1 0;");
        for (int i = 0; i < headers.size(); i++) {
            Label hl = new Label(headers.get(i));
            hl.setStyle("-fx-font-size:11px;-fx-font-weight:700;-fx-text-fill: -color-text-muted;-fx-padding:0 12;");
            double w = assignmentsColW(headers.get(i), i, headers.size());
            hl.setMinWidth(w);
            hl.setMaxWidth(w);
            headerBox.getChildren().add(hl);
        }
        card.getChildren().add(headerBox);

        for (int r = 1; r < rows.size(); r++) {
            Element row = rows.get(r);
            Elements cells = row.select("td");
            if (cells.isEmpty()) continue;

            HBox dataRow = new HBox(0);
            dataRow.setAlignment(Pos.CENTER_LEFT);
            String bg = (r % 2 == 0) ? "-color-bg-card" : "-color-bg-main";
            dataRow.setStyle("-fx-background-color:" + bg + ";-fx-padding:12 0;-fx-border-color: -color-border;-fx-border-width:0 0 1 0;");

            String courseTitle = "";
            String assignmentTitle = "";
            boolean isRowClosed = false;

            // Scan columns for status first to determine closed state
            for (int c = 0; c < cells.size(); c++) {
                if (c >= headers.size()) break;
                String colHeader = headers.get(c).toLowerCase();
                if (colHeader.contains("status")) {
                    String statusText = cells.get(c).text().toLowerCase();
                    if (statusText.contains("closed")) {
                        isRowClosed = true;
                    }
                }
            }

            for (int c = 0; c < cells.size(); c++) {
                if (c >= headers.size()) break;

                Element cell = cells.get(c);
                String txt = cell.text().trim().replaceAll("\\s+", " ");
                double w = assignmentsColW(headers.get(c), c, headers.size());
                Node cellNode;

                if (c == 1) courseTitle = txt;
                if (c == 2) assignmentTitle = txt;

                String colHeader = headers.get(c).toLowerCase();

                if (colHeader.contains("submission")) {
                    Label badge = new Label(txt);
                    boolean isSubmitted = txt.toLowerCase().contains("submitted") && !txt.toLowerCase().contains("not");
                    if (isSubmitted) {
                        badge.getStyleClass().add("badge-success");
                    } else {
                        badge.getStyleClass().add("badge-danger");
                    }
                    HBox wrapper = new HBox(badge);
                    wrapper.setAlignment(Pos.CENTER_LEFT);
                    wrapper.setPadding(new Insets(0, 12, 0, 12));
                    wrapper.setMinWidth(w);
                    wrapper.setMaxWidth(w);
                    cellNode = wrapper;
                } else if (colHeader.contains("status")) {
                    Label badge = new Label(txt);
                    if (isRowClosed) {
                        badge.getStyleClass().add("badge-muted");
                    } else {
                        badge.getStyleClass().add("badge-info");
                    }
                    HBox wrapper = new HBox(badge);
                    wrapper.setAlignment(Pos.CENTER_LEFT);
                    wrapper.setPadding(new Insets(0, 12, 0, 12));
                    wrapper.setMinWidth(w);
                    wrapper.setMaxWidth(w);
                    cellNode = wrapper;
                } else if (colHeader.contains("download") || colHeader.contains("dowload")) {
                    String href = context.portalRepository().extractAssignmentDownloadLink(cell);
                    Button dlBtn = new Button("Download");
                    dlBtn.setCursor(javafx.scene.Cursor.HAND);
                    dlBtn.setStyle("-fx-background-color: -color-accent;-fx-text-fill: -color-bg-main;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:4;-fx-padding:4 10;");
                    
                    final String finalCourse = courseTitle;
                    final String finalAssign = assignmentTitle;

                    if (href == null || href.isBlank()) {
                        dlBtn.setOnAction(e -> showWarningDialog("Download Assignment", "No assignment files have been uploaded by the teacher."));
                    } else {
                        dlBtn.setOnAction(e -> triggerAssignmentDownload(href, finalAssign, finalCourse, dlBtn));
                    }

                    HBox wrapper = new HBox(dlBtn);
                    wrapper.setAlignment(Pos.CENTER_LEFT);
                    wrapper.setPadding(new Insets(0, 12, 0, 12));
                    wrapper.setMinWidth(w);
                    wrapper.setMaxWidth(w);
                    cellNode = wrapper;
                } else if (colHeader.contains("submit")) {
                    if (!isRowClosed) {
                        String submitUrl = context.portalRepository().extractAssignmentSubmitLink(cell, "CoursePortal.aspx");
                        if (!submitUrl.isEmpty()) {
                            String btnText = "Submit";
                            Element aOrInput = cell.select("a, button, input[type=submit], input[type=button]").first();
                            if (aOrInput != null) {
                                String val = aOrInput.attr("value").trim();
                                if (val.isEmpty()) val = aOrInput.text().trim();
                                if (!val.isEmpty()) btnText = val;
                            }
                            Button subBtn = new Button(btnText);
                            subBtn.setCursor(javafx.scene.Cursor.HAND);
                            subBtn.setStyle("-fx-background-color: -color-accent;-fx-text-fill:white;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:4;-fx-padding:4 10;");
                            subBtn.setOnAction(e -> triggerAssignmentUpload(submitUrl, subBtn));
                            uploadButtons.add(subBtn);
                            applyOfflineStateIfOffline(subBtn, btnText);

                            HBox wrapper = new HBox(subBtn);
                            wrapper.setAlignment(Pos.CENTER_LEFT);
                            wrapper.setPadding(new Insets(0, 12, 0, 12));
                            wrapper.setMinWidth(w);
                            wrapper.setMaxWidth(w);
                            cellNode = wrapper;
                        } else {
                            Label cl = new Label(txt.isEmpty() ? "-" : txt);
                            cl.setStyle("-fx-font-size:12px;-fx-text-fill: -color-text-muted;-fx-padding:0 12;");
                            cl.setMinWidth(w);
                            cl.setMaxWidth(w);
                            cellNode = cl;
                        }
                    } else {
                        Label cl = new Label(txt);
                        if (txt.toLowerCase().contains("closed")) {
                            cl.setStyle("-fx-font-size:11px;-fx-text-fill:#b91c1c;-fx-font-weight:bold;-fx-padding:0 12;");
                        } else {
                            cl.setStyle("-fx-font-size:12px;-fx-text-fill: -color-text-main;-fx-padding:0 12;");
                        }
                        cl.setMinWidth(w);
                        cl.setMaxWidth(w);
                        cellNode = cl;
                    }
                } else {
                    Label cl = new Label(txt);
                    cl.setWrapText(true);
                    cl.setStyle("-fx-font-size:12px;-fx-text-fill: -color-text-main;-fx-padding:0 12;");
                    cl.setMinWidth(w);
                    cl.setMaxWidth(w);
                    cellNode = cl;
                }

                dataRow.getChildren().add(cellNode);
            }
            card.getChildren().add(dataRow);
        }

        return card;
    }

    private void triggerAssignmentDownload(String href, String assignmentTitle, String courseTitle, Button btn) {
        if (btn != null) {
            Platform.runLater(() -> btn.setDisable(true));
        }

        new Thread(() -> {
            try {
                PortalRepository.DownloadResult result = context.portalRepository().downloadAssignment(href);

                Platform.runLater(() -> {
                    if (btn != null) btn.setDisable(false);

                    if (result instanceof PortalRepository.DownloadResult.Success success) {
                        String extension = "";
                        String fileName = success.fileName();
                        if (fileName != null && !fileName.isBlank()) {
                            int dotIdx = fileName.lastIndexOf('.');
                            if (dotIdx > 0) {
                                extension = fileName.substring(dotIdx);
                            }
                        }
                        if (extension.isEmpty() && success.mimeType() != null) {
                            String extFromMime = context.portalRepository().getExtensionFromMimeType(success.mimeType());
                            if (extFromMime != null && !extFromMime.isEmpty() && !extFromMime.equals("bin")) {
                                extension = "." + extFromMime;
                            }
                        }
                        if (extension.isEmpty()) {
                            extension = ".pdf";
                        }

                        final String finalFilename = getCustomFilename(assignmentTitle, courseTitle, extension);
                        final byte[] fileBytes = success.bytes();

                        FileChooser fileChooser = new FileChooser();
                        fileChooser.setTitle("Save Assignment Document");
                        fileChooser.setInitialFileName(finalFilename);

                        if (!extension.isEmpty()) {
                            fileChooser.getExtensionFilters().add(
                                new FileChooser.ExtensionFilter(extension.toUpperCase().substring(1) + " Files", "*" + extension)
                            );
                        }
                        fileChooser.getExtensionFilters().add(
                            new FileChooser.ExtensionFilter("All Files", "*.*")
                        );

                        File file = fileChooser.showSaveDialog(context.stage());
                        if (file != null) {
                            new Thread(() -> {
                                try {
                                    java.nio.file.Files.write(file.toPath(), fileBytes);
                                    Platform.runLater(() -> showSuccessDialog("Download Complete", "File saved successfully: " + file.getName()));
                                } catch (Exception ex) {
                                    ErrorReporter.logError("CoursePortalTabView#triggerAssignmentDownload save file", ex);
                                    Platform.runLater(() -> showErrorDialog("Download Failed", "Error saving file: " + ex.getMessage()));
                                }
                            }).start();
                        }
                    } else if (result instanceof PortalRepository.DownloadResult.NetworkError) {
                        showErrorDialog("Download Error", "Network connection failed. Please check your internet connection.");
                    } else if (result instanceof PortalRepository.DownloadResult.Rejected rejected) {
                        showErrorDialog("Download Rejected", rejected.reason());
                    } else if (result instanceof PortalRepository.DownloadResult.Error err) {
                        showErrorDialog("Download Error", err.message());
                    }
                });
            } catch (Exception ex) {
                ErrorReporter.logError("CoursePortalTabView#triggerAssignmentDownload", ex);
                Platform.runLater(() -> {
                    if (btn != null) btn.setDisable(false);
                    showErrorDialog("Download Error", "An unexpected error occurred: " + ex.getMessage());
                });
            }
        }).start();
    }

    private void triggerAssignmentUpload(String submitUrl, Button btn) {
        if (!context.isOnline()) {
            showErrorDialog("Upload Offline", "Cannot upload assignments in offline mode.");
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Assignment File to Upload");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("All Files", "*.*"),
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
            new FileChooser.ExtensionFilter("ZIP Files", "*.zip"),
            new FileChooser.ExtensionFilter("Word Documents", "*.doc", "*.docx")
        );
        
        File selectedFile = fileChooser.showOpenDialog(context.stage());
        if (selectedFile == null) {
            return;
        }

        if (btn != null) {
            Platform.runLater(() -> btn.setDisable(true));
        }
        showLoading("Uploading assignment file...");

        new Thread(() -> {
            try {
                PortalRepository.UploadResult result = context.portalRepository().uploadAssignment(submitUrl, selectedFile);

                Platform.runLater(() -> {
                    if (btn != null) btn.setDisable(false);
                    
                    if (result instanceof PortalRepository.UploadResult.Success) {
                        showSuccessDialog("Upload Complete", "Assignment uploaded successfully!");
                        loadAssignmentsData();
                    } else {
                        loadAssignmentsData();
                        if (result instanceof PortalRepository.UploadResult.NetworkError) {
                            showErrorDialog("Upload Failed", "Network connection error. Please try again.");
                        } else if (result instanceof PortalRepository.UploadResult.Timeout) {
                            showErrorDialog("Upload Failed", "Request timed out. The server might be slow or file might be too large.");
                        } else if (result instanceof PortalRepository.UploadResult.Rejected rejected) {
                            showErrorDialog("Upload Rejected", rejected.reason());
                        } else if (result instanceof PortalRepository.UploadResult.Error err) {
                            showErrorDialog("Upload Failed", err.message());
                        }
                    }
                });
            } catch (Exception ex) {
                ErrorReporter.logError("CoursePortalTabView#triggerAssignmentUpload", ex);
                Platform.runLater(() -> {
                    if (btn != null) btn.setDisable(false);
                    loadAssignmentsData();
                    showErrorDialog("Upload Error", "An unexpected error occurred: " + ex.getMessage());
                });
            }
        }).start();
    }

    private void showWarningDialog(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private double assignmentsColW(String h, int i, int total) {
        String l = h.toLowerCase();
        if (l.contains("#")) return 40;
        if (l.contains("course")) return 240;
        if (l.contains("title")) return 200;
        if (l.contains("start")) return 110;
        if (l.contains("deadline") || l.contains("due")) return 140;
        if (l.contains("submission")) return 140;
        if (l.contains("status")) return 130;
        if (l.contains("download") || l.contains("dowload")) return 100;
        if (l.contains("submit")) return 120;
        return 100;
    }

    // ==================== COURSE CONTENTS ====================
    private void loadCourseContentsData() {
        showLoading("Loading Course Contents...");
        new Thread(() -> {
            try {
                String html = context.portalRepository().fetchPageHtml("CoursePortalContentsSummary.aspx");
                if (html == null) {
                    Platform.runLater(() -> {
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(buildErrorState(
                            "Unable to load Course Contents",
                            "Failed to connect to the portal. Please check your internet connection and try again."
                        ));
                    });
                    return;
                }

                try {
                    java.nio.file.Files.writeString(java.nio.file.Paths.get("course_contents_raw.html"), html);
                } catch (IOException ex) {
                    ErrorReporter.logError("CoursePortalTabView#loadCourseContentsData write course_contents_raw.html", ex);
                }

                List<String[]> courses = context.portalRepository().parseDropdownOptions(html, "course");
                if (courses.isEmpty()) {
                    courses.addAll(context.portalRepository().parseDropdownOptions(html, "ddl"));
                }

                final String finalHtml = html;
                final List<String[]> finalCourses = courses;

                Platform.runLater(() -> {
                    try {
                        contentPane.getChildren().clear();
                        if (finalCourses.isEmpty()) {
                            contentPane.getChildren().add(buildErrorState(
                                "No Courses Found",
                                "No registered courses found in the Course Contents portal."
                            ));
                        } else {
                            contentPane.getChildren().add(buildCourseContentsSelectorView(finalCourses, finalHtml));
                        }
                    } catch (Exception ex) {
                        ErrorReporter.notify(context, "Failed to render Course Contents. Portal layout may have changed.", "CoursePortalTabView#loadCourseContentsData", ex);
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(buildErrorState(
                            "UI Render Error",
                            "Failed to render the subject selection list: " + ex.getMessage()
                        ));
                    }
                });
            } catch (Exception e) {
                ErrorReporter.notify(context, "Failed to load Course Contents. Please try again.", "CoursePortalTabView#loadCourseContentsData", e);
                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    contentPane.getChildren().add(buildErrorState(
                        "Loading Error",
                        "An unexpected error occurred while loading contents: " + e.getMessage()
                    ));
                });
            }
        }).start();
    }

    private ScrollPane buildCourseContentsSelectorView(List<String[]> courses, String rawHtml) {
        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");

        VBox content = new VBox(16);
        content.setPadding(new Insets(16, 28, 24, 28));
        content.setFillWidth(true);

        Label subHeading = new Label("Course Contents (Lecture Notes, Quiz/Assignment Solutions, Research Papers etc)");
        subHeading.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill: -color-text-main;-fx-padding:0 0 4 0;");
        content.getChildren().add(subHeading);

        VBox card = new VBox(0);
        card.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:8;-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:8;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.02),10,0,0,2);");

        HBox headerBox = new HBox(0);
        headerBox.setStyle("-fx-background-color: -color-bg-main;-fx-padding:12 0;-fx-border-color: -color-border;-fx-border-width:0 0 1 0;");

        Label hNum = new Label("#");
        hNum.setStyle("-fx-font-size:11px;-fx-font-weight:700;-fx-text-fill: -color-text-muted;-fx-padding:0 12;");
        hNum.setMinWidth(60); hNum.setMaxWidth(60);

        Label hTitle = new Label("Course Title");
        hTitle.setStyle("-fx-font-size:11px;-fx-font-weight:700;-fx-text-fill: -color-text-muted;-fx-padding:0 12;");
        hTitle.setMinWidth(450); hTitle.setMaxWidth(450);

        Label hAction = new Label("Action");
        hAction.setStyle("-fx-font-size:11px;-fx-font-weight:700;-fx-text-fill: -color-text-muted;-fx-padding:0 12;");
        hAction.setMinWidth(150); hAction.setMaxWidth(150);

        headerBox.getChildren().addAll(hNum, hTitle, hAction);
        card.getChildren().add(headerBox);

        for (int i = 0; i < courses.size(); i++) {
            String[] course = courses.get(i);
            String val = course[0];
            String title = course[1];

            HBox dataRow = new HBox(0);
            String bg = (i % 2 == 1) ? "-color-bg-card" : "-color-bg-main";
            dataRow.setStyle("-fx-background-color:" + bg + ";-fx-padding:12 0;-fx-border-color: -color-border;-fx-border-width:0 0 1 0;");
            dataRow.setAlignment(Pos.CENTER_LEFT);

            Label cNum = new Label(String.valueOf(i + 1));
            cNum.setStyle("-fx-font-size:12px;-fx-text-fill: -color-text-main;-fx-padding:0 12;");
            cNum.setMinWidth(60); cNum.setMaxWidth(60);

            Label cTitle = new Label(title);
            cTitle.setWrapText(true);
            cTitle.setStyle("-fx-font-size:12px;-fx-text-fill:#0066cc;-fx-underline:true;-fx-cursor:hand;-fx-padding:0 12;-fx-font-weight:bold;");
            cTitle.setMinWidth(450); cTitle.setMaxWidth(450);
            cTitle.setOnMouseClicked(e -> loadCourseContentsDetails(val, title, rawHtml));

            Button viewBtn = new Button("View Contents");
            viewBtn.setCursor(javafx.scene.Cursor.HAND);
            viewBtn.setStyle("-fx-background-color: -color-accent;-fx-text-fill: -color-bg-main;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:4;-fx-padding:4 12;");
            viewBtn.setOnAction(e -> loadCourseContentsDetails(val, title, rawHtml));

            HBox btnWrapper = new HBox(viewBtn);
            btnWrapper.setAlignment(Pos.CENTER_LEFT);
            btnWrapper.setPadding(new Insets(0, 12, 0, 12));
            btnWrapper.setMinWidth(150); btnWrapper.setMaxWidth(150);

            dataRow.getChildren().addAll(cNum, cTitle, btnWrapper);
            card.getChildren().add(dataRow);
        }

        content.getChildren().add(card);
        sp.setContent(content);
        return sp;
    }

    private void loadCourseContentsDetails(String courseId, String courseTitle, String pageHtml) {
        showLoading("Loading contents for " + courseTitle + "...");
        new Thread(() -> {
            try {
                String ddName = context.portalRepository().findDropdownName(pageHtml, "course");
                if (ddName == null) ddName = context.portalRepository().findDropdownName(pageHtml, "ddl");
                
                if (ddName == null) {
                    Platform.runLater(() -> {
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(buildErrorState(
                            "Parsing Error",
                            "Failed to identify the course selection dropdown."
                        ));
                    });
                    return;
                }

                String resultHtml = context.portalRepository().postbackWithDropdown("CoursePortalContentsSummary.aspx", ddName, courseId);
                if (resultHtml == null) {
                    Platform.runLater(() -> {
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(buildErrorState(
                            "Connection Error",
                            "Failed to fetch course details from the server."
                        ));
                    });
                    return;
                }

                try {
                    java.nio.file.Files.writeString(java.nio.file.Paths.get("course_contents_postback.html"), resultHtml);
                } catch (IOException ex) {
                    ErrorReporter.logError("CoursePortalTabView#loadCourseContentsDetails write course_contents_postback.html", ex);
                }

                final String finalResult = resultHtml;
                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    try {
                        contentPane.getChildren().add(buildCourseContentsDetailView(courseId, courseTitle, finalResult, pageHtml));
                    } catch (Exception ex) {
                        ErrorReporter.notify(context, "Failed to parse Course Contents. Portal layout may have changed.", "CoursePortalTabView#loadCourseContentsDetails", ex);
                        contentPane.getChildren().add(buildErrorState(
                            "Parsing Error",
                            "Failed to read the course content table cleanly."
                        ));
                    }
                });
            } catch (Exception e) {
                ErrorReporter.notify(context, "Failed to load Course Contents. Please try again.", "CoursePortalTabView#loadCourseContentsDetails", e);
                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    contentPane.getChildren().add(buildErrorState(
                        "Loading Error",
                        "An unexpected error occurred while fetching details: " + e.getMessage()
                    ));
                });
            }
        }).start();
    }

    private ScrollPane buildCourseContentsDetailView(String courseId, String courseTitle, String html, String originalPageHtml) {
        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");

        VBox content = new VBox(16);
        content.setPadding(new Insets(16, 28, 24, 28));
        content.setFillWidth(true);

        Button backBtn = new Button("← Back to Subjects");
        backBtn.setCursor(javafx.scene.Cursor.HAND);
        backBtn.setStyle("-fx-background-color: -color-bg-card;-fx-text-fill: -color-accent;-fx-border-color: -color-accent;-fx-border-width:1;-fx-border-radius:4;-fx-background-radius:4;-fx-font-size:12px;-fx-font-weight:bold;-fx-padding:6 12;");
        backBtn.setOnAction(e -> {
            activeTab = ""; // Force reload
            loadTab("portal_contents");
        });

        Document doc = Jsoup.parse(html);
        Element table = findMainGridTable(doc, "DataContent_gvPortalSummary");

        List<String[]> filesToDownload = new ArrayList<>();
        if (table != null) {
            Elements rows = table.select("tr");
            for (int r = 1; r < rows.size(); r++) {
                Element row = rows.get(r);
                Elements cells = row.select("td");
                if (cells.size() >= 3) {
                    String titleText = cells.get(1).text().trim().replaceAll("\\s+", " ");
                    String descText = cells.size() > 2 ? cells.get(2).text().trim().replaceAll("\\s+", " ") : "";
                    
                    for (Element cell : cells) {
                        Element aTag = cell.select("a").first();
                        if (aTag != null && !aTag.attr("href").isEmpty() && aTag.attr("href").toLowerCase().contains("download")) {
                            String href = aTag.attr("href");
                            filesToDownload.add(new String[]{href, titleText, descText});
                            break;
                        }
                    }
                }
            }
        }

        HBox topBar = new HBox(12);
        topBar.setStyle("-fx-padding:0 0 4 0;");
        topBar.getChildren().add(backBtn);

        if (!filesToDownload.isEmpty()) {
            Button downloadAllBtn = new Button("⬇ Download All (" + filesToDownload.size() + " files)");
            downloadAllBtn.setCursor(javafx.scene.Cursor.HAND);
            downloadAllBtn.setStyle("-fx-background-color: -color-accent;-fx-text-fill: -color-bg-main;-fx-font-size:12px;-fx-font-weight:bold;-fx-background-radius:4;-fx-padding:6 12;");
            downloadAllBtn.setOnAction(e -> triggerDownloadAll(filesToDownload, courseId, courseTitle, html, originalPageHtml));
            topBar.getChildren().add(downloadAllBtn);
        }
        content.getChildren().add(topBar);

        Label titleLabel = new Label(courseTitle + " - Course Contents");
        titleLabel.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill: -color-text-main;");
        content.getChildren().add(titleLabel);

        if (table != null) {
            String tableText = table.text().trim();
            if (tableText.contains("No Content Found Selected Course") || tableText.contains("No Content Found For Selected Course")) {
                renderEmptyContentsState(content);
            } else {
                Elements rows = table.select("tr");
                if (rows.size() > 1) {
                    VBox tableCard = buildNativeContentsTable(rows, courseTitle);
                    content.getChildren().add(tableCard);
                    content.setMinWidth(tableCard.getMinWidth() + 56);
                } else {
                    renderEmptyContentsState(content);
                }
            }
        } else {
            renderEmptyContentsState(content);
        }

        sp.setContent(content);
        return sp;
    }

    private void renderEmptyContentsState(VBox content) {
        VBox emptyCard = new VBox(12);
        emptyCard.setAlignment(Pos.CENTER);
        emptyCard.setPadding(new Insets(32, 24, 32, 24));
        emptyCard.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:8;-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:8;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.02),10,0,0,2);");

        Label icon = new Label("📂");
        icon.setStyle("-fx-font-size:28px;");

        Label label = new Label("No Course Contents Found");
        label.setStyle("-fx-text-fill: -color-text-muted;-fx-font-size:13px;-fx-font-weight:bold;");

        Label desc = new Label("There are currently no lecture notes, assignments, or study materials uploaded for this course.");
        desc.setStyle("-fx-text-fill: -color-text-muted;-fx-font-size:11px;-fx-text-alignment:center;");
        desc.setWrapText(true);

        emptyCard.getChildren().addAll(icon, label, desc);
        content.getChildren().add(emptyCard);
    }

    private VBox buildNativeContentsTable(Elements rows, String courseTitle) {
        VBox card = new VBox(0);
        card.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:8;-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:8;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.02),10,0,0,2);");
        
        Element headerRow = rows.first();
        if (headerRow == null) return card;

        Elements headerCells = headerRow.select("th, td");
        List<String> headers = new ArrayList<>();
        for (Element hc : headerCells) {
            headers.add(hc.text().trim());
        }

        double totalWidth = 0;
        for (int i = 0; i < headers.size(); i++) {
            totalWidth += contentsColW(headers.get(i), i, headers.size());
        }
        card.setMinWidth(totalWidth);

        HBox headerBox = new HBox(0);
        headerBox.setStyle("-fx-background-color: -color-bg-main;-fx-padding:12 0;-fx-border-color: -color-border;-fx-border-width:0 0 1 0;");
        for (int i = 0; i < headers.size(); i++) {
            Label hl = new Label(headers.get(i));
            hl.setStyle("-fx-font-size:11px;-fx-font-weight:700;-fx-text-fill: -color-text-muted;-fx-padding:0 12;");
            double w = contentsColW(headers.get(i), i, headers.size());
            hl.setMinWidth(w);
            hl.setMaxWidth(w);
            headerBox.getChildren().add(hl);
        }
        card.getChildren().add(headerBox);

        for (int r = 1; r < rows.size(); r++) {
            Element row = rows.get(r);
            Elements cells = row.select("td");
            if (cells.isEmpty()) continue;

            HBox dataRow = new HBox(0);
            String bg = (r % 2 == 0) ? "-color-bg-card" : "-color-bg-main";
            dataRow.setStyle("-fx-background-color:" + bg + ";-fx-padding:12 0;-fx-border-color: -color-border;-fx-border-width:0 0 1 0;");
            dataRow.setAlignment(Pos.CENTER_LEFT);

            String titleText = "";
            String descText = "";

            for (int c = 0; c < cells.size(); c++) {
                if (c >= headers.size()) break;

                Element cell = cells.get(c);
                String txt = cell.text().trim().replaceAll("\\s+", " ");
                double w = contentsColW(headers.get(c), c, headers.size());
                Node cellNode;

                if (c == 1) titleText = txt;
                if (c == 2) descText = txt;

                Element aTag = cell.select("a").first();
                if (aTag != null && !aTag.attr("href").isEmpty()) {
                    String linkText = aTag.text().trim();
                    String href = aTag.attr("href");

                    Button actBtn = new Button(linkText);
                    actBtn.setCursor(javafx.scene.Cursor.HAND);
                    actBtn.setStyle("-fx-background-color: -color-accent;-fx-text-fill: -color-bg-main;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:4;-fx-padding:4 12;");
                    
                    final String finalTitle = titleText;
                    final String finalDesc = descText;
                    actBtn.setOnAction(e -> triggerFileDownload(href, finalTitle, finalDesc, actBtn));

                    HBox btnWrapper = new HBox(actBtn);
                    btnWrapper.setAlignment(Pos.CENTER_LEFT);
                    btnWrapper.setPadding(new Insets(0, 12, 0, 12));
                    btnWrapper.setMinWidth(w);
                    btnWrapper.setMaxWidth(w);
                    cellNode = btnWrapper;
                } else {
                    Label cl = new Label(txt);
                    cl.setWrapText(true);
                    cl.setStyle("-fx-font-size:12px;-fx-text-fill: -color-text-main;-fx-padding:0 12;");
                    cl.setMinWidth(w);
                    cl.setMaxWidth(w);
                    cellNode = cl;
                }

                dataRow.getChildren().add(cellNode);
            }
            card.getChildren().add(dataRow);
        }

        return card;
    }

    private double contentsColW(String h, int i, int total) {
        String l = h.toLowerCase();
        if (l.contains("#")) return 50;
        if (l.contains("title")) return 200;
        if (l.contains("description")) return 320;
        if (l.contains("date")) return 130;
        if (l.contains("download")) return 110;
        return 120;
    }

    private String sanitizeFilename(String name) {
        if (name == null) return "";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    private String getCustomFilename(String docTitle, String docDesc, String extension) {
        String titlePart = sanitizeFilename(docTitle);
        String descPart = sanitizeFilename(docDesc);
        
        String filename = titlePart;
        if (!descPart.isEmpty()) {
            if (filename.isEmpty()) {
                filename = descPart;
            } else {
                filename = filename + " - " + descPart;
            }
        }
        
        if (filename.isEmpty()) {
            filename = "document_" + System.currentTimeMillis();
        }
        
        if (extension != null && !extension.isEmpty()) {
            if (!extension.startsWith(".")) {
                extension = "." + extension;
            }
            if (!filename.toLowerCase().endsWith(extension.toLowerCase())) {
                filename = filename + extension;
            }
        }
        return filename;
    }

    private Element findMainGridTable(Document doc, String preferredId) {
        if (preferredId != null && !preferredId.isEmpty()) {
            Element tbl = doc.getElementById(preferredId);
            if (tbl != null) return tbl;
        }
        for (Element tbl : doc.select("table.Grid")) {
            boolean isStudentDetails = tbl.parents().stream()
                .anyMatch(p -> p.hasClass("studentdetails") || p.id().equals("studentdetails") || p.tagName().equals("li"));
            if (isStudentDetails) continue;

            String text = tbl.text().toLowerCase();
            if (text.contains("name :") || text.contains("roll no :") || text.contains("father name :")) {
                continue;
            }
            return tbl;
        }
        return null;
    }

    private void triggerFileDownload(String href, String docTitle, String docDesc, Button btn) {
        if (btn != null) {
            Platform.runLater(() -> btn.setDisable(true));
        }

        new Thread(() -> {
            try {
                try (Response response = context.portalRepository().downloadFile(href)) {
                    if (!response.isSuccessful()) {
                        Platform.runLater(() -> {
                            if (btn != null) btn.setDisable(false);
                            showErrorDialog("Download Error", "Server returned HTTP " + response.code());
                        });
                        return;
                    }

                    ResponseBody body = response.body();
                    if (body == null) {
                        Platform.runLater(() -> {
                            if (btn != null) btn.setDisable(false);
                            showErrorDialog("Download Error", "Empty response body received.");
                        });
                        return;
                    }

                    String extension = ".pdf";
                    String disposition = response.header("Content-Disposition");
                    String serverFilename = "";
                    if (disposition != null && disposition.contains("filename=")) {
                        int index = disposition.indexOf("filename=");
                        String rawFilename = disposition.substring(index + 9).trim();
                        if (rawFilename.startsWith("\"") && rawFilename.endsWith("\"")) {
                            rawFilename = rawFilename.substring(1, rawFilename.length() - 1);
                        } else if (rawFilename.contains(";")) {
                            rawFilename = rawFilename.substring(0, rawFilename.indexOf(";")).trim();
                        }
                        serverFilename = rawFilename;
                    }

                    int dotIdx = serverFilename.lastIndexOf('.');
                    if (dotIdx > 0) {
                        extension = serverFilename.substring(dotIdx);
                    } else if (href.contains("fileType=q")) {
                        extension = ".pdf";
                    }

                    final String finalFilename = getCustomFilename(docTitle, docDesc, extension);

                    // Write to temporary file
                    File tempFile = File.createTempFile("assignly_download_", ".tmp");
                    tempFile.deleteOnExit();
                    try (InputStream is = body.byteStream();
                         OutputStream os = new FileOutputStream(tempFile)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = is.read(buffer)) != -1) {
                            os.write(buffer, 0, read);
                        }
                    }

                    final String finalExt = extension;
                    Platform.runLater(() -> {
                        FileChooser fileChooser = new FileChooser();
                        fileChooser.setTitle("Save Course Document");
                        fileChooser.setInitialFileName(finalFilename);

                        if (!finalExt.isEmpty()) {
                            fileChooser.getExtensionFilters().add(
                                new FileChooser.ExtensionFilter(finalExt.toUpperCase().substring(1) + " Files", "*" + finalExt)
                            );
                        }
                        fileChooser.getExtensionFilters().add(
                            new FileChooser.ExtensionFilter("All Files", "*.*")
                        );

                        File file = fileChooser.showSaveDialog(context.stage());

                        if (file != null) {
                            new Thread(() -> {
                                try {
                                    java.nio.file.Files.copy(tempFile.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                    Platform.runLater(() -> {
                                        if (btn != null) btn.setDisable(false);
                                        showSuccessDialog("Download Complete", "File saved: " + file.getName());
                                    });
                                } catch (Exception ex) {
                                    ErrorReporter.logError("CoursePortalTabView#triggerCourseDocumentDownload save file", ex);
                                    Platform.runLater(() -> {
                                        if (btn != null) btn.setDisable(false);
                                        showErrorDialog("Download Failed", "Error saving file: " + ex.getMessage());
                                    });
                                } finally {
                                    tempFile.delete();
                                }
                            }).start();
                        } else {
                            if (btn != null) btn.setDisable(false);
                            tempFile.delete();
                        }
                    });
                }
            } catch (Exception ex) {
                ErrorReporter.logError("CoursePortalTabView#triggerCourseDocumentDownload", ex);
                Platform.runLater(() -> {
                    if (btn != null) btn.setDisable(false);
                    showErrorDialog("Download Error", "Connection failed: " + ex.getMessage());
                });
            }
        }).start();
    }

    private void triggerDownloadAll(List<String[]> filesToDownload, String courseId, String courseTitle, String html, String originalPageHtml) {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Directory to Save All Files for " + courseTitle);
        File selectedDir = dirChooser.showDialog(context.stage());
        if (selectedDir == null) return;

        showLoading("Downloading all files to " + selectedDir.getName() + "...");

        new Thread(() -> {
            int successCount = 0;
            int total = filesToDownload.size();
            List<String> failedFiles = new ArrayList<>();

            for (int i = 0; i < total; i++) {
                String[] fileInfo = filesToDownload.get(i);
                String href = fileInfo[0];
                String docTitle = fileInfo[1];
                String docDesc = fileInfo[2];

                final int currentIdx = i + 1;
                Platform.runLater(() -> {
                    showLoading("Downloading file " + currentIdx + " of " + total + ": " + docTitle + "...");
                });

                try {
                    try (Response response = context.portalRepository().downloadFile(href)) {
                        if (!response.isSuccessful() || response.body() == null) {
                            failedFiles.add(docTitle + " (HTTP " + response.code() + ")");
                            continue;
                        }

                        String extension = ".pdf";
                        String disposition = response.header("Content-Disposition");
                        String serverFilename = "";
                        if (disposition != null && disposition.contains("filename=")) {
                            int idx = disposition.indexOf("filename=");
                            String rawFilename = disposition.substring(idx + 9).trim();
                            if (rawFilename.startsWith("\"") && rawFilename.endsWith("\"")) {
                                rawFilename = rawFilename.substring(1, rawFilename.length() - 1);
                            } else if (rawFilename.contains(";")) {
                                rawFilename = rawFilename.substring(0, rawFilename.indexOf(";")).trim();
                            }
                            serverFilename = rawFilename;
                        }

                        int dotIdx = serverFilename.lastIndexOf('.');
                        if (dotIdx > 0) {
                            extension = serverFilename.substring(dotIdx);
                        } else if (href.contains("fileType=q")) {
                            extension = ".pdf";
                        }

                        String finalFilename = getCustomFilename(docTitle, docDesc, extension);

                        File destFile = new File(selectedDir, finalFilename);
                        try (InputStream is = response.body().byteStream();
                             OutputStream os = new FileOutputStream(destFile)) {
                            byte[] buffer = new byte[8192];
                            int read;
                            while ((read = is.read(buffer)) != -1) {
                                os.write(buffer, 0, read);
                            }
                            successCount++;
                        }
                    }
                } catch (Exception ex) {
                    ErrorReporter.logError("CoursePortalTabView#triggerDownloadAll", ex);
                    failedFiles.add(docTitle + " (" + ex.getMessage() + ")");
                }
            }

            final int finalSuccess = successCount;
            final List<String> finalFailed = failedFiles;
            Platform.runLater(() -> {
                contentPane.getChildren().clear();
                try {
                    contentPane.getChildren().add(buildCourseContentsDetailView(courseId, courseTitle, html, originalPageHtml));
                } catch (Exception ex) {
                    ErrorReporter.logError("CoursePortalTabView#triggerDownloadAll refresh view", ex);
                    activeTab = "";
                    loadTab("portal_contents");
                }

                if (finalFailed.isEmpty()) {
                    showSuccessDialog("Download All Complete", "Successfully downloaded all " + finalSuccess + " files to " + selectedDir.getAbsolutePath());
                } else {
                    String msg = "Downloaded " + finalSuccess + " of " + total + " files.\nFailed files:\n" + String.join("\n", finalFailed);
                    showErrorDialog("Download All Completed with Errors", msg);
                }
            });
        }).start();
    }

    // ==================== PENDING ASSIGNMENTS ====================
    private void loadPendingAssignmentsData() {
        showLoading("Loading Pending Assignments...");
        new Thread(() -> {
            try {
                String html = context.portalRepository().fetchPageHtml("CoursePortalPendingAssignments.aspx");
                if (html == null) {
                    Platform.runLater(() -> {
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(buildErrorState(
                            "Unable to load Pending Assignments",
                            "Failed to connect to the portal. Please check your internet connection and try again."
                        ));
                    });
                    return;
                }

                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    try {
                        contentPane.getChildren().add(buildPendingAssignmentsView(html));
                    } catch (Exception ex) {
                        ErrorReporter.notify(context, "Failed to parse Pending Assignments. Portal layout may have changed.", "CoursePortalTabView#loadPendingAssignmentsData", ex);
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(buildErrorState(
                            "Parsing Error",
                            "Failed to read the Pending Assignments table cleanly."
                        ));
                    }
                });
            } catch (Exception e) {
                ErrorReporter.notify(context, "Failed to load Pending Assignments. Please try again.", "CoursePortalTabView#loadPendingAssignmentsData", e);
                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    contentPane.getChildren().add(buildErrorState(
                        "Loading Error",
                        "An unexpected error occurred while loading pending assignments: " + e.getMessage()
                    ));
                });
            }
        }).start();
    }

    private ScrollPane buildPendingAssignmentsView(String html) {
        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");

        VBox content = new VBox(16);
        content.setPadding(new Insets(16, 28, 24, 28));
        content.setFillWidth(true);

        Document doc = Jsoup.parse(html);
        
        String headingText = "Pending Assignments";
        Element h3 = doc.select("h3").first();
        if (h3 != null && !h3.text().trim().isEmpty()) {
            headingText = h3.text().trim();
        }

        Label subHeading = new Label(headingText);
        subHeading.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill: -color-text-main;-fx-padding:0 0 4 0;");
        content.getChildren().add(subHeading);

        Element table = findMainGridTable(doc, "DataContent_gvPortalSummary");

        if (table != null) {
            String tableText = table.text().trim();
            if (tableText.contains("No Assignment Found") || tableText.contains("No Pending Assignment")) {
                renderEmptyPendingState(content);
            } else {
                Elements rows = table.select("tr");
                if (rows.size() > 1) {
                    VBox tableCard = buildNativePendingTable(rows);
                    content.getChildren().add(tableCard);
                    content.setMinWidth(tableCard.getMinWidth() + 56);
                } else {
                    renderEmptyPendingState(content);
                }
            }
        } else {
            renderEmptyPendingState(content);
        }

        sp.setContent(content);
        return sp;
    }

    private void renderEmptyPendingState(VBox content) {
        VBox emptyCard = new VBox(12);
        emptyCard.setAlignment(Pos.CENTER);
        emptyCard.setPadding(new Insets(32, 24, 32, 24));
        emptyCard.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:8;-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:8;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.02),10,0,0,2);");

        Label icon = new Label("📌");
        icon.setStyle("-fx-font-size:28px;");

        Label label = new Label("No Pending Assignments");
        label.setStyle("-fx-text-fill: -color-text-muted;-fx-font-size:13px;-fx-font-weight:bold;");

        Label desc = new Label("There are currently no pending assignments requiring your attention. All assignments have been submitted or closed.");
        desc.setStyle("-fx-text-fill: -color-text-muted;-fx-font-size:11px;-fx-text-alignment:center;");
        desc.setWrapText(true);

        emptyCard.getChildren().addAll(icon, label, desc);
        content.getChildren().add(emptyCard);
    }

    private VBox buildNativePendingTable(Elements rows) {
        VBox card = new VBox(0);
        card.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:8;-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:8;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.02),10,0,0,2);");
        
        Element headerRow = rows.first();
        if (headerRow == null) return card;

        Elements headerCells = headerRow.select("th, td");
        List<String> headers = new ArrayList<>();
        for (Element hc : headerCells) {
            headers.add(hc.text().trim());
        }

        double totalWidth = 0;
        for (int i = 0; i < headers.size(); i++) {
            totalWidth += pendingColW(headers.get(i), i, headers.size());
        }
        card.setMinWidth(totalWidth);

        HBox headerBox = new HBox(0);
        headerBox.setStyle("-fx-background-color: -color-bg-main;-fx-padding:12 0;-fx-border-color: -color-border;-fx-border-width:0 0 1 0;");
        for (int i = 0; i < headers.size(); i++) {
            Label hl = new Label(headers.get(i));
            hl.setStyle("-fx-font-size:11px;-fx-font-weight:700;-fx-text-fill: -color-text-muted;-fx-padding:0 12;");
            double w = pendingColW(headers.get(i), i, headers.size());
            hl.setMinWidth(w);
            hl.setMaxWidth(w);
            headerBox.getChildren().add(hl);
        }
        card.getChildren().add(headerBox);

        for (int r = 1; r < rows.size(); r++) {
            Element row = rows.get(r);
            Elements cells = row.select("td");
            if (cells.isEmpty()) continue;

            HBox dataRow = new HBox(0);
            dataRow.setAlignment(Pos.CENTER_LEFT);
            String bg = (r % 2 == 0) ? "-color-bg-card" : "-color-bg-main";
            dataRow.setStyle("-fx-background-color:" + bg + ";-fx-padding:12 0;-fx-border-color: -color-border;-fx-border-width:0 0 1 0;");

            String courseTitle = "";
            String assignmentTitle = "";
            boolean isRowClosed = false;

            // Scan columns for status first to determine closed state
            for (int c = 0; c < cells.size(); c++) {
                if (c >= headers.size()) break;
                String colHeader = headers.get(c).toLowerCase();
                if (colHeader.contains("status")) {
                    String statusText = cells.get(c).text().toLowerCase();
                    if (statusText.contains("closed")) {
                        isRowClosed = true;
                    }
                }
            }

            for (int c = 0; c < cells.size(); c++) {
                if (c >= headers.size()) break;

                Element cell = cells.get(c);
                String txt = cell.text().trim().replaceAll("\\s+", " ");
                double w = pendingColW(headers.get(c), c, headers.size());
                Node cellNode;

                if (c == 1) courseTitle = txt;
                if (c == 2) assignmentTitle = txt;

                String colHeader = headers.get(c).toLowerCase();

                if (colHeader.contains("submission")) {
                    Label badge = new Label(txt);
                    boolean isSubmitted = txt.toLowerCase().contains("submitted") && !txt.toLowerCase().contains("not");
                    if (isSubmitted) {
                        badge.getStyleClass().add("badge-success");
                    } else {
                        badge.getStyleClass().add("badge-danger");
                    }
                    HBox wrapper = new HBox(badge);
                    wrapper.setAlignment(Pos.CENTER_LEFT);
                    wrapper.setPadding(new Insets(0, 12, 0, 12));
                    wrapper.setMinWidth(w);
                    wrapper.setMaxWidth(w);
                    cellNode = wrapper;
                } else if (colHeader.contains("status")) {
                    Label badge = new Label(txt);
                    if (isRowClosed) {
                        badge.getStyleClass().add("badge-muted");
                    } else {
                        badge.getStyleClass().add("badge-info");
                    }
                    HBox wrapper = new HBox(badge);
                    wrapper.setAlignment(Pos.CENTER_LEFT);
                    wrapper.setPadding(new Insets(0, 12, 0, 12));
                    wrapper.setMinWidth(w);
                    wrapper.setMaxWidth(w);
                    cellNode = wrapper;
                } else if (colHeader.contains("download") || colHeader.contains("dowload")) {
                    String href = context.portalRepository().extractAssignmentDownloadLink(cell);
                    Button dlBtn = new Button("Download");
                    dlBtn.setCursor(javafx.scene.Cursor.HAND);
                    dlBtn.setStyle("-fx-background-color: -color-accent;-fx-text-fill: -color-bg-main;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:4;-fx-padding:4 10;");
                    
                    final String finalCourse = courseTitle;
                    final String finalAssign = assignmentTitle;

                    if (href == null || href.isBlank()) {
                        dlBtn.setOnAction(e -> showWarningDialog("Download Assignment", "No assignment files have been uploaded by the teacher."));
                    } else {
                        dlBtn.setOnAction(e -> triggerAssignmentDownload(href, finalAssign, finalCourse, dlBtn));
                    }

                    HBox wrapper = new HBox(dlBtn);
                    wrapper.setAlignment(Pos.CENTER_LEFT);
                    wrapper.setPadding(new Insets(0, 12, 0, 12));
                    wrapper.setMinWidth(w);
                    wrapper.setMaxWidth(w);
                    cellNode = wrapper;
                } else if (colHeader.contains("submit")) {
                    if (!isRowClosed) {
                        String submitUrl = context.portalRepository().extractAssignmentSubmitLink(cell, "CoursePortalPendingAssignments.aspx");
                        if (!submitUrl.isEmpty()) {
                            String btnText = "Submit";
                            Element aOrInput = cell.select("a, button, input[type=submit], input[type=button]").first();
                            if (aOrInput != null) {
                                String val = aOrInput.attr("value").trim();
                                if (val.isEmpty()) val = aOrInput.text().trim();
                                if (!val.isEmpty()) btnText = val;
                            }
                            Button subBtn = new Button(btnText);
                            subBtn.setCursor(javafx.scene.Cursor.HAND);
                            subBtn.setStyle("-fx-background-color: -color-accent;-fx-text-fill:white;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:4;-fx-padding:4 10;");
                            subBtn.setOnAction(e -> triggerAssignmentUpload(submitUrl, subBtn));
                            uploadButtons.add(subBtn);
                            applyOfflineStateIfOffline(subBtn, btnText);

                            HBox wrapper = new HBox(subBtn);
                            wrapper.setAlignment(Pos.CENTER_LEFT);
                            wrapper.setPadding(new Insets(0, 12, 0, 12));
                            wrapper.setMinWidth(w);
                            wrapper.setMaxWidth(w);
                            cellNode = wrapper;
                        } else {
                            Label cl = new Label(txt.isEmpty() ? "-" : txt);
                            cl.setStyle("-fx-font-size:12px;-fx-text-fill: -color-text-muted;-fx-padding:0 12;");
                            cl.setMinWidth(w);
                            cl.setMaxWidth(w);
                            cellNode = cl;
                        }
                    } else {
                        Label cl = new Label(txt);
                        if (txt.toLowerCase().contains("closed")) {
                            cl.setStyle("-fx-font-size:11px;-fx-text-fill:#b91c1c;-fx-font-weight:bold;-fx-padding:0 12;");
                        } else {
                            cl.setStyle("-fx-font-size:12px;-fx-text-fill: -color-text-main;-fx-padding:0 12;");
                        }
                        cl.setMinWidth(w);
                        cl.setMaxWidth(w);
                        cellNode = cl;
                    }
                } else {
                    Label cl = new Label(txt);
                    cl.setWrapText(true);
                    cl.setStyle("-fx-font-size:12px;-fx-text-fill: -color-text-main;-fx-padding:0 12;");
                    cl.setMinWidth(w);
                    cl.setMaxWidth(w);
                    cellNode = cl;
                }

                dataRow.getChildren().add(cellNode);
            }
            card.getChildren().add(dataRow);
        }

        return card;
    }

    private double pendingColW(String h, int i, int total) {
        String l = h.toLowerCase();
        if (l.contains("#")) return 40;
        if (l.contains("course")) return 240;
        if (l.contains("title")) return 200;
        if (l.contains("start")) return 110;
        if (l.contains("deadline") || l.contains("due")) return 140;
        if (l.contains("submission")) return 140;
        if (l.contains("status")) return 130;
        if (l.contains("download") || l.contains("dowload")) return 100;
        if (l.contains("submit")) return 120;
        return 100;
    }

    private void showSuccessDialog(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showErrorDialog(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void applyOfflineStateIfOffline(Button btn, String originalText) {
        if (!context.isOnline()) {
            btn.setDisable(true);
            btn.setText("🔒 " + originalText);
            btn.setTooltip(new javafx.scene.control.Tooltip("This feature is disabled in offline mode."));
        }
    }

    private void onConnectivityChanged(boolean isOnline) {
        Platform.runLater(() -> {
            for (Button btn : uploadButtons) {
                String originalText = btn.getText();
                if (isOnline) {
                    btn.setDisable(false);
                    if (originalText.startsWith("🔒 ")) {
                        btn.setText(originalText.substring(2));
                    }
                    btn.setTooltip(null);
                } else {
                    btn.setDisable(true);
                    if (!originalText.startsWith("🔒 ")) {
                        btn.setText("🔒 " + originalText);
                    }
                    btn.setTooltip(new javafx.scene.control.Tooltip("This feature is disabled in offline mode."));
                }
            }
        });
    }

    public VBox getRoot() { return root; }
  }
