package com.ruet.campusmap;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

/**
 * Floating bottom-center map navigation controls:
 * - Zoom In (+) and Zoom Out (-) grouped side-by-side in a horizontal card
 * - My Location target icon in a floating card
 *
 * Placed at bottom-center (Pos.BOTTOM_CENTER).
 * No setOnClick used (handles interactions via setOnMousePressed).
 */
public class MapBottomControls {

    private final HBox container;

    public MapBottomControls(Group mapGroup) {
        // ================= 1. ZOOM IN BUTTON (+) =================
        SVGPath zoomInIcon = new SVGPath();
        zoomInIcon.setContent("M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z");
        zoomInIcon.setFill(Color.web("#5f6368"));
        zoomInIcon.setScaleX(0.85);
        zoomInIcon.setScaleY(0.85);

        StackPane zoomInBtn = new StackPane(zoomInIcon);
        zoomInBtn.setPrefSize(40, 40);
        zoomInBtn.setMinSize(40, 40);
        zoomInBtn.setMaxSize(40, 40);
        zoomInBtn.setStyle("-fx-cursor: hand; -fx-background-color: transparent;");
        Tooltip.install(zoomInBtn, new Tooltip("Zoom In"));

        // Zoom In action (via mouse pressed, no setOnClick)
        zoomInBtn.setOnMousePressed(e -> {
            double newScale = mapGroup.getScaleX() * 1.2;
            if (newScale <= 4.0) {
                mapGroup.setScaleX(newScale);
                mapGroup.setScaleY(newScale);
            }
        });

        // Vertical divider between + and - side-by-side
        Separator zoomDivider = new Separator(Orientation.VERTICAL);
        zoomDivider.setStyle("-fx-opacity: 0.35;");
        zoomDivider.setMaxHeight(26);

        // ================= 2. ZOOM OUT BUTTON (-) =================
        SVGPath zoomOutIcon = new SVGPath();
        zoomOutIcon.setContent("M19 13H5v-2h14v2z");
        zoomOutIcon.setFill(Color.web("#5f6368"));
        zoomOutIcon.setScaleX(0.85);
        zoomOutIcon.setScaleY(0.85);

        StackPane zoomOutBtn = new StackPane(zoomOutIcon);
        zoomOutBtn.setPrefSize(40, 40);
        zoomOutBtn.setMinSize(40, 40);
        zoomOutBtn.setMaxSize(40, 40);
        zoomOutBtn.setStyle("-fx-cursor: hand; -fx-background-color: transparent;");
        Tooltip.install(zoomOutBtn, new Tooltip("Zoom Out"));

        // Zoom Out action (via mouse pressed, no setOnClick)
        zoomOutBtn.setOnMousePressed(e -> {
            double newScale = mapGroup.getScaleX() * 0.8;
            if (newScale >= 0.5) {
                mapGroup.setScaleX(newScale);
                mapGroup.setScaleY(newScale);
            }
        });

        // Combined Zoom Card: Side-by-side (Horizontal HBox)
        HBox zoomCard = new HBox(zoomInBtn, zoomDivider, zoomOutBtn);
        zoomCard.setAlignment(Pos.CENTER);
        zoomCard.setPrefHeight(42);
        zoomCard.setStyle(
            "-fx-background-color: #ffffff; " +
            "-fx-background-radius: 8px; " +
            "-fx-border-radius: 8px; " +
            "-fx-border-color: #dadce0; " +
            "-fx-border-width: 1px;"
        );
        zoomCard.setEffect(new DropShadow(10, 0, 3, Color.rgb(60, 64, 67, 0.25)));

        // ================= 3. MY LOCATION ICON =================
        // Google Maps crosshair / current location target icon
        SVGPath locationIcon = new SVGPath();
        locationIcon.setContent("M12 8c-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4-1.79-4-4-4zm8.94 3c-.46-4.17-3.77-7.48-7.94-7.94V1h-2v2.06C6.83 3.52 3.52 6.83 3.06 11H1v2h2.06c.46 4.17 3.77 7.48 7.94 7.94V23h2v-2.06c4.17-.46 7.48-3.77 7.94-7.94H23v-2h-2.06zM12 19c-3.87 0-7-3.13-7-7s3.13-7 7-7 7 3.13 7 7-3.13 7-7 7z");
        locationIcon.setFill(Color.web("#1a73e8")); // Google Blue
        locationIcon.setScaleX(0.95);
        locationIcon.setScaleY(0.95);

        StackPane locationBtn = new StackPane(locationIcon);
        locationBtn.setPrefSize(42, 42);
        locationBtn.setMinSize(42, 42);
        locationBtn.setMaxSize(42, 42);
        locationBtn.setStyle(
            "-fx-background-color: #ffffff; " +
            "-fx-background-radius: 8px; " +
            "-fx-border-radius: 8px; " +
            "-fx-border-color: #dadce0; " +
            "-fx-border-width: 1px; " +
            "-fx-cursor: hand;"
        );
        locationBtn.setEffect(new DropShadow(10, 0, 3, Color.rgb(60, 64, 67, 0.25)));
        Tooltip.install(locationBtn, new Tooltip("My Location"));

        // ================= 4. MAIN BOTTOM-CENTER CONTAINER =================
        // Arranged side-by-side: [My Location] [ + | - ]
        container = new HBox(12, locationBtn, zoomCard);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(0, 0, 24, 0)); // 24px bottom margin
        container.setMaxSize(HBox.USE_PREF_SIZE, HBox.USE_PREF_SIZE);

        // Prevent clicking or dragging buttons from panning map underneath
        container.setOnMousePressed(javafx.event.Event::consume);
        container.setOnMouseDragged(javafx.event.Event::consume);

        // Position floating at bottom-center
        StackPane.setAlignment(container, Pos.BOTTOM_CENTER);
    }

    public HBox getContainer() {
        return container;
    }
}
