package com.ruet.campusmap;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

/**
 * Floating control buttons (Settings and Admin) styled like Google Maps controls.
 * Floating on the top-right corner of the map viewport.
 */
public class MapActionButtons {

    private final HBox container;
    private final StackPane settingsBtn;
    private final StackPane adminBtn;
    private final SVGPath settingsIcon;
    private final SVGPath adminIcon;

    private boolean isSettingsActive = false;
    private boolean isDark = false;

    public MapActionButtons() {
        // --- 1. Settings Icon Button ---
        // Material Design Settings Gear icon path
        settingsIcon = new SVGPath();
        settingsIcon.setContent("M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z");
        settingsIcon.setFill(Color.web("#5f6368"));
        settingsIcon.setScaleX(0.9);
        settingsIcon.setScaleY(0.9);

        settingsBtn = createCircularButton(settingsIcon, "Settings");

        // --- 2. Admin Icon Button ---
        // Material Design Admin / Supervisor Account icon path
        adminIcon = new SVGPath();
        adminIcon.setContent("M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z");
        adminIcon.setFill(Color.web("#1a73e8")); // Google Blue accent
        adminIcon.setScaleX(1.0);
        adminIcon.setScaleY(1.0);

        adminBtn = createCircularButton(adminIcon, "Admin Panel");

        // --- 3. Container setup (aligned top-right) ---
        container = new HBox(12, settingsBtn, adminBtn);
        container.setAlignment(Pos.CENTER_RIGHT);
        container.setPadding(new Insets(18, 18, 0, 0)); // Top & Right margins
        container.setMaxSize(HBox.USE_PREF_SIZE, HBox.USE_PREF_SIZE);

        // Prevent dragging buttons from panning map underneath
        container.setOnMousePressed(javafx.event.Event::consume);
        container.setOnMouseDragged(javafx.event.Event::consume);

        // Position floating at top-right
        StackPane.setAlignment(container, Pos.TOP_RIGHT);

        refreshStyles();
    }

    /**
     * Creates a Google Maps style circular floating card button.
     */
    private StackPane createCircularButton(SVGPath icon, String tooltipText) {
        StackPane button = new StackPane(icon);
        button.setPrefSize(44, 44);
        button.setMinSize(44, 44);
        button.setMaxSize(44, 44);
        button.setEffect(new DropShadow(10, 0, 3, Color.rgb(60, 64, 67, 0.25)));

        Tooltip tooltip = new Tooltip(tooltipText);
        Tooltip.install(button, tooltip);

        button.setOnMouseEntered(e -> {
            if (button == settingsBtn && isSettingsActive) {
                return; // Keep active style
            }
            if (isDark) {
                button.setStyle(
                    "-fx-background-color: #35373a; " +
                    "-fx-background-radius: 50%; " +
                    "-fx-border-radius: 50%; " +
                    "-fx-border-color: #5f6368; " +
                    "-fx-border-width: 1px; " +
                    "-fx-cursor: hand;"
                );
            } else {
                button.setStyle(
                    "-fx-background-color: #f8f9fa; " +
                    "-fx-background-radius: 50%; " +
                    "-fx-border-radius: 50%; " +
                    "-fx-border-color: #c4c7c5; " +
                    "-fx-border-width: 1px; " +
                    "-fx-cursor: hand;"
                );
            }
        });

        button.setOnMouseExited(e -> {
            refreshButton(button);
        });

        return button;
    }

    private void refreshButton(StackPane button) {
        if (button == settingsBtn && isSettingsActive) {
            button.setStyle(
                "-fx-background-color: #1a73e8; " +
                "-fx-background-radius: 50%; " +
                "-fx-border-radius: 50%; " +
                "-fx-border-color: #1a73e8; " +
                "-fx-border-width: 1px; " +
                "-fx-cursor: hand;"
            );
            settingsIcon.setFill(Color.WHITE);
            return;
        }

        if (isDark) {
            button.setStyle(
                "-fx-background-color: #202124; " +
                "-fx-background-radius: 50%; " +
                "-fx-border-radius: 50%; " +
                "-fx-border-color: #3c4043; " +
                "-fx-border-width: 1px; " +
                "-fx-cursor: hand;"
            );
            if (button == settingsBtn) {
                settingsIcon.setFill(Color.web("#bdc1c6"));
            } else {
                adminIcon.setFill(Color.web("#8ab4f8"));
            }
        } else {
            button.setStyle(
                "-fx-background-color: #ffffff; " +
                "-fx-background-radius: 50%; " +
                "-fx-border-radius: 50%; " +
                "-fx-border-color: #dadce0; " +
                "-fx-border-width: 1px; " +
                "-fx-cursor: hand;"
            );
            if (button == settingsBtn) {
                settingsIcon.setFill(Color.web("#5f6368"));
            } else {
                adminIcon.setFill(Color.web("#1a73e8"));
            }
        }
    }

    private void refreshStyles() {
        refreshButton(settingsBtn);
        refreshButton(adminBtn);
    }

    public void setOnSettingsAction(Runnable action) {
        settingsBtn.setOnMouseClicked(e -> {
            if (action != null) {
                action.run();
            }
        });
    }

    public void setSettingsActive(boolean active) {
        this.isSettingsActive = active;
        refreshButton(settingsBtn);
    }

    public void applyTheme(boolean isDark) {
        this.isDark = isDark;
        refreshStyles();
    }

    public void setOnAdminAction(Runnable action) {
        adminBtn.setOnMouseClicked(e -> {
            if (action != null) {
                action.run();
            }
        });
    }

    public HBox getContainer() {
        return container;
    }
}
