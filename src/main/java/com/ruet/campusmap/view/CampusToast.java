package com.ruet.campusmap.view;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Modern floating pill toast notification (Google Maps / Material 3 style)
 * providing brief visual confirmations when map preferences or settings change.
 */
public class CampusToast {

    private static HBox activeToast = null;

    /**
     * Shows a floating pill toast message anchored at the bottom-center of the root pane.
     *
     * @param rootPane The root StackPane of the application
     * @param message  The toast message text
     * @param isDark   Whether the current theme is dark mode
     */
    public static void show(Pane rootPane, String message, boolean isDark) {
        if (rootPane == null || message == null || message.isBlank()) {
            return;
        }

        // Remove any currently active toast immediately
        if (activeToast != null) {
            rootPane.getChildren().remove(activeToast);
            activeToast = null;
        }

        HBox toast = new HBox(8);
        toast.setAlignment(Pos.CENTER);
        toast.setPadding(new Insets(8, 16, 8, 16));
        toast.setMaxSize(HBox.USE_PREF_SIZE, HBox.USE_PREF_SIZE);
        toast.setMouseTransparent(true); // Allow clicks to pass through

        if (isDark) {
            toast.setStyle(
                "-fx-background-color: rgba(48, 49, 52, 0.95); " +
                "-fx-background-radius: 20px; " +
                "-fx-border-color: #5f6368; " +
                "-fx-border-radius: 20px; " +
                "-fx-border-width: 1px;"
            );
        } else {
            toast.setStyle(
                "-fx-background-color: rgba(32, 33, 36, 0.90); " +
                "-fx-background-radius: 20px;"
            );
        }

        toast.setEffect(new DropShadow(12, 0, 4, Color.rgb(0, 0, 0, 0.35)));

        Label label = new Label(message);
        label.setStyle(
            "-fx-font-family: 'Segoe UI', Roboto, sans-serif; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 600; " +
            "-fx-text-fill: #ffffff;"
        );

        toast.getChildren().add(label);

        // Position bottom-center, floating above bottom controls
        StackPane.setAlignment(toast, Pos.BOTTOM_CENTER);
        StackPane.setMargin(toast, new Insets(0, 0, 80, 0));

        rootPane.getChildren().add(toast);
        activeToast = toast;

        // Animations: Slide up slightly and fade in
        toast.setOpacity(0.0);
        toast.setTranslateY(10.0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(180), toast);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(180), toast);
        slideIn.setFromY(10.0);
        slideIn.setToY(0.0);

        ParallelTransition showAnim = new ParallelTransition(fadeIn, slideIn);

        PauseTransition stay = new PauseTransition(Duration.millis(1600));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(220), toast);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(220), toast);
        slideOut.setFromY(0.0);
        slideOut.setToY(6.0);

        ParallelTransition hideAnim = new ParallelTransition(fadeOut, slideOut);

        showAnim.setOnFinished(e -> stay.play());
        stay.setOnFinished(e -> hideAnim.play());
        hideAnim.setOnFinished(e -> {
            rootPane.getChildren().remove(toast);
            if (activeToast == toast) {
                activeToast = null;
            }
        });

        showAnim.play();
    }
}
