package com.ruet.campusmap.view;

import com.ruet.campusmap.model.AppSettings;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

/**
 * Modern floating Settings card (Google Maps / Apple Maps style) anchored
 * at the top-right beneath the floating action buttons.
 */
public class SettingsCard {

    private final VBox container;
    private final AppSettings settings;
    private final Stage stage;
    private final StackPane rootPane;
    private Runnable onCloseCallback;

    // References for dynamic theme styling
    private final Label titleLabel;
    private final Button closeBtn;
    private final VBox contentBox;
    private final ScrollPane scrollPane;

    // Segmented buttons
    private final Map<AppSettings.ThemeMode, Button> themeButtons = new HashMap<>();
    private final Map<AppSettings.BuildingLabelMode, Button> labelButtons = new HashMap<>();
    private final Map<AppSettings.TransportMode, Button> transportButtons = new HashMap<>();

    // Accessibility Switch
    private final Button avoidStairsBtn;

    // Filter Chips
    private final Button foodChip;
    private final Button printerChip;
    private final Button restroomChip;
    private final Button parkingChip;

    // Action buttons in About section
    private final Button errorBtn;
    private final Button securityBtn;

    // Section headers & labels for theme color update
    private final java.util.List<Label> sectionHeaders = new java.util.ArrayList<>();
    private final java.util.List<Label> itemLabels = new java.util.ArrayList<>();
    private final java.util.List<Label> hintLabels = new java.util.ArrayList<>();
    private final java.util.List<Separator> separators = new java.util.ArrayList<>();

    private boolean isAnimating = false;

    public SettingsCard(AppSettings settings, Stage stage, StackPane rootPane) {
        this.settings = settings;
        this.stage = stage;
        this.rootPane = rootPane;

        container = new VBox(10);
        container.setPrefWidth(350);
        container.setMaxWidth(350);
        container.setPadding(new Insets(16));
        container.setEffect(new DropShadow(20, 0, 6, Color.rgb(60, 64, 67, 0.35)));
        container.setVisible(false);
        container.setManaged(false);

        // Prevent dragging on settings card from moving the campus map underneath
        container.setOnMousePressed(javafx.event.Event::consume);
        container.setOnMouseDragged(javafx.event.Event::consume);

        // --- 1. Header (Title + Close) ---
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        titleLabel = new Label("⚙️ Settings");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 14px; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> hide());

        header.getChildren().addAll(titleLabel, closeBtn);

        // --- Content Box with Scrollable flow ---
        contentBox = new VBox(12);
        contentBox.setPadding(new Insets(0, 4, 0, 0));

        // ==========================================
        // SECTION 1: APPEARANCE
        // ==========================================
        VBox appearanceSection = new VBox(8);
        Label appearanceTitle = createSectionHeader("APPEARANCE");

        // Theme Selector
        Label themeLabel = createItemLabel("Theme");
        HBox themePill = new HBox(4);
        themePill.setAlignment(Pos.CENTER);
        for (AppSettings.ThemeMode tm : AppSettings.ThemeMode.values()) {
            String text = switch (tm) {
                case LIGHT -> "☀️ Light";
                case DARK -> "🌙 Dark";
                case AUTO -> "🕒 Auto";
            };
            Button btn = createSegmentButton(text);
            btn.setOnAction(e -> {
                settings.setThemeMode(tm);
                updateButtonStates();
                CampusToast.show(rootPane, "Theme: " + text, settings.isEffectiveDarkMode());
            });
            themeButtons.put(tm, btn);
            HBox.setHgrow(btn, Priority.ALWAYS);
            btn.setMaxWidth(Double.MAX_VALUE);
            themePill.getChildren().add(btn);
        }

        // Building Labels Selector
        Label labelsTitle = createItemLabel("Building Labels");
        HBox labelsPill = new HBox(4);
        labelsPill.setAlignment(Pos.CENTER);
        for (AppSettings.BuildingLabelMode lm : AppSettings.BuildingLabelMode.values()) {
            String text = switch (lm) {
                case FULL_NAME -> "Full Name";
                case CODE -> "Code";
                case NONE -> "Off";
            };
            Button btn = createSegmentButton(text);
            btn.setOnAction(e -> {
                settings.setLabelMode(lm);
                updateButtonStates();
                CampusToast.show(rootPane, "Building Labels: " + text, settings.isEffectiveDarkMode());
            });
            labelButtons.put(lm, btn);
            HBox.setHgrow(btn, Priority.ALWAYS);
            btn.setMaxWidth(Double.MAX_VALUE);
            labelsPill.getChildren().add(btn);
        }

        appearanceSection.getChildren().addAll(appearanceTitle, themeLabel, themePill, labelsTitle, labelsPill);

        // ==========================================
        // SECTION 2: NAVIGATION & ACCESSIBILITY
        // ==========================================
        VBox navSection = new VBox(8);
        Label navTitle = createSectionHeader("NAVIGATION & ACCESSIBILITY");

        // Avoid Stairs / Wheelchair Accessible
        HBox stairsRow = new HBox(8);
        stairsRow.setAlignment(Pos.CENTER_LEFT);

        VBox stairsTextBox = new VBox(2);
        Label stairsLabel = createItemLabel("Avoid Stairs / Accessible");
        Label stairsHint = createHintLabel("Prioritizes ramps & level walkways");
        stairsTextBox.getChildren().addAll(stairsLabel, stairsHint);
        HBox.setHgrow(stairsTextBox, Priority.ALWAYS);

        avoidStairsBtn = new Button("OFF");
        avoidStairsBtn.setPrefWidth(56);
        avoidStairsBtn.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: #555; -fx-font-weight: bold; -fx-background-radius: 12px; -fx-cursor: hand;");
        avoidStairsBtn.setOnAction(e -> {
            boolean newState = !settings.isAvoidStairs();
            settings.setAvoidStairs(newState);
            updateButtonStates();
            CampusToast.show(rootPane, newState ? "♿ Wheelchair Accessible mode: ON (Ramps prioritized)" : "🚶 Standard walkways enabled", settings.isEffectiveDarkMode());
        });
        stairsRow.getChildren().addAll(stairsTextBox, avoidStairsBtn);

        // Default Transport Mode
        Label transportLabel = createItemLabel("Default Transport Mode");
        HBox transportPill = new HBox(4);
        transportPill.setAlignment(Pos.CENTER);
        for (AppSettings.TransportMode mode : AppSettings.TransportMode.values()) {
            String text = switch (mode) {
                case WALK -> "🚶 Walk";
                case BIKE -> "🚲 Bike";
                case SHUTTLE -> "🚐 Shuttle";
            };
            Button btn = createSegmentButton(text);
            btn.setOnAction(e -> {
                settings.setTransportMode(mode);
                updateButtonStates();
                String toastMsg = switch (mode) {
                    case WALK -> "🚶 Walking mode: Standard pedestrian walkways";
                    case BIKE -> "🚲 Bicycle mode: Campus cycle stands highlighted";
                    case SHUTTLE -> "🚐 Shuttle mode: Campus circular stops highlighted";
                };
                CampusToast.show(rootPane, toastMsg, settings.isEffectiveDarkMode());
            });
            transportButtons.put(mode, btn);
            HBox.setHgrow(btn, Priority.ALWAYS);
            btn.setMaxWidth(Double.MAX_VALUE);
            transportPill.getChildren().add(btn);
        }

        navSection.getChildren().addAll(navTitle, stairsRow, transportLabel, transportPill);

        // ==========================================
        // SECTION 3: MAP LAYERS & FILTERS
        // ==========================================
        VBox filterSection = new VBox(8);
        Label filterTitle = createSectionHeader("MAP LAYERS & FILTERS");
        Label quickTogglesLabel = createItemLabel("Quick Highlights (POIs)");

        FlowPane chipsPane = new FlowPane(8, 8);
        foodChip = createFilterChip("🍽️ Food", () -> {
            boolean active = !settings.isShowFood();
            settings.setShowFood(active);
            updateButtonStates();
            CampusToast.show(rootPane, active ? "🍽️ Food & Cafeterias shown" : "🍽️ Food markers hidden", settings.isEffectiveDarkMode());
        });
        printerChip = createFilterChip("🖨️ Printers", () -> {
            boolean active = !settings.isShowPrinters();
            settings.setShowPrinters(active);
            updateButtonStates();
            CampusToast.show(rootPane, active ? "🖨️ Printing & Copy spots shown" : "🖨️ Printer markers hidden", settings.isEffectiveDarkMode());
        });
        restroomChip = createFilterChip("🚻 Restrooms", () -> {
            boolean active = !settings.isShowRestrooms();
            settings.setShowRestrooms(active);
            updateButtonStates();
            CampusToast.show(rootPane, active ? "🚻 Restroom locations shown" : "🚻 Restroom markers hidden", settings.isEffectiveDarkMode());
        });
        parkingChip = createFilterChip("🅿️ Parking", () -> {
            boolean active = !settings.isShowParking();
            settings.setShowParking(active);
            updateButtonStates();
            CampusToast.show(rootPane, active ? "🅿️ Parking areas shown" : "🅿️ Parking markers hidden", settings.isEffectiveDarkMode());
        });

        chipsPane.getChildren().addAll(foodChip, printerChip, restroomChip, parkingChip);
        filterSection.getChildren().addAll(filterTitle, quickTogglesLabel, chipsPane);

        // ==========================================
        // SECTION 4: ABOUT & SUPPORT
        // ==========================================
        VBox supportSection = new VBox(8);
        Label supportTitle = createSectionHeader("ABOUT & SUPPORT");

        HBox supportBtnRow = new HBox(8);
        errorBtn = new Button("📝 Report Map Error");
        errorBtn.setStyle("-fx-background-color: #f1f3f4; -fx-text-fill: #3c4043; -fx-font-size: 12px; -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 6 10;");
        errorBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(errorBtn, Priority.ALWAYS);
        errorBtn.setOnAction(e -> ReportErrorDialog.show(stage));

        securityBtn = new Button("🚨 Campus Security");
        securityBtn.setStyle("-fx-background-color: #fce8e6; -fx-text-fill: #d93025; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 6 10;");
        securityBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(securityBtn, Priority.ALWAYS);
        securityBtn.setOnAction(e -> CampusSecurityDialog.show(stage));

        supportBtnRow.getChildren().addAll(errorBtn, securityBtn);
        supportSection.getChildren().addAll(supportTitle, supportBtnRow);

        // --- Assemble All Sections with Separators ---
        Separator sep1 = createSeparator();
        Separator sep2 = createSeparator();
        Separator sep3 = createSeparator();

        contentBox.getChildren().addAll(
            appearanceSection,
            sep1,
            navSection,
            sep2,
            filterSection,
            sep3,
            supportSection
        );

        scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setMaxHeight(500);

        container.getChildren().addAll(header, scrollPane);

        // Position floating top-right (just below the floating action buttons)
        StackPane.setAlignment(container, Pos.TOP_RIGHT);
        StackPane.setMargin(container, new Insets(72, 18, 0, 0));

        updateButtonStates();
        applyTheme(settings.isEffectiveDarkMode());
    }

    private Separator createSeparator() {
        Separator s = new Separator();
        s.setStyle("-fx-opacity: 0.5;");
        separators.add(s);
        return s;
    }

    private Label createSectionHeader(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 0.5px;");
        sectionHeaders.add(l);
        return l;
    }

    private Label createItemLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        itemLabels.add(l);
        return l;
    }

    private Label createHintLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 10px;");
        hintLabels.add(l);
        return l;
    }

    private Button createSegmentButton(String text) {
        Button btn = new Button(text);
        btn.setStyle(
            "-fx-background-color: #f1f3f4; " +
            "-fx-text-fill: #3c4043; " +
            "-fx-font-size: 11px; " +
            "-fx-background-radius: 6px; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 5 8;"
        );
        return btn;
    }

    private Button createFilterChip(String text, Runnable onClick) {
        Button btn = new Button(text);
        btn.setStyle(
            "-fx-background-color: #f1f3f4; " +
            "-fx-text-fill: #3c4043; " +
            "-fx-font-size: 11px; " +
            "-fx-border-color: #dadce0; " +
            "-fx-border-radius: 14px; " +
            "-fx-background-radius: 14px; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 5 12;"
        );
        btn.setOnAction(e -> onClick.run());
        return btn;
    }

    public void updateButtonStates() {
        boolean isDark = settings.isEffectiveDarkMode();

        // 1. Theme buttons
        for (Map.Entry<AppSettings.ThemeMode, Button> entry : themeButtons.entrySet()) {
            boolean active = (entry.getKey() == settings.getThemeMode());
            setSegmentStyle(entry.getValue(), active, isDark);
        }

        // 2. Building labels
        for (Map.Entry<AppSettings.BuildingLabelMode, Button> entry : labelButtons.entrySet()) {
            boolean active = (entry.getKey() == settings.getLabelMode());
            setSegmentStyle(entry.getValue(), active, isDark);
        }

        // 3. Transport mode
        for (Map.Entry<AppSettings.TransportMode, Button> entry : transportButtons.entrySet()) {
            boolean active = (entry.getKey() == settings.getTransportMode());
            setSegmentStyle(entry.getValue(), active, isDark);
        }

        // 4. Avoid stairs
        if (settings.isAvoidStairs()) {
            avoidStairsBtn.setText("ON");
            avoidStairsBtn.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 12px; -fx-cursor: hand;");
        } else {
            avoidStairsBtn.setText("OFF");
            avoidStairsBtn.setStyle("-fx-background-color: " + (isDark ? "#3c4043" : "#e0e0e0") + "; -fx-text-fill: " + (isDark ? "#9aa0a6" : "#555") + "; -fx-font-weight: bold; -fx-background-radius: 12px; -fx-cursor: hand;");
        }

        // 5. POI Chips
        setChipStyle(foodChip, settings.isShowFood(), "#ea8600", isDark);
        setChipStyle(printerChip, settings.isShowPrinters(), "#1a73e8", isDark);
        setChipStyle(restroomChip, settings.isShowRestrooms(), "#00897b", isDark);
        setChipStyle(parkingChip, settings.isShowParking(), "#1e8e3e", isDark);
    }

    private void setSegmentStyle(Button btn, boolean active, boolean isDark) {
        if (active) {
            btn.setStyle(
                "-fx-background-color: #1a73e8; " +
                "-fx-text-fill: #ffffff; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 11px; " +
                "-fx-background-radius: 6px; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 5 8;"
            );
        } else {
            btn.setStyle(
                "-fx-background-color: " + (isDark ? "#303134" : "#f1f3f4") + "; " +
                "-fx-text-fill: " + (isDark ? "#bdc1c6" : "#3c4043") + "; " +
                "-fx-font-weight: normal; " +
                "-fx-font-size: 11px; " +
                "-fx-background-radius: 6px; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 5 8;"
            );
        }
    }

    private void setChipStyle(Button chip, boolean active, String activeColor, boolean isDark) {
        if (active) {
            chip.setStyle(
                "-fx-background-color: " + activeColor + "; " +
                "-fx-text-fill: #ffffff; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 11px; " +
                "-fx-border-color: " + activeColor + "; " +
                "-fx-border-radius: 14px; " +
                "-fx-background-radius: 14px; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 5 12;"
            );
        } else {
            chip.setStyle(
                "-fx-background-color: " + (isDark ? "#282a2e" : "#ffffff") + "; " +
                "-fx-text-fill: " + (isDark ? "#bdc1c6" : "#5f6368") + "; " +
                "-fx-font-weight: normal; " +
                "-fx-font-size: 11px; " +
                "-fx-border-color: " + (isDark ? "#4a4d52" : "#dadce0") + "; " +
                "-fx-border-radius: 14px; " +
                "-fx-background-radius: 14px; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 5 12;"
            );
        }
    }

    public void applyTheme(boolean isDark) {
        if (isDark) {
            container.setStyle(
                "-fx-background-color: #202124; " +
                "-fx-background-radius: 14px; " +
                "-fx-border-radius: 14px; " +
                "-fx-border-color: #3c4043; " +
                "-fx-border-width: 1px;"
            );
            titleLabel.setTextFill(Color.web("#e8eaed"));
            closeBtn.setTextFill(Color.web("#9aa0a6"));
            for (Label l : sectionHeaders) l.setTextFill(Color.web("#8ab4f8"));
            for (Label l : itemLabels) l.setTextFill(Color.web("#e8eaed"));
            for (Label l : hintLabels) l.setTextFill(Color.web("#9aa0a6"));

            errorBtn.setStyle("-fx-background-color: #303134; -fx-text-fill: #e8eaed; -fx-font-size: 12px; -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 6 10; -fx-border-color: #4a4d52; -fx-border-radius: 6px;");
            securityBtn.setStyle("-fx-background-color: #3e2723; -fx-text-fill: #f28b82; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 6 10; -fx-border-color: #5c2c27; -fx-border-radius: 6px;");
        } else {
            container.setStyle(
                "-fx-background-color: #ffffff; " +
                "-fx-background-radius: 14px; " +
                "-fx-border-radius: 14px; " +
                "-fx-border-color: #dadce0; " +
                "-fx-border-width: 1px;"
            );
            titleLabel.setTextFill(Color.web("#202124"));
            closeBtn.setTextFill(Color.web("#5f6368"));
            for (Label l : sectionHeaders) l.setTextFill(Color.web("#1a73e8"));
            for (Label l : itemLabels) l.setTextFill(Color.web("#202124"));
            for (Label l : hintLabels) l.setTextFill(Color.web("#70757a"));

            errorBtn.setStyle("-fx-background-color: #f1f3f4; -fx-text-fill: #3c4043; -fx-font-size: 12px; -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 6 10;");
            securityBtn.setStyle("-fx-background-color: #fce8e6; -fx-text-fill: #d93025; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 6 10;");
        }
        updateButtonStates();
    }

    public void setOnClose(Runnable onCloseCallback) {
        this.onCloseCallback = onCloseCallback;
    }

    public boolean isVisible() {
        return container.isVisible();
    }

    public void toggle() {
        if (container.isVisible()) {
            hide();
        } else {
            show();
        }
    }

    public void show() {
        if (isAnimating) return;
        applyTheme(settings.isEffectiveDarkMode());
        container.setVisible(true);
        container.setManaged(true);
        container.toFront();

        isAnimating = true;
        container.setOpacity(0.0);
        container.setTranslateY(-10.0);

        FadeTransition fade = new FadeTransition(Duration.millis(180), container);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        TranslateTransition translate = new TranslateTransition(Duration.millis(180), container);
        translate.setFromY(-10.0);
        translate.setToY(0.0);

        ParallelTransition anim = new ParallelTransition(fade, translate);
        anim.setOnFinished(e -> isAnimating = false);
        anim.play();
    }

    public void hide() {
        if (!container.isVisible() || isAnimating) return;
        isAnimating = true;

        FadeTransition fade = new FadeTransition(Duration.millis(140), container);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);

        TranslateTransition translate = new TranslateTransition(Duration.millis(140), container);
        translate.setFromY(0.0);
        translate.setToY(-8.0);

        ParallelTransition anim = new ParallelTransition(fade, translate);
        anim.setOnFinished(e -> {
            container.setVisible(false);
            container.setManaged(false);
            isAnimating = false;
            if (onCloseCallback != null) {
                onCloseCallback.run();
            }
        });
        anim.play();
    }

    public VBox getContainer() {
        return container;
    }
}
