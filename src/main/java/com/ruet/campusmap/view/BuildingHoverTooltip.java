package com.ruet.campusmap.view;

import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * High-performance, in-scene floating building tooltip.
 * Completely mouse-transparent to guarantee zero cursor interception,
 * zero focus stealing, and zero hover blinking/flickering.
 * Automatically autosizes to text length to prevent any three-dot (...) truncation.
 */
public class BuildingHoverTooltip {

    private static BuildingHoverTooltip instance;

    private final Label tooltipLabel;
    private Pane parentPane;
    private FadeTransition fadeTransition;
    private boolean isDark = false;

    public BuildingHoverTooltip() {
        tooltipLabel = new Label();
        tooltipLabel.setAlignment(Pos.CENTER);

        // Prevent any text truncation or ellipsis (...)
        tooltipLabel.setTextOverrun(OverrunStyle.CLIP);
        tooltipLabel.setWrapText(false);
        tooltipLabel.setMinWidth(Label.USE_PREF_SIZE);
        tooltipLabel.setMinHeight(Label.USE_PREF_SIZE);

        // CRITICAL: Mouse transparent so it never steals cursor or causes MOUSE_EXITED on polygons
        tooltipLabel.setMouseTransparent(true);
        tooltipLabel.setManaged(false);
        tooltipLabel.setVisible(false);
        tooltipLabel.setOpacity(0.0);

        applyTheme(false);
    }

    public static BuildingHoverTooltip getInstance() {
        if (instance == null) {
            instance = new BuildingHoverTooltip();
        }
        return instance;
    }

    public void attachTo(Pane parent) {
        this.parentPane = parent;
        if (!parent.getChildren().contains(tooltipLabel)) {
            parent.getChildren().add(tooltipLabel);
        }
    }

    public void applyTheme(boolean isDark) {
        this.isDark = isDark;
        if (isDark) {
            tooltipLabel.setStyle(
                "-fx-background-color: rgba(24, 27, 34, 0.95); " +
                "-fx-text-fill: #ffffff; " +
                "-fx-font-family: 'Segoe UI', 'Roboto', sans-serif; " +
                "-fx-font-size: 13px; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 6px 14px; " +
                "-fx-background-radius: 8px; " +
                "-fx-border-color: rgba(255, 255, 255, 0.22); " +
                "-fx-border-radius: 8px; " +
                "-fx-border-width: 1px;"
            );
            tooltipLabel.setEffect(new DropShadow(12, 0, 3, Color.rgb(0, 0, 0, 0.55)));
        } else {
            tooltipLabel.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.96); " +
                "-fx-text-fill: #202124; " +
                "-fx-font-family: 'Segoe UI', 'Roboto', sans-serif; " +
                "-fx-font-size: 13px; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 6px 14px; " +
                "-fx-background-radius: 8px; " +
                "-fx-border-color: rgba(0, 0, 0, 0.12); " +
                "-fx-border-radius: 8px; " +
                "-fx-border-width: 1px;"
            );
            tooltipLabel.setEffect(new DropShadow(12, 0, 3, Color.rgb(60, 64, 67, 0.30)));
        }
        tooltipLabel.applyCss();
        tooltipLabel.autosize();
    }

    public void show(String buildingName, double sceneX, double sceneY) {
        if (buildingName == null || buildingName.trim().isEmpty() || parentPane == null) {
            hide();
            return;
        }

        tooltipLabel.setText(buildingName.trim());
        tooltipLabel.applyCss();
        tooltipLabel.autosize();

        // Ensure width is at least the full preferred text size
        double prefW = tooltipLabel.prefWidth(-1);
        double prefH = tooltipLabel.prefHeight(-1);
        if (prefW > 0 && prefH > 0) {
            tooltipLabel.resize(prefW, prefH);
        }

        tooltipLabel.setVisible(true);

        updatePosition(sceneX, sceneY);

        if (fadeTransition != null) {
            fadeTransition.stop();
        }
        fadeTransition = new FadeTransition(Duration.millis(90), tooltipLabel);
        fadeTransition.setFromValue(tooltipLabel.getOpacity());
        fadeTransition.setToValue(1.0);
        fadeTransition.play();
    }

    public void updatePosition(double sceneX, double sceneY) {
        if (parentPane == null || !tooltipLabel.isVisible()) return;

        double tipW = tooltipLabel.getWidth();
        if (tipW <= 0) {
            tipW = tooltipLabel.prefWidth(-1);
        }
        if (tipW <= 0) {
            tipW = tooltipLabel.getText().length() * 8.5 + 28;
        }
        double tipH = tooltipLabel.getHeight();
        if (tipH <= 0) {
            tipH = tooltipLabel.prefHeight(-1);
        }
        if (tipH <= 0) {
            tipH = 34.0;
        }

        double targetX = sceneX + 14.0;
        double targetY = sceneY - tipH - 10.0;

        // Prevent overflow beyond right or top edge of window
        if (parentPane != null && targetX + tipW > parentPane.getWidth() - 12.0) {
            targetX = sceneX - tipW - 14.0;
        }
        if (targetY < 12.0) {
            targetY = sceneY + 22.0; // Display below cursor if too close to top
        }

        tooltipLabel.setLayoutX(Math.max(10.0, targetX));
        tooltipLabel.setLayoutY(Math.max(10.0, targetY));
    }

    public void hide() {
        if (!tooltipLabel.isVisible()) return;

        if (fadeTransition != null) {
            fadeTransition.stop();
        }
        fadeTransition = new FadeTransition(Duration.millis(80), tooltipLabel);
        fadeTransition.setFromValue(tooltipLabel.getOpacity());
        fadeTransition.setToValue(0.0);
        fadeTransition.setOnFinished(e -> tooltipLabel.setVisible(false));
        fadeTransition.play();
    }

    public Label getContainer() {
        return tooltipLabel;
    }
}
