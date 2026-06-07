package com.assignly.util;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.util.Duration;

public final class ShimmerBuilder {

    private ShimmerBuilder() {}

    private static Region createBlock(double width, double height, String borderRadius) {
        Region region = new Region();
        if (width > 0) {
            region.setPrefWidth(width);
            region.setMinWidth(width);
            region.setMaxWidth(width);
        } else {
            HBox.setHgrow(region, Priority.ALWAYS);
            VBox.setVgrow(region, Priority.ALWAYS);
        }
        region.setPrefHeight(height);
        region.setMinHeight(height);
        region.setMaxHeight(height);
        region.setStyle("-fx-background-color: -color-border; -fx-background-radius: " + borderRadius + ";");
        return region;
    }

    private static void applyPulseAnimation(Node node) {
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(node.opacityProperty(), 0.35)),
            new KeyFrame(Duration.millis(750), new KeyValue(node.opacityProperty(), 0.75)),
            new KeyFrame(Duration.millis(1500), new KeyValue(node.opacityProperty(), 0.35))
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    public static Region buildDashboardShimmer() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20, 28, 28, 28));
        root.setFillWidth(true);

        HBox header = new HBox(createBlock(200, 28, "6"));
        root.getChildren().add(header);

        Region welcome = createBlock(-1, 140, "16");
        root.getChildren().add(welcome);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        for (int c = 0; c < 3; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(33.33);
            grid.getColumnConstraints().add(cc);
        }
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 3; c++) {
                grid.add(createBlock(-1, 95, "14"), c, r);
            }
        }
        root.getChildren().add(grid);

        HBox charts = new HBox(20);
        Region chart1 = createBlock(-1, 260, "14");
        Region chart2 = createBlock(-1, 260, "14");
        HBox.setHgrow(chart1, Priority.ALWAYS);
        HBox.setHgrow(chart2, Priority.ALWAYS);
        charts.getChildren().addAll(chart1, chart2);
        root.getChildren().add(charts);

        applyPulseAnimation(root);
        return root;
    }

    public static Region buildTimetableShimmer() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20, 28, 28, 28));
        root.setFillWidth(true);

        HBox topBar = new HBox(20);
        topBar.getChildren().addAll(
            createBlock(240, 32, "6"),
            createBlock(120, 32, "6")
        );
        root.getChildren().add(topBar);

        HBox statsBar = new HBox(16);
        for (int i = 0; i < 4; i++) {
            Region statCard = createBlock(-1, 55, "8");
            HBox.setHgrow(statCard, Priority.ALWAYS);
            statsBar.getChildren().add(statCard);
        }
        root.getChildren().add(statsBar);

        for (int i = 0; i < 3; i++) {
            VBox dayBox = new VBox(10);
            dayBox.getChildren().addAll(
                createBlock(100, 16, "4"),
                createBlock(-1, 80, "8")
            );
            root.getChildren().add(dayBox);
        }

        applyPulseAnimation(root);
        return root;
    }

    public static Region buildCoursesShimmer() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20, 28, 28, 28));
        root.setFillWidth(true);

        root.getChildren().add(createBlock(250, 28, "6"));

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        for (int c = 0; c < 3; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(33.33);
            grid.getColumnConstraints().add(cc);
        }
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 3; c++) {
                grid.add(createBlock(-1, 140, "12"), c, r);
            }
        }
        root.getChildren().add(grid);

        applyPulseAnimation(root);
        return root;
    }

    public static Region buildResultShimmer() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20, 28, 28, 28));
        root.setFillWidth(true);

        HBox topBar = new HBox(16);
        topBar.getChildren().addAll(
            createBlock(200, 36, "6"),
            createBlock(120, 36, "6")
        );
        root.getChildren().add(topBar);

        root.getChildren().add(createBlock(-1, 220, "14"));

        for (int i = 0; i < 3; i++) {
            root.getChildren().add(createBlock(-1, 55, "8"));
        }

        applyPulseAnimation(root);
        return root;
    }

    public static Region buildFeeShimmer() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20, 28, 28, 28));
        root.setFillWidth(true);

        root.getChildren().add(createBlock(180, 24, "6"));

        HBox summary = new HBox(20);
        Region left = createBlock(-1, 140, "12");
        Region right = createBlock(-1, 140, "12");
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        summary.getChildren().addAll(left, right);
        root.getChildren().add(summary);

        root.getChildren().add(createBlock(-1, 200, "12"));

        applyPulseAnimation(root);
        return root;
    }

    public static Region buildScholarshipShimmer() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20, 28, 28, 28));
        root.setFillWidth(true);

        root.getChildren().add(createBlock(-1, 120, "12"));

        for (int i = 0; i < 2; i++) {
            root.getChildren().add(createBlock(-1, 90, "10"));
        }

        applyPulseAnimation(root);
        return root;
    }

    public static Region buildCoursePortalShimmer() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20, 28, 28, 28));
        root.setFillWidth(true);

        HBox topBar = new HBox(16);
        topBar.getChildren().addAll(
            createBlock(250, 36, "6"),
            createBlock(150, 36, "6")
        );
        root.getChildren().add(topBar);

        root.getChildren().add(createBlock(-1, 40, "6"));

        for (int i = 0; i < 3; i++) {
            root.getChildren().add(createBlock(-1, 110, "10"));
        }

        applyPulseAnimation(root);
        return root;
    }

    public static Region buildExamCouponShimmer() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20, 28, 28, 28));
        root.setFillWidth(true);

        HBox metrics = new HBox(12);
        for (int i = 0; i < 5; i++) {
            Region card = createBlock(-1, 75, "10");
            HBox.setHgrow(card, Priority.ALWAYS);
            metrics.getChildren().add(card);
        }
        root.getChildren().add(metrics);

        HBox body = new HBox(20);
        VBox scheduleCol = new VBox(12);
        HBox.setHgrow(scheduleCol, Priority.ALWAYS);
        for (int i = 0; i < 3; i++) {
            scheduleCol.getChildren().add(createBlock(-1, 100, "12"));
        }
        Region sidebar = createBlock(280, 320, "12");
        body.getChildren().addAll(scheduleCol, sidebar);
        root.getChildren().add(body);

        applyPulseAnimation(root);
        return root;
    }
}
