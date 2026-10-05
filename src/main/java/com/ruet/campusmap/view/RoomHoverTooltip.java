package com.ruet.campusmap.view;

import com.ruet.campusmap.model.RoomLabel;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * Modern floating glassmorphic tooltip card for inner map rooms.
 * Completely mouse-transparent to guarantee zero cursor interception,
 * zero hover flickering, and uninterrupted panning/zooming.
 */
public class RoomHoverTooltip {

    private final VBox card;
    private final Circle typeDot;
    private final Label roomNumberPill;
    private final Label roomTypeLabel;
    private final Label roomNameLabel;
    private final Label occupantsLabel;
    private final Label descLabel;

    private Pane parentPane;
    private FadeTransition fadeTransition;
    private boolean isDark = false;
    private RoomLabel currentRoom = null;

    public RoomHoverTooltip() {
        card = new VBox(3);
        card.setMouseTransparent(true);
        card.setManaged(false);
        card.setVisible(false);
        card.setOpacity(0.0);
        card.setMaxWidth(340);

        typeDot = new Circle(4);
        typeDot.setStroke(Color.WHITE);
        typeDot.setStrokeWidth(1.0);

        roomNumberPill = new Label();
        roomNumberPill.setStyle(
            "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 1 6; -fx-background-radius: 6px;"
        );

        roomTypeLabel = new Label();
        roomTypeLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 500;");

        HBox topRow = new HBox(6, typeDot, roomNumberPill, roomTypeLabel);
        topRow.setAlignment(Pos.CENTER_LEFT);

        roomNameLabel = new Label();
        roomNameLabel.setStyle(
            "-fx-font-size: 13px; -fx-font-weight: bold; -fx-font-family: 'Segoe UI', Roboto, sans-serif;"
        );
        roomNameLabel.setWrapText(true);

        occupantsLabel = new Label();
        occupantsLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-font-family: 'Segoe UI', Roboto, sans-serif;");
        occupantsLabel.setWrapText(true);
        occupantsLabel.setVisible(false);
        occupantsLabel.setManaged(false);

        descLabel = new Label();
        descLabel.setStyle("-fx-font-size: 11px; -fx-font-family: 'Segoe UI', Roboto, sans-serif;");
        descLabel.setWrapText(true);
        descLabel.setVisible(false);
        descLabel.setManaged(false);

        card.getChildren().addAll(topRow, roomNameLabel, occupantsLabel, descLabel);
        applyTheme(false);
    }

    public void attachTo(Pane parent) {
        this.parentPane = parent;
        if (!parent.getChildren().contains(card)) {
            parent.getChildren().add(card);
        }
    }

    public VBox getCard() {
        return card;
    }

    public void applyTheme(boolean isDark) {
        this.isDark = isDark;
        if (isDark) {
            card.setStyle(
                "-fx-background-color: rgba(32, 33, 36, 0.96); " +
                "-fx-background-radius: 10px; " +
                "-fx-border-color: rgba(255, 255, 255, 0.16); " +
                "-fx-border-radius: 10px; " +
                "-fx-border-width: 1px; " +
                "-fx-padding: 8 14 9 14;"
            );
            card.setEffect(new DropShadow(14, 0, 4, Color.rgb(0, 0, 0, 0.55)));
            roomNameLabel.setTextFill(Color.web("#e8eaed"));
            roomTypeLabel.setTextFill(Color.web("#9aa0a6"));
            descLabel.setTextFill(Color.web("#9aa0a6"));
            occupantsLabel.setTextFill(Color.web("#8ab4f8"));
        } else {
            card.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.98); " +
                "-fx-background-radius: 10px; " +
                "-fx-border-color: rgba(0, 0, 0, 0.14); " +
                "-fx-border-radius: 10px; " +
                "-fx-border-width: 1px; " +
                "-fx-padding: 8 14 9 14;"
            );
            card.setEffect(new DropShadow(14, 0, 4, Color.rgb(60, 64, 67, 0.25)));
            roomNameLabel.setTextFill(Color.web("#202124"));
            roomTypeLabel.setTextFill(Color.web("#5f6368"));
            descLabel.setTextFill(Color.web("#5f6368"));
            occupantsLabel.setTextFill(Color.web("#1a73e8"));
        }
    }

    public void show(RoomLabel room, double sceneX, double sceneY) {
        if (room == null || parentPane == null) {
            hide();
            return;
        }

        this.currentRoom = room;

        // 1. Room Number
        String num = room.getDisplayTitle();
        roomNumberPill.setText(num.isEmpty() ? "Room" : num);
        String typeColor = colorForType(room.getType());
        typeDot.setFill(Color.web(typeColor));
        roomNumberPill.setStyle(
            "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 1 7; -fx-background-radius: 6px; " +
            "-fx-background-color: " + (isDark ? "rgba(255,255,255,0.12);" : "rgba(0,0,0,0.06);") +
            "-fx-text-fill: " + (isDark ? "#e8eaed;" : "#202124;")
        );

        // 2. Room Type
        String typeStr = (room.getType() != null && !room.getType().isBlank()) ? room.getType() : "";
        roomTypeLabel.setText(typeStr);
        roomTypeLabel.setVisible(!typeStr.isEmpty());
        roomTypeLabel.setManaged(!typeStr.isEmpty());

        // 3. Room Name (shown prominently)
        String name = (room.getName() != null && !room.getName().isBlank()) ? room.getName().trim() : ("Room " + num);
        roomNameLabel.setText(name);

        // 4. Occupants (teachers)
        if (!room.getOccupants().isEmpty()) {
            occupantsLabel.setText("👤 " + String.join(", ", room.getOccupants()));
            occupantsLabel.setVisible(true);
            occupantsLabel.setManaged(true);
        } else {
            occupantsLabel.setVisible(false);
            occupantsLabel.setManaged(false);
        }

        // 5. Description
        if (room.getDescription() != null && !room.getDescription().isBlank()) {
            descLabel.setText(room.getDescription());
            descLabel.setVisible(true);
            descLabel.setManaged(true);
        } else {
            descLabel.setVisible(false);
            descLabel.setManaged(false);
        }

        card.applyCss();
        card.autosize();

        positionTooltip(sceneX, sceneY);

        if (!card.isVisible() || card.getOpacity() < 0.95) {
            card.setVisible(true);
            if (fadeTransition != null) fadeTransition.stop();
            fadeTransition = new FadeTransition(Duration.millis(110), card);
            fadeTransition.setFromValue(card.getOpacity());
            fadeTransition.setToValue(1.0);
            fadeTransition.setInterpolator(Interpolator.EASE_OUT);
            fadeTransition.play();
        }
    }

    public void updatePosition(double sceneX, double sceneY) {
        if (!card.isVisible()) return;
        positionTooltip(sceneX, sceneY);
    }

    private void positionTooltip(double sceneX, double sceneY) {
        if (parentPane == null) return;

        double w = card.getWidth() > 0 ? card.getWidth() : card.prefWidth(-1);
        double h = card.getHeight() > 0 ? card.getHeight() : card.prefHeight(-1);

        double parentW = parentPane.getWidth() > 0 ? parentPane.getWidth() : 1200;
        double parentH = parentPane.getHeight() > 0 ? parentPane.getHeight() : 800;

        // Position slightly above and to the right of cursor
        double targetX = sceneX + 12;
        double targetY = sceneY - h - 14;

        // If too close to the top, position below cursor instead
        if (targetY < 85) {
            targetY = sceneY + 22;
        }

        // Keep within horizontal boundaries
        if (targetX + w > parentW - 20) {
            targetX = Math.max(20, sceneX - w - 12);
        }
        if (targetX < 20) {
            targetX = 20;
        }

        // Keep within vertical boundaries
        if (targetY + h > parentH - 20) {
            targetY = parentH - h - 20;
        }

        card.relocate(targetX, targetY);
    }

    public void hide() {
        if (!card.isVisible() && card.getOpacity() <= 0.0) return;
        currentRoom = null;
        if (fadeTransition != null) fadeTransition.stop();

        fadeTransition = new FadeTransition(Duration.millis(90), card);
        fadeTransition.setFromValue(card.getOpacity());
        fadeTransition.setToValue(0.0);
        fadeTransition.setInterpolator(Interpolator.EASE_IN);
        fadeTransition.setOnFinished(e -> {
            card.setVisible(false);
            card.setOpacity(0.0);
        });
        fadeTransition.play();
    }

    public boolean isShowing() {
        return card.isVisible() && card.getOpacity() > 0.0;
    }

    private static String colorForType(String type) {
        if (type == null) return "#5f6368";
        return switch (type.toLowerCase()) {
            case "classroom" -> "#1a73e8";
            case "lab" -> "#e37400";
            case "faculty office", "head office" -> "#1e8e3e";
            case "administrative" -> "#00897b";
            case "seminar room", "library" -> "#8e24aa";
            case "washroom", "prayer room" -> "#12b5cb";
            case "stairs", "store" -> "#80868b";
            default -> "#5f6368";
        };
    }
}
