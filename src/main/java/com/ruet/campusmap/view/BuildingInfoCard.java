package com.ruet.campusmap.view;

import com.ruet.campusmap.model.BuildingPolygon;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

/**
 * Modern floating Google Maps-style place sheet that displays building or POI details
 * when clicked on the map, with smooth slide-up entrance animation and action chips.
 */
public class BuildingInfoCard {

    private final VBox cardContainer;
    private final Label categoryBadge;
    private final Label titleLabel;
    private final Label subtitleLabel;
    private final Button directionsBtn;
    private final Button copyBtn;
    private final Button innerMapBtn;
    private final StackPane closeBtn;
    private final SVGPath closeIcon;
    private final SVGPath directionsIcon;
    private final SVGPath copyIcon;
    private final SVGPath innerMapIcon;
    private final Separator divider;
    private final Label feedbackLabel;
    private java.util.function.Consumer<BuildingPolygon> onOpenInnerMap;
    private BuildingPolygon currentBuilding;
    private boolean isDark = false;
    private ParallelTransition currentAnim;

    public BuildingInfoCard() {
        cardContainer = new VBox(12);
        cardContainer.setPrefWidth(370);
        cardContainer.setMaxWidth(370);
        cardContainer.setPadding(new Insets(18, 20, 18, 20));
        cardContainer.setEffect(new DropShadow(18, 0, 6, Color.rgb(32, 33, 36, 0.22)));
        cardContainer.setVisible(false);
        cardContainer.setManaged(false);

        // Prevent dragging or clicking on card from panning map underneath
        cardContainer.setOnMousePressed(javafx.event.Event::consume);
        cardContainer.setOnMouseDragged(javafx.event.Event::consume);

        // Top Row: Category Badge Pill + Close Button
        categoryBadge = new Label("CAMPUS BUILDING");
        categoryBadge.setStyle(
            "-fx-font-size: 11px; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 4 10; " +
            "-fx-background-radius: 12px;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Circular close button with SVG cross
        closeIcon = new SVGPath();
        closeIcon.setContent("M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z");
        closeIcon.setScaleX(0.8);
        closeIcon.setScaleY(0.8);

        closeBtn = new StackPane(closeIcon);
        closeBtn.setPrefSize(28, 28);
        closeBtn.setMinSize(28, 28);
        closeBtn.setMaxSize(28, 28);
        closeBtn.setStyle("-fx-cursor: hand; -fx-background-radius: 14px;");
        Tooltip.install(closeBtn, new Tooltip("Close"));
        closeBtn.setOnMousePressed(e -> hide());

        HBox topRow = new HBox(8, categoryBadge, spacer, closeBtn);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Title
        titleLabel = new Label("Building Name");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-font-family: 'Segoe UI', Roboto, sans-serif;");
        titleLabel.setWrapText(true);

        // Subtitle / description
        subtitleLabel = new Label("RUET Campus • Rajshahi-6204");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-font-family: 'Segoe UI', Roboto, sans-serif;");

        // Subtle divider
        divider = new Separator();
        divider.setStyle("-fx-opacity: 0.4;");

        // Action Buttons Row (Inner Map + Directions + Copy Name)
        innerMapIcon = new SVGPath();
        innerMapIcon.setContent("M11.99 18.54l-7.37-5.73L3 14.07l9 7 9-7-1.63-1.27-7.38 5.74zM12 16l7.36-5.73L21 9l-9-7-9 7 1.63 1.27L12 16z");
        innerMapIcon.setScaleX(0.75);
        innerMapIcon.setScaleY(0.75);

        innerMapBtn = new Button("Floor Plan", innerMapIcon);
        innerMapBtn.setVisible(false);
        innerMapBtn.setManaged(false);
        innerMapBtn.setOnAction(e -> {
            if (onOpenInnerMap != null && currentBuilding != null) {
                onOpenInnerMap.accept(currentBuilding);
            }
        });

        directionsIcon = new SVGPath();
        directionsIcon.setContent("M21.71 11.29l-9-9a.996.996 0 0 0-1.41 0l-9 9a.996.996 0 0 0 0 1.41l9 9c.39.39 1.02.39 1.41 0l9-9a.996.996 0 0 0 0-1.41zM14 14.5V12h-4v3H8v-4c0-.55.45-1 1-1h5V7.5l3.5 3.5-3.5 3.5z");
        directionsIcon.setFill(Color.WHITE);
        directionsIcon.setScaleX(0.8);
        directionsIcon.setScaleY(0.8);

        directionsBtn = new Button("Directions", directionsIcon);
        directionsBtn.setStyle(
            "-fx-background-color: #1a73e8; " +
            "-fx-text-fill: #ffffff; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 20px; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 7 14;"
        );

        copyIcon = new SVGPath();
        copyIcon.setContent("M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z");
        copyIcon.setScaleX(0.75);
        copyIcon.setScaleY(0.75);

        copyBtn = new Button("Copy Name", copyIcon);
        copyBtn.setStyle(
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 500; " +
            "-fx-background-radius: 20px; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 7 12;"
        );

        feedbackLabel = new Label("✓ Copied!");
        feedbackLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #34a853; -fx-font-weight: bold;");
        feedbackLabel.setVisible(false);
        feedbackLabel.setManaged(false);

        copyBtn.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(titleLabel.getText());
            clipboard.setContent(content);

            feedbackLabel.setVisible(true);
            feedbackLabel.setManaged(true);
            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(ev -> {
                feedbackLabel.setVisible(false);
                feedbackLabel.setManaged(false);
            });
            pause.play();
        });

        HBox actionsRow = new HBox(8, innerMapBtn, directionsBtn, copyBtn, feedbackLabel);
        actionsRow.setAlignment(Pos.CENTER_LEFT);

        cardContainer.getChildren().addAll(topRow, titleLabel, subtitleLabel, divider, actionsRow);

        StackPane.setAlignment(cardContainer, Pos.BOTTOM_LEFT);
        StackPane.setMargin(cardContainer, new Insets(0, 0, 80, 24));

        applyTheme(false);
    }

    public void showBuilding(BuildingPolygon building) {
        if (building == null) return;
        this.currentBuilding = building;
        String name = building.getName();
        titleLabel.setText(name);

        String category;
        String lower = name.toLowerCase();
        if (lower.contains("hall")) {
            category = "🏠 RESIDENTIAL HALL";
        } else if (lower.contains("library")) {
            category = "📚 CENTRAL LIBRARY";
        } else if (lower.contains("cafeteria") || lower.contains("canteen")) {
            category = "🍽️ DINING & FOOD";
        } else if (lower.contains("admin")) {
            category = "🏢 ADMINISTRATION";
        } else if (lower.contains("auditorium")) {
            category = "🎭 AUDITORIUM";
        } else if (lower.contains("medical") || lower.contains("hospital")) {
            category = "🏥 HEALTH CENTER";
        } else if (lower.contains("mosque")) {
            category = "🕌 CAMPUS MOSQUE";
        } else {
            category = "🏛️ ACADEMIC BUILDING";
        }

        categoryBadge.setText(category);
        subtitleLabel.setText("RUET Campus • Rajshahi-6204");

        boolean hasInner = com.ruet.campusmap.service.InnerMapRegistry.hasInnerMap(name);
        innerMapBtn.setVisible(hasInner);
        innerMapBtn.setManaged(hasInner);

        animateShow();
    }

    public void showPoi(String name, String category, String details) {
        this.currentBuilding = null;
        innerMapBtn.setVisible(false);
        innerMapBtn.setManaged(false);

        titleLabel.setText(name);
        categoryBadge.setText(category != null ? category.toUpperCase() : "POINT OF INTEREST");
        subtitleLabel.setText(details != null && !details.isBlank() ? details : "RUET Campus Facilities");
        animateShow();
    }

    private void animateShow() {
        if (currentAnim != null) {
            currentAnim.stop();
        }
        cardContainer.setVisible(true);
        cardContainer.setManaged(true);
        cardContainer.toFront();

        cardContainer.setOpacity(0.0);
        cardContainer.setTranslateY(20);

        FadeTransition fade = new FadeTransition(Duration.millis(250), cardContainer);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition translate = new TranslateTransition(Duration.millis(250), cardContainer);
        translate.setFromY(20);
        translate.setToY(0);
        translate.setInterpolator(Interpolator.EASE_OUT);

        currentAnim = new ParallelTransition(fade, translate);
        currentAnim.play();
    }

    public void hide() {
        if (!cardContainer.isVisible()) return;
        if (currentAnim != null) {
            currentAnim.stop();
        }
        FadeTransition fade = new FadeTransition(Duration.millis(180), cardContainer);
        fade.setFromValue(cardContainer.getOpacity());
        fade.setToValue(0.0);
        fade.setInterpolator(Interpolator.EASE_IN);

        TranslateTransition translate = new TranslateTransition(Duration.millis(180), cardContainer);
        translate.setFromY(cardContainer.getTranslateY());
        translate.setToY(15);
        translate.setInterpolator(Interpolator.EASE_IN);

        currentAnim = new ParallelTransition(fade, translate);
        currentAnim.setOnFinished(e -> {
            cardContainer.setVisible(false);
            cardContainer.setManaged(false);
            cardContainer.setTranslateY(0);
        });
        currentAnim.play();
    }

    public void applyTheme(boolean isDark) {
        this.isDark = isDark;
        if (isDark) {
            cardContainer.setStyle(
                "-fx-background-color: #202124; " +
                "-fx-background-radius: 16px; " +
                "-fx-border-radius: 16px; " +
                "-fx-border-color: #3c4043; " +
                "-fx-border-width: 1px;"
            );
            categoryBadge.setStyle(
                "-fx-background-color: #173154; " +
                "-fx-text-fill: #8ab4f8; " +
                "-fx-font-size: 11px; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 4 10; " +
                "-fx-background-radius: 12px;"
            );
            titleLabel.setTextFill(Color.web("#e8eaed"));
            subtitleLabel.setTextFill(Color.web("#9aa0a6"));
            closeIcon.setFill(Color.web("#9aa0a6"));
            closeBtn.setStyle("-fx-cursor: hand; -fx-background-color: #303134; -fx-background-radius: 14px;");
            copyBtn.setStyle(
                "-fx-background-color: #303134; " +
                "-fx-text-fill: #e8eaed; " +
                "-fx-border-color: #5f6368; " +
                "-fx-border-radius: 20px; " +
                "-fx-border-width: 1px; " +
                "-fx-font-size: 12px; " +
                "-fx-font-weight: 500; " +
                "-fx-background-radius: 20px; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 7 12;"
            );
            copyIcon.setFill(Color.web("#bdc1c6"));
            innerMapBtn.setStyle(
                "-fx-background-color: #14b8a6; " +
                "-fx-text-fill: #0f172a; " +
                "-fx-font-size: 12px; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 20px; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 7 14;"
            );
            innerMapIcon.setFill(Color.web("#0f172a"));
        } else {
            cardContainer.setStyle(
                "-fx-background-color: #ffffff; " +
                "-fx-background-radius: 16px; " +
                "-fx-border-radius: 16px; " +
                "-fx-border-color: #dadce0; " +
                "-fx-border-width: 1px;"
            );
            categoryBadge.setStyle(
                "-fx-background-color: #e8f0fe; " +
                "-fx-text-fill: #1a73e8; " +
                "-fx-font-size: 11px; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 4 10; " +
                "-fx-background-radius: 12px;"
            );
            titleLabel.setTextFill(Color.web("#202124"));
            subtitleLabel.setTextFill(Color.web("#5f6368"));
            closeIcon.setFill(Color.web("#5f6368"));
            closeBtn.setStyle("-fx-cursor: hand; -fx-background-color: #f1f3f4; -fx-background-radius: 14px;");
            copyBtn.setStyle(
                "-fx-background-color: #f1f3f4; " +
                "-fx-text-fill: #3c4043; " +
                "-fx-border-color: #dadce0; " +
                "-fx-border-radius: 20px; " +
                "-fx-border-width: 1px; " +
                "-fx-font-size: 12px; " +
                "-fx-font-weight: 500; " +
                "-fx-background-radius: 20px; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 7 12;"
            );
            copyIcon.setFill(Color.web("#5f6368"));
            innerMapBtn.setStyle(
                "-fx-background-color: #0d9488; " +
                "-fx-text-fill: #ffffff; " +
                "-fx-font-size: 12px; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 20px; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 7 14;"
            );
            innerMapIcon.setFill(Color.WHITE);
        }
    }

    public void setOnOpenInnerMap(java.util.function.Consumer<BuildingPolygon> callback) {
        this.onOpenInnerMap = callback;
    }

    public VBox getContainer() {
        return cardContainer;
    }
}
