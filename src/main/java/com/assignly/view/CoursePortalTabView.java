package com.assignly.view;

import com.assignly.util.AppContext;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.ArrayList;
import java.util.List;

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

    public CoursePortalTabView(AppContext context) {
        this.context = context;
        buildShell();
        loadTab("portal_mcq");
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
        b.setCursor(javafx.scene.Cursor.HAND);
        b.setStyle(tabStyle(false));
        b.setOnAction(e -> loadTab(id));
        return b;
    }

    private String tabStyle(boolean on) {
        return on ? "-fx-background-color:#004643;-fx-text-fill:white;-fx-font-size:11px;-fx-font-weight:600;-fx-background-radius:6;-fx-padding:6 12;"
                  : "-fx-background-color:white;-fx-text-fill:#666;-fx-font-size:11px;-fx-font-weight:500;-fx-background-radius:6;-fx-padding:6 12;-fx-border-color:#d5d0ce;-fx-border-radius:6;-fx-border-width:1;";
    }

    private void loadTab(String tabKey) {
        if (activeTab.equals(tabKey)) return;
        activeTab = tabKey;

        for (var n : tabBar.getChildren()) {
            if (n instanceof Button b) {
                b.setStyle(tabStyle(tabKey.equals(b.getUserData())));
            }
        }

        contentPane.getChildren().clear();
        switch (tabKey) {
            case "portal_mcq" -> loadMcqData();
            case "portal_subjective" -> loadSubjectiveData();
            case "portal_contents" -> contentPane.getChildren().add(new WebPortalTabView(context, "CoursePortalContentsSummary.aspx", "Course Contents").getRoot());
            case "portal_assign_summ" -> contentPane.getChildren().add(new WebPortalTabView(context, "CoursePortal.aspx", "Assignments Summary").getRoot());
            case "portal_pending" -> contentPane.getChildren().add(new WebPortalTabView(context, "CoursePortalPendingAssignments.aspx", "Pending Assignments").getRoot());
        }
    }

    private void showLoading(String msg) {
        Platform.runLater(() -> {
            contentPane.getChildren().clear();
            VBox box = new VBox(10); box.setAlignment(Pos.CENTER);
            ProgressIndicator sp = new ProgressIndicator(); sp.setMaxSize(28,28);
            Label l = new Label(msg); l.setStyle("-fx-text-fill:#888;-fx-font-size:12px;");
            box.getChildren().addAll(sp, l);
            contentPane.getChildren().add(new StackPane(box));
        });
    }

    private VBox buildErrorState(String title, String message) {
        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40, 28, 40, 28));
        box.setStyle("-fx-background-color:#fff5f5;-fx-background-radius:12;-fx-border-color:#fee2e2;-fx-border-width:1;-fx-border-radius:12;-fx-max-width:550;");

        Label icon = new Label("⚠️");
        icon.setStyle("-fx-font-size:32px;");

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill:#991b1b;-fx-font-size:16px;-fx-font-weight:bold;");

        Label msgLbl = new Label(message);
        msgLbl.setStyle("-fx-text-fill:#b91c1c;-fx-font-size:12px;-fx-text-alignment:center;");
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
            } catch (Exception ignored) {}

            Platform.runLater(() -> {
                contentPane.getChildren().clear();
                try {
                    contentPane.getChildren().add(buildMcqView(html));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    contentPane.getChildren().clear();
                    contentPane.getChildren().add(buildErrorState(
                        "Parsing Error",
                        "Failed to read the MCQ table layout cleanly. The portal database may be undergoing updates."
                    ));
                }
            });
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
        subHeading.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#334155;-fx-padding:0 0 4 0;");
        content.getChildren().add(subHeading);

        Element table = doc.getElementById("DataContent_gvCTSdashboard");
        if (table == null) {
            table = doc.select("table.Grid").first();
        }

        if (table != null) {
            Elements rows = table.select("tr");
            if (rows.size() > 1) {
                VBox tableCard = buildNativeMcqTable(rows);
                content.getChildren().add(tableCard);
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
        empty.setStyle("-fx-text-fill:#64748b;-fx-font-size:12px;-fx-padding:10 0;");
        content.getChildren().add(empty);
    }

    private VBox buildNativeMcqTable(Elements rows) {
        VBox card = new VBox(0);
        card.setStyle("-fx-background-color:white;-fx-background-radius:8;-fx-border-color:#e2e8f0;-fx-border-width:1;-fx-border-radius:8;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.02),10,0,0,2);");
        
        Element headerRow = rows.first();
        if (headerRow == null) return card;

        Elements headerCells = headerRow.select("th, td");
        List<String> headers = new ArrayList<>();
        for (Element hc : headerCells) {
            headers.add(hc.text().trim());
        }

        HBox headerBox = new HBox(0);
        headerBox.setStyle("-fx-background-color:#f8fafc;-fx-padding:12 0;-fx-border-color:#e2e8f0;-fx-border-width:0 0 1 0;");
        for (int i = 0; i < headers.size(); i++) {
            Label hl = new Label(headers.get(i));
            hl.setStyle("-fx-font-size:11px;-fx-font-weight:700;-fx-text-fill:#475569;-fx-padding:0 12;");
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
            String bg = (r % 2 == 0) ? "#f8fafc" : "white";
            dataRow.setStyle("-fx-background-color:" + bg + ";-fx-padding:12 0;-fx-border-color:#e2e8f0;-fx-border-width:0 0 1 0;");

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
                    actBtn.setStyle("-fx-background-color:#004643;-fx-text-fill:white;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:4;-fx-padding:4 10;");
                    
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
                    cl.setStyle("-fx-font-size:12px;-fx-text-fill:#334155;-fx-padding:0 12;");
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
        if (l.contains("action")) return 110;
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
            } catch (Exception ignored) {}

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
                        ex.printStackTrace();
                        renderWebViewPaper(pageUrl, title);
                    }
                } else {
                    renderWebViewPaper(pageUrl, title);
                }
            });
        }).start();
    }

    private VBox buildNativeResultView(Document doc, String pageUrl) {
        VBox wrapper = new VBox(20);
        wrapper.setPadding(new Insets(16, 28, 24, 28));
        wrapper.setFillWidth(true);

        Button backBtn = new Button("← Back to MCQ Tests List");
        backBtn.setCursor(javafx.scene.Cursor.HAND);
        backBtn.setStyle("-fx-background-color:white;-fx-text-fill:#004643;-fx-border-color:#004643;-fx-border-width:1;-fx-border-radius:4;-fx-background-radius:4;-fx-font-size:12px;-fx-font-weight:bold;-fx-padding:6 12;");
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
        scorecard.setStyle("-fx-background-color:white;-fx-background-radius:12;-fx-border-color:#e2e8f0;-fx-border-width:1;-fx-border-radius:12;-fx-padding:24;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.03),15,0,0,3);");
        
        HBox scoreHeader = new HBox(12);
        scoreHeader.setAlignment(Pos.CENTER_LEFT);
        
        Label statusBadge = new Label("COMPLETED");
        statusBadge.setStyle("-fx-background-color:#def7ec;-fx-text-fill:#03543f;-fx-font-size:10px;-fx-font-weight:bold;-fx-padding:4 8;-fx-background-radius:4;");
        
        Label titleLabel = new Label("MCQ Test Scorecard");
        titleLabel.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
        
        scoreHeader.getChildren().addAll(titleLabel, statusBadge);
        scorecard.getChildren().add(scoreHeader);

        VBox detailBox = new VBox(0);
        detailBox.setStyle("-fx-border-color:#e2e8f0;-fx-border-width:1;-fx-border-radius:8;-fx-background-radius:8;-fx-background-color:#f8fafc;");
        
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
        bigScoreCircle.setStyle("-fx-background-color:#004643;-fx-background-radius:100;-fx-min-width:110;-fx-min-height:110;-fx-max-width:110;-fx-max-height:110;-fx-effect:dropshadow(three-pass-box,rgba(0,70,67,0.2),10,0,0,4);");
        
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
        } catch (Exception ignored) {}
        
        Label pctLbl = new Label(String.format("%.1f%% Score", pctVal));
        pctLbl.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#004643;");
        
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
        r.setStyle("-fx-padding:12 16;-fx-border-color:#e2e8f0;-fx-border-width:0 0 1 0;" + (bg ? "-fx-background-color:white;" : "-fx-background-color:#f8fafc;"));
        
        Label kl = new Label(key);
        kl.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#475569;");
        kl.setMinWidth(150);

        Label vl = new Label(val);
        vl.setStyle("-fx-font-size:12px;-fx-text-fill:#1e293b;-fx-font-weight:500;");
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
        backBtn.setStyle("-fx-background-color:white;-fx-text-fill:#004643;-fx-border-color:#004643;-fx-border-width:1;-fx-border-radius:4;-fx-background-radius:4;-fx-font-size:12px;-fx-font-weight:bold;-fx-padding:6 12;");
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
            } catch (Exception ignored) {}

            Platform.runLater(() -> {
                contentPane.getChildren().clear();
                try {
                    contentPane.getChildren().add(buildSubjectiveView(html));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    contentPane.getChildren().clear();
                    contentPane.getChildren().add(buildErrorState(
                        "Parsing Error", 
                        "Failed to read the Subjective Tests table. The portal layout might have changed."
                    ));
                }
            });
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
        subHeading.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#334155;-fx-padding:0 0 4 0;");
        content.getChildren().add(subHeading);

        Element table = doc.getElementById("DataContent_gvPortalSummary");
        if (table == null) {
            table = doc.select("table.Grid").first();
        }

        if (table != null) {
            String tableText = table.text().trim();
            if (tableText.contains("No Assignment Found") || tableText.contains("No Subjective Test")) {
                renderEmptySubjectiveState(content);
            } else {
                Elements rows = table.select("tr");
                if (rows.size() > 1) {
                    VBox tableCard = buildNativeSubjectiveTable(rows);
                    content.getChildren().add(tableCard);
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
        emptyCard.setStyle("-fx-background-color:white;-fx-background-radius:8;-fx-border-color:#e2e8f0;-fx-border-width:1;-fx-border-radius:8;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.02),10,0,0,2);");

        Label icon = new Label("📝");
        icon.setStyle("-fx-font-size:28px;");

        Label label = new Label("No Subjective Tests Found");
        label.setStyle("-fx-text-fill:#475569;-fx-font-size:13px;-fx-font-weight:bold;");

        Label desc = new Label("There are currently no active or completed subjective tests/assignments listed for your registered courses.");
        desc.setStyle("-fx-text-fill:#64748b;-fx-font-size:11px;-fx-text-alignment:center;");
        desc.setWrapText(true);

        emptyCard.getChildren().addAll(icon, label, desc);
        content.getChildren().add(emptyCard);
    }

    private VBox buildNativeSubjectiveTable(Elements rows) {
        VBox card = new VBox(0);
        card.setStyle("-fx-background-color:white;-fx-background-radius:8;-fx-border-color:#e2e8f0;-fx-border-width:1;-fx-border-radius:8;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.02),10,0,0,2);");
        
        Element headerRow = rows.first();
        if (headerRow == null) return card;

        Elements headerCells = headerRow.select("th, td");
        List<String> headers = new ArrayList<>();
        for (Element hc : headerCells) {
            headers.add(hc.text().trim());
        }

        HBox headerBox = new HBox(0);
        headerBox.setStyle("-fx-background-color:#f8fafc;-fx-padding:12 0;-fx-border-color:#e2e8f0;-fx-border-width:0 0 1 0;");
        for (int i = 0; i < headers.size(); i++) {
            Label hl = new Label(headers.get(i));
            hl.setStyle("-fx-font-size:11px;-fx-font-weight:700;-fx-text-fill:#475569;-fx-padding:0 12;");
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
            String bg = (r % 2 == 0) ? "#f8fafc" : "white";
            dataRow.setStyle("-fx-background-color:" + bg + ";-fx-padding:12 0;-fx-border-color:#e2e8f0;-fx-border-width:0 0 1 0;");

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
                    actBtn.setStyle("-fx-background-color:#004643;-fx-text-fill:white;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:4;-fx-padding:4 10;");
                    
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
                    cl.setStyle("-fx-font-size:12px;-fx-text-fill:#334155;-fx-padding:0 12;");
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

    private double subjectiveColW(String h, int i, int total) {
        String l = h.toLowerCase();
        if (l.contains("#")) return 50;
        if (l.contains("test") || l.contains("title") || l.contains("assignment")) return 200;
        if (l.contains("course")) return 280;
        if (l.contains("date") || l.contains("time") || l.contains("due")) return 180;
        if (l.contains("action") || l.contains("status")) return 110;
        return 120;
    }

    private void loadSubjectivePaperView(String relativeUrl, String title) {
        String urlTemp = relativeUrl;
        if (!urlTemp.startsWith("CTS/") && !urlTemp.startsWith("/CTS/")) {
            urlTemp = "CTS/" + urlTemp;
        }
        final String pageUrl = urlTemp;

        showLoading("Loading Subjective Test details...");

        new Thread(() -> {
            String html = context.portalRepository().fetchPageHtml(pageUrl);
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

            Platform.runLater(() -> renderWebViewPaper(pageUrl, title));
        }).start();
    }

    public VBox getRoot() { return root; }
}
