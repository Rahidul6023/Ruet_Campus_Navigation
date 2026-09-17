package com.ruet.campusmap.view;

import com.ruet.campusmap.model.AppSettings;
import com.ruet.campusmap.model.BuildingPolygon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Layer that renders floating text labels/badges over campus buildings.
 * Supports displaying Short Codes (e.g., "CSE", "LIB") or Full Building Names,
 * as well as theme-aware styling (Light vs Dark mode).
 */
public class BuildingLabelsLayer {

    private final Pane container;
    private final List<LabelEntry> entries = new ArrayList<>();
    private final AppSettings settings;
    private final Consumer<String> onLabelClick;

    private static final Map<String, String> CODE_MAPPINGS = Map.of(
        "Central Library", "LIB",
        "CSE Department", "CSE",
        "Auditorium", "AUD",
        "Admin Building", "ADM",
        "Cafeteria", "CAFE",
        "Shahid Shahidul Islam Hall", "SHH",
        "New Building", "NB"
    );

    private static class LabelEntry {
        final StackPane badge;
        final Label label;
        final String fullName;
        final String code;

        LabelEntry(StackPane badge, Label label, String fullName, String code) {
            this.badge = badge;
            this.label = label;
            this.fullName = fullName;
            this.code = code;
        }
    }

    public BuildingLabelsLayer(AppSettings settings, Consumer<String> onLabelClick) {
        this.settings = settings;
        this.onLabelClick = onLabelClick;
        this.container = new Pane();
        this.container.setPickOnBounds(false); // Let clicks pass through empty spaces to polygons/map
    }

    /**
     * Initializes default campus landmark labels based on known coordinates.
     */
    public void initKnownLandmarks(Map<String, double[]> coordinates) {
        if (coordinates == null) return;
        for (Map.Entry<String, double[]> entry : coordinates.entrySet()) {
            String name = entry.getKey();
            double[] coords = entry.getValue();
            if (coords != null && coords.length >= 2) {
                addBuildingLabel(name, coords[0], coords[1]);
            }
        }
        refresh();
    }

    /**
     * Adds a label from a BuildingPolygon (computes centroid).
     */
    public void addPolygonLabel(BuildingPolygon bp) {
        if (bp == null || bp.getName() == null || bp.getPoints() == null || bp.getPoints().isEmpty()) {
            return;
        }
        // Avoid duplicate labels if already exists
        for (LabelEntry e : entries) {
            if (e.fullName.equalsIgnoreCase(bp.getName())) {
                return;
            }
        }
        double sumX = 0;
        double sumY = 0;
        int count = 0;
        for (double[] pt : bp.getPoints()) {
            if (pt.length >= 2) {
                sumX += pt[0];
                sumY += pt[1];
                count++;
            }
        }
        if (count > 0) {
            addBuildingLabel(bp.getName(), sumX / count, sumY / count);
            refresh();
        }
    }

    private void addBuildingLabel(String fullName, double centerX, double centerY) {
        String code = CODE_MAPPINGS.getOrDefault(fullName, generateShortCode(fullName));

        Label label = new Label(code);
        label.setStyle("-fx-font-family: 'Segoe UI', Roboto, sans-serif; -fx-font-weight: bold; -fx-font-size: 11px;");

        StackPane badge = new StackPane(label);
        badge.setPadding(new Insets(3, 8, 3, 8));
        badge.setAlignment(Pos.CENTER);
        badge.setEffect(new DropShadow(4, 0, 2, Color.rgb(0, 0, 0, 0.25)));
        badge.setStyle("-fx-cursor: hand;");

        Tooltip tooltip = new Tooltip(fullName + " (" + code + ")");
        Tooltip.install(badge, tooltip);

        badge.setOnMouseClicked(e -> {
            if (onLabelClick != null) {
                onLabelClick.accept(fullName);
            }
            e.consume();
        });

        // Center on coordinate
        badge.setLayoutX(centerX - 24);
        badge.setLayoutY(centerY - 12);

        LabelEntry entry = new LabelEntry(badge, label, fullName, code);
        entries.add(entry);
        container.getChildren().add(badge);
    }

    private String generateShortCode(String name) {
        if (name == null || name.isBlank()) return "BLD";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(4, parts[0].length())).toUpperCase();
        }
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isBlank()) {
                sb.append(Character.toUpperCase(p.charAt(0)));
            }
        }
        return sb.length() > 4 ? sb.substring(0, 4) : sb.toString();
    }

    /**
     * Refreshes label texts and visibility according to active AppSettings.
     */
    public void refresh() {
        boolean isDark = settings.isEffectiveDarkMode();
        AppSettings.BuildingLabelMode mode = settings.getLabelMode();

        container.setVisible(mode != AppSettings.BuildingLabelMode.NONE);

        for (LabelEntry e : entries) {
            if (mode == AppSettings.BuildingLabelMode.CODE) {
                e.label.setText(e.code);
            } else {
                e.label.setText(e.fullName);
            }

            if (isDark) {
                e.badge.setStyle(
                    "-fx-background-color: rgba(32, 33, 36, 0.88); " +
                    "-fx-background-radius: 12px; " +
                    "-fx-border-color: #8ab4f8; " +
                    "-fx-border-radius: 12px; " +
                    "-fx-border-width: 1px; " +
                    "-fx-cursor: hand;"
                );
                e.label.setTextFill(Color.web("#e8eaed"));
            } else {
                e.badge.setStyle(
                    "-fx-background-color: rgba(255, 255, 255, 0.92); " +
                    "-fx-background-radius: 12px; " +
                    "-fx-border-color: #dadce0; " +
                    "-fx-border-radius: 12px; " +
                    "-fx-border-width: 1px; " +
                    "-fx-cursor: hand;"
                );
                e.label.setTextFill(Color.web("#202124"));
            }
        }
    }

    public Pane getContainer() {
        return container;
    }
}
