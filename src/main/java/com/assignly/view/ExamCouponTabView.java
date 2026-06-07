package com.assignly.view;

import com.assignly.util.AppContext;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import java.util.Base64;
import java.util.List;

/**
 * Exam Entry Coupon view – fetches EntryCouponSelect.aspx,
 * checks if the coupon is available, auto-follows the print link,
 * and renders a beautiful card-based dashboard with a preserved on-demand
 * document viewer matching the modern Assignly UI theme.
 */
public class ExamCouponTabView {

    private final VBox root = new VBox();
    private final AppContext context;
    private final WebView webView = new WebView();
    private StackPane contentPane;

    // Parsed Data Models
    private final List<ParsedExam> parsedExams = new ArrayList<>();
    private final List<String> parsedInstructions = new ArrayList<>();
    private ParsedStudent parsedStudent = null;
    private String currentHtmlContent = null;

    // Overlay components
    private StackPane overlayPane;
    private boolean isFullScreen = false;
    private static String cachedStudentRegNo = null;
    private static byte[] cachedStudentPhotoBytes = null;


    private static class ParsedExam {
        String subject = "";
        String teacher = "";
        String dateStr = "";
        String dayStr = "";
        String timeStr = "";
        String venue = "";
        String seatNo = "";
    }

    private static class ParsedStudent {
        String name = "";
        String regNo = "";
        String program = "";
        String campus = "";
        String session = "";
        String photoUrl = "";
    }

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
        ScrollPane sp = new ScrollPane(com.assignly.util.ShimmerBuilder.buildExamCouponShimmer());
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        contentPane.getChildren().add(sp);
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
                        VBox.setVgrow(container, Priority.ALWAYS);
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
                if (!isOffline) {
                    for (Element img : doc.select("img")) {
                        String srcAttr = img.attr("src");
                        String absUrl = img.attr("abs:src");
                        if (img.id().equals("stImg") || srcAttr.contains("PictureHandler.ashx")) {
                            if (!absUrl.isEmpty() && !absUrl.startsWith("data:")) {
                                byte[] imgBytes = null;
                                String currentReg = context.getSessionRegistration();
                                if (cachedStudentPhotoBytes != null && currentReg != null && currentReg.equals(cachedStudentRegNo)) {
                                    imgBytes = cachedStudentPhotoBytes;
                                } else {
                                    imgBytes = context.portalRepository().fetchPhotoBytes(absUrl, refererUrl);
                                    if (imgBytes != null && imgBytes.length > 0) {
                                        cachedStudentPhotoBytes = imgBytes;
                                        cachedStudentRegNo = currentReg;
                                    }
                                }

                                if (imgBytes != null && imgBytes.length > 0) {
                                    String base64 = java.util.Base64.getEncoder().encodeToString(imgBytes);
                                    img.attr("src", "data:image/jpeg;base64," + base64);
                                }
                            }
                        }
                    }
                }
                
                final String htmlWithBase = doc.outerHtml();
                final boolean finalOffline = isOffline;

                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    VBox container = new VBox();
                    VBox.setVgrow(container, Priority.ALWAYS);
                    if (finalOffline) container.getChildren().add(buildOfflineBanner());
                    
                    VBox dashboard = buildDashboardView(htmlWithBase);
                    VBox.setVgrow(dashboard, Priority.ALWAYS);
                    
                    container.getChildren().add(dashboard);
                    contentPane.getChildren().add(container);
                });
            } catch (Exception e) {
                e.printStackTrace();
                showError("Error", "An unexpected error occurred: " + e.getMessage());
            }
        }).start();
    }

    // ==================== Parser ====================

    private void parseCouponHtml(String html) {
        try {
            Document doc = Jsoup.parse(html);
            
            // 1. Student info
            ParsedStudent student = new ParsedStudent();
            Element nameEl = doc.getElementById("lblName");
            if (nameEl != null) {
                String fullName = nameEl.text().trim();
                if (fullName.contains(" S/O ")) {
                    student.name = fullName.split(" S/O ")[0].trim();
                } else if (fullName.contains(" D/O ")) {
                    student.name = fullName.split(" D/O ")[0].trim();
                } else {
                    student.name = fullName;
                }
            }
            
            Element sectionEl = doc.getElementById("lblSection");
            if (sectionEl != null) {
                String sect = sectionEl.text().trim();
                student.program = sect;
                if (sect.contains("-")) {
                    String[] parts = sect.split("-");
                    if (parts.length > 1) {
                        student.program = parts[1].trim();
                    }
                }
            }
            
            Element sessionEl = doc.getElementById("lblSessioName");
            if (sessionEl != null) {
                student.session = sessionEl.text().trim();
            }
            
            Element subtitleEl = doc.selectFirst(".MsoSubtitle");
            if (subtitleEl != null) {
                student.campus = subtitleEl.text().trim();
            } else {
                Element deptEl = doc.getElementById("lblDeptCheck");
                if (deptEl != null) {
                    student.campus = deptEl.text().trim();
                } else {
                    student.campus = "Abbottabad Campus";
                }
            }
            
            Element imgEl = doc.getElementById("stImg");
            if (imgEl != null) {
                student.photoUrl = imgEl.attr("src");
            }
            student.regNo = context.getSessionRegistration();
            
            this.parsedStudent = student;
            
            // 2. Exam Schedule
            this.parsedExams.clear();
            Element table = doc.getElementById("gvCourseSummary");
            if (table != null) {
                Elements rows = table.select("tr");
                for (Element row : rows) {
                    if (row.hasClass("GridItem") || row.hasClass("GridAlternatingItem")) {
                        Elements cells = row.select("td");
                        if (cells.size() >= 8) {
                            ParsedExam exam = new ParsedExam();
                            exam.subject = cells.get(1).text().trim();
                            exam.teacher = cells.get(2).text().trim();
                            exam.dateStr = cells.get(3).text().trim();
                            exam.dayStr = cells.get(4).text().trim();
                            exam.timeStr = cells.get(5).text().trim();
                            exam.venue = cells.get(6).text().trim();
                            exam.seatNo = cells.get(7).text().trim();
                            this.parsedExams.add(exam);
                        }
                    }
                }
            }
            
            // 3. Instructions
            this.parsedInstructions.clear();
            Elements ulList = doc.select("ul");
            if (!ulList.isEmpty()) {
                for (Element li : ulList.first().select("li")) {
                    this.parsedInstructions.add(li.text().trim());
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private LocalDate parseExamDate(String dateStr) {
        try {
            String[] parts = dateStr.trim().split("\\s+");
            if (parts.length == 3) {
                int day = Integer.parseInt(parts[0]);
                int month = getMonthNumber(parts[1]);
                int year = 2000 + Integer.parseInt(parts[2]);
                return LocalDate.of(year, month, day);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private int getMonthNumber(String monthStr) {
        String m = monthStr.toLowerCase();
        if (m.startsWith("jan")) return 1;
        if (m.startsWith("feb")) return 2;
        if (m.startsWith("mar")) return 3;
        if (m.startsWith("apr")) return 4;
        if (m.startsWith("may")) return 5;
        if (m.startsWith("jun")) return 6;
        if (m.startsWith("jul")) return 7;
        if (m.startsWith("aug")) return 8;
        if (m.startsWith("sep")) return 9;
        if (m.startsWith("oct")) return 10;
        if (m.startsWith("nov")) return 11;
        if (m.startsWith("dec")) return 12;
        return 1;
    }

    // ==================== Dashboard UI ====================

    private VBox buildDashboardView(String htmlContent) {
        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setStyle("-fx-background-color: -color-bg-main;");
        VBox.setVgrow(mainContainer, Priority.ALWAYS);

        // Parse HTML data
        parseCouponHtml(htmlContent);

        // Header Title
        HBox headerBar = new HBox();
        headerBar.setAlignment(Pos.CENTER_LEFT);
        Label mainTitle = new Label("Examination Dashboard");
        mainTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: -color-text-main;");
        headerBar.getChildren().add(mainTitle);
        mainContainer.getChildren().add(headerBar);

        // 1. Top Summary metrics bar
        HBox summaryCards = buildSummaryCardsBar();
        mainContainer.getChildren().add(summaryCards);

        // 2. Next Exam highlight block
        VBox nextExamHighlight = buildNextExamHighlight();
        if (nextExamHighlight != null) {
            mainContainer.getChildren().add(nextExamHighlight);
        }

        // 3. Body Split
        HBox body = new HBox(20);
        VBox.setVgrow(body, Priority.ALWAYS);

        // Left Column (Schedule Cards Grid)
        VBox leftCol = new VBox(15);
        HBox.setHgrow(leftCol, Priority.ALWAYS);
        
        Label scheduleTitle = new Label("Exam Dates & Schedules");
        scheduleTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");
        leftCol.getChildren().add(scheduleTitle);

        ScrollPane scheduleScroll = new ScrollPane();
        scheduleScroll.setFitToWidth(true);
        scheduleScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scheduleScroll, Priority.ALWAYS);

        VBox cardsContainer = new VBox(12);
        for (ParsedExam exam : parsedExams) {
            cardsContainer.getChildren().add(buildExamCard(exam));
        }
        scheduleScroll.setContent(cardsContainer);
        leftCol.getChildren().add(scheduleScroll);

        // Right Column (Student profile & instructions)
        VBox rightCol = new VBox(20);
        rightCol.setPrefWidth(300);
        rightCol.setMinWidth(280);
        rightCol.setMaxWidth(350);

        VBox studentPanel = buildStudentPanel();
        VBox instructionsPanel = buildInstructionsPanel();
        rightCol.getChildren().addAll(studentPanel, instructionsPanel);

        body.getChildren().addAll(leftCol, rightCol);
        mainContainer.getChildren().add(body);

        // ScrollPane around the entire container to support smaller height environments
        ScrollPane rootScroll = new ScrollPane(mainContainer);
        rootScroll.setFitToWidth(true);
        rootScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        VBox wrapper = new VBox(rootScroll);
        VBox.setVgrow(wrapper, Priority.ALWAYS);

        // Responsive layout listeners
        body.widthProperty().addListener((obs, oldVal, newVal) -> {
            boolean isNarrow = newVal.doubleValue() < 750;
            boolean isSideBySide = body.getChildren().contains(rightCol);
            if (isNarrow && isSideBySide) {
                Platform.runLater(() -> {
                    if (body.getChildren().contains(rightCol)) {
                        body.getChildren().remove(rightCol);
                        if (!mainContainer.getChildren().contains(rightCol)) {
                            mainContainer.getChildren().add(rightCol);
                        }
                        rightCol.setMaxWidth(Double.MAX_VALUE);
                    }
                });
            } else if (!isNarrow && !isSideBySide) {
                Platform.runLater(() -> {
                    if (mainContainer.getChildren().contains(rightCol)) {
                        mainContainer.getChildren().remove(rightCol);
                        if (!body.getChildren().contains(rightCol)) {
                            body.getChildren().add(rightCol);
                        }
                        rightCol.setMaxWidth(350);
                    }
                });
            }
        });

        this.currentHtmlContent = htmlContent;

        // Initialize WebView asynchronously after the UI rendering to prevent tab-switching lag
        Platform.runLater(() -> {
            try {
                webView.getEngine().setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                    if (newState == Worker.State.SUCCEEDED) {
                        webView.getEngine().executeScript(
                                "var nav = document.getElementById('navigation'); if(nav) nav.style.display = 'none';" +
                                "var header = document.getElementById('header'); if(header) header.style.display = 'none';"
                        );
                    }
                });
                webView.getEngine().loadContent(htmlContent);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        return wrapper;
    }

    private HBox buildSummaryCardsBar() {
        HBox bar = new HBox(15);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setFillHeight(true);

        VBox card1 = createStatCard("Total Exams", String.valueOf(parsedExams.size()), "📝", null);

        LocalDate minDate = null;
        LocalDate maxDate = null;
        for (ParsedExam exam : parsedExams) {
            LocalDate d = parseExamDate(exam.dateStr);
            if (d != null) {
                if (minDate == null || d.isBefore(minDate)) minDate = d;
                if (maxDate == null || d.isAfter(maxDate)) maxDate = d;
            }
        }

        String firstDateStr = minDate != null ? formatDate(minDate) : "N/A";
        String firstCountdown = minDate != null ? getCountdownString(minDate) : "";
        String lastDateStr = maxDate != null ? formatDate(maxDate) : "N/A";

        VBox card2 = createStatCard("First Exam", firstDateStr, "📅", firstCountdown);
        VBox card3 = createStatCard("Last Exam", lastDateStr, "🏁", null);

        String period = "N/A";
        if (parsedStudent != null && parsedStudent.session != null) {
            period = parsedStudent.session;
        }
        VBox card4 = createStatCard("Exam Period", period, "⏰", null);

        boolean allExamsOver = false;
        if (!parsedExams.isEmpty()) {
            LocalDateTime lastExamDateTime = null;
            for (ParsedExam exam : parsedExams) {
                LocalDateTime examDt = getExamDateTime(exam);
                if (examDt != null) {
                    if (lastExamDateTime == null || examDt.isAfter(lastExamDateTime)) {
                        lastExamDateTime = examDt;
                    }
                }
            }
            if (lastExamDateTime != null) {
                if (LocalDateTime.now().isAfter(lastExamDateTime.plusHours(2))) {
                    allExamsOver = true;
                }
            }
        }

        VBox card5;
        if (allExamsOver) {
            card5 = createStatCard("Exam Status", "Exam Over", "🎉", null);
        } else {
            card5 = createStatCard("Eligibility Status", "Eligible", "✅", null);
        }
        card5.setStyle(card5.getStyle() + ";-fx-border-color: #10B981;-fx-border-width:1;");

        HBox.setHgrow(card1, Priority.ALWAYS);
        HBox.setHgrow(card2, Priority.ALWAYS);
        HBox.setHgrow(card3, Priority.ALWAYS);
        HBox.setHgrow(card4, Priority.ALWAYS);
        HBox.setHgrow(card5, Priority.ALWAYS);

        bar.getChildren().addAll(card1, card2, card3, card4, card5);
        return bar;
    }

    private String formatDate(LocalDate date) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        return date.format(dtf);
    }

    private String getCountdownString(LocalDate examDate) {
        if (examDate == null) return "";
        LocalDate today = LocalDate.now();
        long days = java.time.temporal.ChronoUnit.DAYS.between(today, examDate);
        if (days == 0) {
            return "Starts today!";
        } else if (days == 1) {
            return "Starts tomorrow!";
        } else if (days > 1) {
            return "Starts in " + days + " Days";
        } else if (days == -1) {
            return "Took place yesterday";
        } else {
            return "Took place " + Math.abs(days) + " Days ago";
        }
    }

    private String getCountdownString(LocalDateTime examDateTime) {
        if (examDateTime == null) return "";
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(examDateTime) && now.isBefore(examDateTime.plusHours(3))) {
            return "Ongoing / In Progress";
        }
        
        LocalDate today = now.toLocalDate();
        LocalDate examDate = examDateTime.toLocalDate();
        long days = java.time.temporal.ChronoUnit.DAYS.between(today, examDate);
        if (days == 0) {
            if (now.isBefore(examDateTime)) {
                long hours = java.time.temporal.ChronoUnit.HOURS.between(now, examDateTime);
                if (hours > 0) {
                    return "Starts in " + hours + (hours == 1 ? " Hour" : " Hours");
                } else {
                    long minutes = java.time.temporal.ChronoUnit.MINUTES.between(now, examDateTime);
                    return "Starts in " + minutes + (minutes == 1 ? " Minute" : " Minutes");
                }
            } else {
                return "Starts today!";
            }
        } else if (days == 1) {
            return "Starts tomorrow!";
        } else if (days > 1) {
            return "Starts in " + days + " Days";
        } else if (days == -1) {
            return "Took place yesterday";
        } else {
            return "Took place " + Math.abs(days) + " Days ago";
        }
    }


    private VBox createStatCard(String label, String value, String emoji, String subText) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:8;"
                + "-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:8;"
                + "-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.02),10,0,0,2);");
        
        Label lbl = new Label(label.toUpperCase());
        lbl.setStyle("-fx-font-size: 9px; -fx-text-fill: -color-text-muted; -fx-font-weight: 800; -fx-letter-spacing: 0.5px;");
        
        HBox valueRow = new HBox(8);
        valueRow.setAlignment(Pos.CENTER_LEFT);
        
        Label emojiLabel = new Label(emoji);
        emojiLabel.setStyle("-fx-font-size: 16px;");
        
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");
        val.setWrapText(true);
        
        valueRow.getChildren().addAll(emojiLabel, val);
        card.getChildren().addAll(lbl, valueRow);

        if (subText != null && !subText.isEmpty()) {
            Label sub = new Label(subText);
            sub.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-accent; -fx-font-weight: bold;");
            card.getChildren().add(sub);
        }

        // Hover animations
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:8;"
                + "-fx-border-color: -color-accent;-fx-border-width:1;-fx-border-radius:8;"
                + "-fx-effect:dropshadow(three-pass-box,rgba(20,184,166,0.1),10,0,0,2);"
                + "-fx-translate-y: -2;"));
        card.setOnMouseExited(e -> {
            String borderStyle = label.equals("Eligibility Status") ? "-fx-border-color: #10B981;" : "-fx-border-color: -color-border;";
            card.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:8;"
                + borderStyle + "-fx-border-width:1;-fx-border-radius:8;"
                + "-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.02),10,0,0,2);"
                + "-fx-translate-y: 0;");
        });

        return card;
    }

    // ==================== Next Exam Highlight ====================

    private VBox buildNextExamHighlight() {
        ParsedExam nextExam = getNextUpcomingExam();
        if (nextExam == null) return null;

        LocalDateTime examDt = getExamDateTime(nextExam);
        String countdown = examDt != null ? getCountdownString(examDt) : "";


        VBox highlight = new VBox(10);
        highlight.setPadding(new Insets(16, 20, 16, 20));
        highlight.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:10;"
                + "-fx-border-color: -color-accent;-fx-border-width:1;-fx-border-radius:10;"
                + "-fx-effect: dropshadow(three-pass-box, rgba(20, 184, 166, 0.15), 15, 0, 0, 0);"); // cyan accent glow

        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER_LEFT);
        
        Label nextLabel = new Label("✨ UPCOMING HIGHLIGHT");
        nextLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: -color-accent; -fx-letter-spacing: 0.5px;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label countdownBadge = new Label(countdown);
        countdownBadge.setStyle("-fx-background-color: rgba(20, 184, 166, 0.1); -fx-text-fill: -color-accent;"
                + "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 20;");
        
        headerRow.getChildren().addAll(nextLabel, spacer, countdownBadge);

        // Details Grid
        GridPane grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(8);

        Label subjectLabel = new Label(nextExam.subject);
        subjectLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: -color-text-main;");
        GridPane.setColumnSpan(subjectLabel, 2);
        grid.add(subjectLabel, 0, 0);

        HBox dateBox = new HBox(6);
        dateBox.setAlignment(Pos.CENTER_LEFT);
        Label dateIcon = new Label("📅");
        Label dateLabel = new Label(nextExam.dateStr + " (" + nextExam.dayStr + ")");
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-main;");
        dateBox.getChildren().addAll(dateIcon, dateLabel);
        grid.add(dateBox, 0, 1);

        HBox timeBox = new HBox(6);
        timeBox.setAlignment(Pos.CENTER_LEFT);
        Label timeIcon = new Label("🕒");
        Label timeLabel = new Label(nextExam.timeStr);
        timeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-main;");
        timeBox.getChildren().addAll(timeIcon, timeLabel);
        grid.add(timeBox, 1, 1);

        HBox venueBox = new HBox(6);
        venueBox.setAlignment(Pos.CENTER_LEFT);
        Label venueIcon = new Label("📍");
        Label venueLabel = new Label("Venue: " + nextExam.venue);
        venueLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-main;");
        venueBox.getChildren().addAll(venueIcon, venueLabel);
        grid.add(venueBox, 0, 2);

        HBox seatBox = new HBox(6);
        seatBox.setAlignment(Pos.CENTER_LEFT);
        Label seatIcon = new Label("🪑");
        Label seatLabel = new Label("Seat: " + nextExam.seatNo);
        seatLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-main;");
        seatBox.getChildren().addAll(seatIcon, seatLabel);
        grid.add(seatBox, 1, 2);

        highlight.getChildren().addAll(headerRow, grid);
        return highlight;
    }

    private ParsedExam getNextUpcomingExam() {
        LocalDateTime now = LocalDateTime.now();
        ParsedExam nextExam = null;
        LocalDateTime nextDateTime = null;

        for (ParsedExam exam : parsedExams) {
            LocalDateTime examDt = getExamDateTime(exam);
            // An exam is considered upcoming or ongoing if it starts in the future, or started less than 3 hours ago
            if (examDt != null && !examDt.plusHours(3).isBefore(now)) {
                if (nextDateTime == null || examDt.isBefore(nextDateTime)) {
                    nextDateTime = examDt;
                    nextExam = exam;
                }
            }
        }
        return nextExam;
    }

    private LocalDateTime getExamDateTime(ParsedExam exam) {
        if (exam == null) return null;
        LocalDate date = parseExamDate(exam.dateStr);
        if (date == null) return null;
        LocalTime time = parseTime(exam.timeStr);
        if (time == null) {
            // Default to end of day if time format is unparseable, to avoid hiding it prematurely
            time = LocalTime.MAX;
        }
        return LocalDateTime.of(date, time);
    }

    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return null;
        String cleaned = timeStr.trim().toUpperCase().replaceAll("\\s+", "");
        
        // Try 12-hour format with AM/PM
        try {
            String amPmCleaned = cleaned;
            if (amPmCleaned.endsWith("AM") && !amPmCleaned.contains(" AM")) {
                amPmCleaned = amPmCleaned.replace("AM", " AM");
            }
            if (amPmCleaned.endsWith("PM") && !amPmCleaned.contains(" PM")) {
                amPmCleaned = amPmCleaned.replace("PM", " PM");
            }
            if (amPmCleaned.indexOf(":") == 1) {
                amPmCleaned = "0" + amPmCleaned;
            }
            return LocalTime.parse(amPmCleaned, DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.US));
        } catch (Exception ignored) {}
        
        // Try 24-hour format (e.g. "09:00", "13:30")
        try {
            return LocalTime.parse(cleaned, DateTimeFormatter.ofPattern("H:mm"));
        } catch (Exception ignored) {}
        
        try {
            return LocalTime.parse(cleaned, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception ignored) {}
        
        return null;
    }


    // ==================== Exam Card ====================

    private VBox buildExamCard(ParsedExam exam) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(10, 14, 10, 14)); // 40% height reduction

        ParsedExam nextUpcoming = getNextUpcomingExam();
        boolean isNext = nextUpcoming != null && nextUpcoming.subject.equals(exam.subject);

        String borderStyle;
        if (isNext) {
            borderStyle = "-fx-border-color: -color-border -color-border -color-border -color-accent;-fx-border-width:1 1 1 4;-fx-border-radius:6;";
        } else {
            borderStyle = "-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:6;";
        }

        card.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:6;"
                + borderStyle
                + "-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.01),8,0,0,1);");

        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label subject = new Label(exam.subject);
        subject.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");
        subject.setWrapText(true);

        Label teacher = new Label(exam.teacher);
        teacher.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-muted;");
        titleBox.getChildren().addAll(subject, teacher);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button viewDetailsBtn = new Button("View Details");
        viewDetailsBtn.setCursor(javafx.scene.Cursor.HAND);
        viewDetailsBtn.setStyle("-fx-background-color: -color-accent; -fx-text-fill: white;"
                + "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 4;");
        viewDetailsBtn.setOnAction(e -> showDetailsModal(exam));

        topRow.getChildren().addAll(titleBox, spacer, viewDetailsBtn);

        // Compact details row
        HBox detailsRow = new HBox(16);
        detailsRow.setAlignment(Pos.CENTER_LEFT);

        HBox dateBox = new HBox(4);
        dateBox.setAlignment(Pos.CENTER_LEFT);
        Label dateIcon = new Label("📅");
        dateIcon.setStyle("-fx-font-size: 11px;");
        Label dateLabel = new Label(exam.dateStr + " (" + exam.dayStr + ")");
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-main;");
        dateBox.getChildren().addAll(dateIcon, dateLabel);

        HBox timeBox = new HBox(4);
        timeBox.setAlignment(Pos.CENTER_LEFT);
        Label timeIcon = new Label("🕒");
        timeIcon.setStyle("-fx-font-size: 11px;");
        Label timeLabel = new Label(exam.timeStr);
        timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-main;");
        timeBox.getChildren().addAll(timeIcon, timeLabel);

        Label venueBadge = new Label("📍 " + exam.venue);
        venueBadge.setStyle("-fx-background-color: rgba(20, 184, 166, 0.08); -fx-text-fill: -color-accent;"
                + "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-background-radius: 4;");

        Label seatBadge = new Label("🪑 Seat " + exam.seatNo);
        seatBadge.setStyle("-fx-background-color: rgba(99, 102, 241, 0.08); -fx-text-fill: #6366F1;"
                + "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-background-radius: 4;");

        detailsRow.getChildren().addAll(dateBox, timeBox, venueBadge, seatBadge);

        card.getChildren().addAll(topRow, detailsRow);

        card.setOnMouseEntered(e -> {
            String effectStyle = isNext
                    ? "-fx-effect:dropshadow(three-pass-box,rgba(20,184,166,0.18),12,0,0,2);-fx-translate-y:-1;"
                    : "-fx-effect:dropshadow(three-pass-box,rgba(20,184,166,0.1),10,0,0,1);-fx-border-color: -color-accent;-fx-translate-y:-1;";
            card.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:6;"
                    + borderStyle + effectStyle);
        });
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:6;"
                    + borderStyle + "-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.01),8,0,0,1);-fx-translate-y:0;");
        });

        return card;
    }

    private void copyToClipboard(ParsedExam exam) {
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        String text = String.format("Course: %s\nDate: %s (%s)\nTime: %s\nVenue: %s\nSeat Number: %s",
                exam.subject, exam.dateStr, exam.dayStr, exam.timeStr, exam.venue, exam.seatNo);
        content.putString(text);
        clipboard.setContent(content);
        showInfoAlert("Copied", "Exam details copied to clipboard!");
    }

    // ==================== Details Modal ====================

    private void showDetailsModal(ParsedExam exam) {
        StackPane modalOverlay = new StackPane();
        modalOverlay.setStyle("-fx-background-color: rgba(15, 23, 42, 0.8);");

        VBox box = new VBox(15);
        box.setPadding(new Insets(20));
        box.setMaxSize(400, 320);
        box.setStyle("-fx-background-color: -color-bg-card; -fx-background-radius: 10;"
                + "-fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 10;"
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 20, 0, 0, 8);");

        HBox titleRow = new HBox();
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Exam Details");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -color-text-muted; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> contentPane.getChildren().remove(modalOverlay));
        titleRow.getChildren().addAll(title, spacer, closeBtn);

        VBox content = new VBox(10);
        
        Label subLabel = new Label(exam.subject);
        subLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: -color-accent;");
        
        Label insLabel = new Label("Instructor: " + exam.teacher);
        insLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");

        Separator sep = new Separator();
        sep.setStyle("-fx-opacity: 0.15;");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(10);

        grid.add(createDetailField("Date & Day", exam.dateStr + " (" + exam.dayStr + ")"), 0, 0);
        grid.add(createDetailField("Time Slot", exam.timeStr), 1, 0);
        grid.add(createDetailField("Examination Venue", exam.venue), 0, 1);
        grid.add(createDetailField("Seat Number", exam.seatNo), 1, 1);

        Separator sep2 = new Separator();
        sep2.setStyle("-fx-opacity: 0.15;");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button copyBtn = new Button("📋 Copy Info");
        copyBtn.setCursor(javafx.scene.Cursor.HAND);
        copyBtn.setStyle("-fx-background-color: -color-accent; -fx-text-fill: white;"
                + "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 4;");
        copyBtn.setOnAction(e -> {
            copyToClipboard(exam);
            contentPane.getChildren().remove(modalOverlay);
        });

        Button doneBtn = new Button("Done");
        doneBtn.setCursor(javafx.scene.Cursor.HAND);
        doneBtn.setStyle("-fx-background-color: -color-bg-elevated; -fx-text-fill: -color-text-main;"
                + "-fx-border-color: -color-border; -fx-border-width: 1; -fx-font-size: 11px; -fx-padding: 5 11; -fx-background-radius: 4;");
        doneBtn.setOnAction(e -> contentPane.getChildren().remove(modalOverlay));

        actions.getChildren().addAll(copyBtn, doneBtn);

        content.getChildren().addAll(subLabel, insLabel, sep, grid, sep2, actions);
        box.getChildren().addAll(titleRow, content);

        modalOverlay.getChildren().add(box);
        StackPane.setAlignment(box, Pos.CENTER);

        contentPane.getChildren().add(modalOverlay);

        modalOverlay.setOpacity(0);
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(150), modalOverlay);
        ft.setToValue(1.0);
        ft.play();
    }

    private VBox createDetailField(String header, String val) {
        VBox v = new VBox(2);
        Label h = new Label(header.toUpperCase());
        h.setStyle("-fx-font-size: 9px; -fx-text-fill: -color-text-muted; -fx-font-weight: 800;");
        Label value = new Label(val);
        value.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-main; -fx-font-weight: bold;");
        v.getChildren().addAll(h, value);
        return v;
    }

    // ==================== Student Panel ====================

    private VBox buildStudentPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(16));
        panel.setStyle("-fx-background-color: -color-bg-card; -fx-background-radius: 8;"
                + "-fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 8;"
                + "-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.02),10,0,0,2);");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.Node photoNode = getStudentPhotoNode();
        
        VBox nameBox = new VBox(4);
        Label nameLabel = new Label(parsedStudent != null ? parsedStudent.name : "Student Name");
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");
        nameLabel.setWrapText(true);
        
        Label regLabel = new Label(parsedStudent != null ? parsedStudent.regNo : "Registration Number");
        regLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-muted;");

        nameBox.getChildren().addAll(nameLabel, regLabel);
        header.getChildren().addAll(photoNode, nameBox);

        Separator sep = new Separator();
        sep.setStyle("-fx-opacity: 0.15;");

        VBox details = new VBox(8);
        details.getChildren().add(createDetailRow("Program", parsedStudent != null ? parsedStudent.program : "N/A"));
        details.getChildren().add(createDetailRow("Campus", parsedStudent != null ? parsedStudent.campus : "N/A"));
        details.getChildren().add(createDetailRow("Session", parsedStudent != null ? parsedStudent.session : "N/A"));

        Separator sep2 = new Separator();
        sep2.setStyle("-fx-opacity: 0.15;");

        VBox actions = new VBox(10);
        
        Button downloadBtn = new Button("Download Official Coupon");
        downloadBtn.setMaxWidth(Double.MAX_VALUE);
        downloadBtn.setCursor(javafx.scene.Cursor.HAND);
        downloadBtn.setStyle("-fx-background-color: -color-accent; -fx-text-fill: white;"
                + "-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6;");
        downloadBtn.setOnAction(e -> {
            downloadBtn.setDisable(true);
            downloadBtn.setText("Generating PDF...");
            saveAsPdf(() -> {
                downloadBtn.setDisable(false);
                downloadBtn.setText("Download Official Coupon");
            });
        });

        Button viewCouponBtn = new Button("Preview Coupon");
        viewCouponBtn.setMaxWidth(Double.MAX_VALUE);
        viewCouponBtn.setCursor(javafx.scene.Cursor.HAND);
        viewCouponBtn.setStyle("-fx-background-color: -color-bg-elevated; -fx-text-fill: -color-text-main;"
                + "-fx-border-color: -color-border; -fx-border-width: 1; -fx-font-size: 12px; -fx-padding: 8 16; -fx-background-radius: 6;");
        viewCouponBtn.setOnAction(e -> showCouponOverlay());

        actions.getChildren().addAll(downloadBtn, viewCouponBtn);

        panel.getChildren().addAll(header, sep, details, sep2, actions);
        return panel;
    }

    private javafx.scene.Node getStudentPhotoNode() {
        try {
            if (parsedStudent != null && parsedStudent.photoUrl != null && parsedStudent.photoUrl.startsWith("data:")) {
                String base64Data = parsedStudent.photoUrl.substring(parsedStudent.photoUrl.indexOf(",") + 1);
                byte[] bytes = Base64.getDecoder().decode(base64Data);
                javafx.scene.image.Image img = new javafx.scene.image.Image(new java.io.ByteArrayInputStream(bytes));
                javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView(img);
                imgView.setFitWidth(80);
                imgView.setFitHeight(80);
                
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(80, 80);
                clip.setArcWidth(12);
                clip.setArcHeight(12);
                imgView.setClip(clip);
                return imgView;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        Label placeholder = new Label("👤");
        placeholder.setStyle("-fx-font-size: 32px; -fx-text-fill: -color-text-muted;");
        StackPane fallback = new StackPane(placeholder);
        fallback.setPrefSize(80, 80);
        fallback.setMaxSize(80, 80);
        fallback.setStyle("-fx-background-color: -color-bg-elevated; -fx-background-radius: 12; -fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 12;");
        return fallback;
    }

    private HBox createDetailRow(String key, String value) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Label keyLabel = new Label(key);
        keyLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label valLabel = new Label(value);
        valLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-main; -fx-font-weight: bold;");
        valLabel.setWrapText(true);
        row.getChildren().addAll(keyLabel, spacer, valLabel);
        return row;
    }

    // ==================== Instructions Panel ====================

    private VBox buildInstructionsPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(16));
        panel.setStyle("-fx-background-color: -color-bg-card; -fx-background-radius: 8;"
                + "-fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 8;"
                + "-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.02),10,0,0,2);");

        Label title = new Label("Exam Instructions & Rules");
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");
        panel.getChildren().add(title);

        List<String> requiredRules = new ArrayList<>();
        List<String> prohibitedRules = new ArrayList<>();
        List<String> importantRules = new ArrayList<>();

        for (String rule : parsedInstructions) {
            String rl = rule.toLowerCase();
            if (rl.contains("print") || rl.contains("bring") || rl.contains("original") || rl.contains("identity") || rl.contains(" card") || rl.contains("display")) {
                requiredRules.add(rule);
            } else if (rl.contains("phone") || rl.contains("mobile") || rl.contains("watch") || rl.contains("electronic") || rl.contains("prohibit") || rl.contains("book") || rl.contains("helping") || rl.contains("device") || rl.contains("ipod")) {
                prohibitedRules.add(rule);
            } else {
                importantRules.add(rule);
            }
        }

        // 1. Required Category Card
        VBox requiredCard = buildGroupedInstructionCard("✓ Required Items", requiredRules, "#10B981");
        
        // 2. Prohibited Category Card
        VBox prohibitedCard = buildGroupedInstructionCard("✕ Prohibited Items", prohibitedRules, "#EF4444");
        
        // 3. Important Notes Card
        VBox importantCard = buildGroupedInstructionCard("⚠ Important Instructions", importantRules, "#F59E0B");

        panel.getChildren().addAll(requiredCard, prohibitedCard, importantCard);
        return panel;
    }

    private VBox buildGroupedInstructionCard(String title, List<String> rules, String badgeColor) {
        VBox box = new VBox(6);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: -color-bg-elevated; -fx-background-radius: 6;"
                + "-fx-border-color: " + badgeColor + "; -fx-border-width: 0 0 0 3; -fx-border-radius: 0 6 6 0;");

        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + badgeColor + ";");
        box.getChildren().add(lbl);

        VBox list = new VBox(4);
        for (String rule : rules) {
            HBox item = new HBox(4);
            item.setAlignment(Pos.TOP_LEFT);
            Label bullet = new Label("•");
            bullet.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 12px;");
            Label text = new Label(rule);
            text.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-main;");
            text.setWrapText(true);
            HBox.setHgrow(text, Priority.ALWAYS);
            item.getChildren().addAll(bullet, text);
            list.getChildren().add(item);
        }

        if (rules.isEmpty()) {
            Label empty = new Label("None listed.");
            empty.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-muted; -fx-font-style: italic;");
            list.getChildren().add(empty);
        }

        box.getChildren().add(list);
        return box;
    }

    // ==================== Document Overlay Modal ====================

    private void showCouponOverlay() {
        if (overlayPane != null) {
            contentPane.getChildren().remove(overlayPane);
        }

        overlayPane = new StackPane();
        overlayPane.setStyle("-fx-background-color: rgba(15, 23, 42, 0.85);"); // Dark themed background container

        VBox modalBox = new VBox(0);
        modalBox.setStyle("-fx-background-color: -color-bg-main; -fx-background-radius: 12;"
                + "-fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 12;"
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 30, 0, 0, 10);");

        updateModalSize(modalBox);

        // Modern Toolbar
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(10, 16, 10, 16));
        toolbar.setStyle("-fx-background-color: -color-bg-card; -fx-background-radius: 12 12 0 0;"
                + "-fx-border-color: -color-border; -fx-border-width: 0 0 1 0;");

        Label title = new Label("Preserved Official Coupon");
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Zoom Buttons
        Button zoomOutBtn = new Button("➖");
        zoomOutBtn.setTooltip(new Tooltip("Zoom Out"));
        zoomOutBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -color-text-main; -fx-font-size: 12px;");
        zoomOutBtn.setOnAction(e -> {
            double z = webView.getZoom();
            if (z > 0.4) webView.setZoom(z - 0.1);
        });

        Label zoomLabel = new Label("Zoom");
        zoomLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");

        Button zoomInBtn = new Button("➕");
        zoomInBtn.setTooltip(new Tooltip("Zoom In"));
        zoomInBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -color-text-main; -fx-font-size: 12px;");
        zoomInBtn.setOnAction(e -> {
            double z = webView.getZoom();
            if (z < 2.0) webView.setZoom(z + 0.1);
        });

        Button fitWidthBtn = new Button("Fit Width");
        fitWidthBtn.setStyle("-fx-background-color: -color-bg-elevated; -fx-text-fill: -color-text-main;"
                + "-fx-border-color: -color-border; -fx-border-width: 1; -fx-font-size: 10px; -fx-padding: 4 8; -fx-background-radius: 4;");
        fitWidthBtn.setOnAction(e -> webView.setZoom(1.0));

        Button fitPageBtn = new Button("Fit Page");
        fitPageBtn.setStyle("-fx-background-color: -color-bg-elevated; -fx-text-fill: -color-text-main;"
                + "-fx-border-color: -color-border; -fx-border-width: 1; -fx-font-size: 10px; -fx-padding: 4 8; -fx-background-radius: 4;");
        fitPageBtn.setOnAction(e -> webView.setZoom(0.75));

        Button downloadBtn = new Button("💾 Save PDF");
        downloadBtn.setStyle("-fx-background-color: -color-accent; -fx-text-fill: white;"
                + "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 4;");
        downloadBtn.setOnAction(e -> {
            downloadBtn.setDisable(true);
            saveAsPdf(() -> downloadBtn.setDisable(false));
        });

        Button fullScreenBtn = new Button("▢");
        fullScreenBtn.setTooltip(new Tooltip("Toggle Full Screen"));
        fullScreenBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -color-text-main; -fx-font-size: 12px;");
        fullScreenBtn.setOnAction(e -> {
            isFullScreen = !isFullScreen;
            fullScreenBtn.setText(isFullScreen ? "⧉" : "▢");
            updateModalSize(modalBox);
        });

        Button closeBtn = new Button("✕");
        closeBtn.setTooltip(new Tooltip("Close"));
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #EF4444; -fx-font-size: 14px; -fx-font-weight: bold;");
        closeBtn.setOnAction(e -> hideCouponOverlay());

        toolbar.getChildren().addAll(title, spacer, zoomOutBtn, zoomLabel, zoomInBtn, fitWidthBtn, fitPageBtn, downloadBtn, fullScreenBtn, closeBtn);

        // Official coupon is kept white inside
        ScrollPane webScroll = new ScrollPane(webView);
        webScroll.setFitToWidth(true);
        webScroll.setFitToHeight(true);
        webScroll.setStyle("-fx-background-color: #ffffff; -fx-background: #ffffff; -fx-border-color: transparent; -fx-background-radius: 0 0 12 12;");
        VBox.setVgrow(webScroll, Priority.ALWAYS);

        modalBox.getChildren().addAll(toolbar, webScroll);

        overlayPane.getChildren().add(modalBox);
        StackPane.setAlignment(modalBox, Pos.CENTER);

        overlayPane.setOpacity(0);
        contentPane.getChildren().add(overlayPane);
        
        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(javafx.util.Duration.millis(250), overlayPane);
        fade.setToValue(1.0);
        fade.play();
    }

    private void updateModalSize(VBox modalBox) {
        if (isFullScreen) {
            modalBox.setMaxWidth(Double.MAX_VALUE);
            modalBox.setMaxHeight(Double.MAX_VALUE);
            StackPane.setMargin(modalBox, new Insets(10));
        } else {
            modalBox.setMaxWidth(850);
            modalBox.setMaxHeight(650);
            StackPane.setMargin(modalBox, new Insets(40, 60, 40, 60));
        }
    }

    private void hideCouponOverlay() {
        if (overlayPane != null) {
            javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(javafx.util.Duration.millis(200), overlayPane);
            fade.setToValue(0.0);
            fade.setOnFinished(e -> contentPane.getChildren().remove(overlayPane));
            fade.play();
        }
    }

    private void triggerSystemPrint() {
        try {
            PrinterJob job = PrinterJob.createPrinterJob();
            if (job != null) {
                if (job.showPrintDialog(root.getScene().getWindow())) {
                    webView.getEngine().print(job);
                    job.endJob();
                }
            } else {
                showErrorAlert("Print Error", "Could not initialize printer job.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showErrorAlert("Print Failed", "Could not print: " + ex.getMessage());
        }
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
                    
                    PageLayout pageLayout = pdfPrinter.createPageLayout(Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.HARDWARE_MINIMUM);
                    job.getJobSettings().setPageLayout(pageLayout);
                    job.getJobSettings().setJobName("Exam_Coupon_" + context.getSessionRegistration().replaceAll("[^a-zA-Z0-9]", ""));
                    
                    webView.getEngine().executeScript(
                        "var style = document.createElement('style');" +
                        "style.type = 'text/css';" +
                        "style.innerHTML = '@media print { body { zoom: 0.85; margin: 0 !important; } }';" +
                        "document.head.appendChild(style);"
                    );
                    
                    webView.getEngine().print(job);
                    job.endJob();
                    onComplete.run();
                    return;
                }
            }

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
        
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().addAll(title, spacer);
        content.getChildren().add(header);

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
            context.notificationService().showInfo(title, message);
        });
    }

    private void showErrorAlert(String title, String message) {
        Platform.runLater(() -> {
            context.notificationService().showError(title, message);
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
