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
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Layer that renders floating text labels/badges over campus buildings.
 * Displays user-defined short codes or full building names without fake auto-generated codes.
 */
public class BuildingLabelsLayer {

    private final Pane container;
    private final List<LabelEntry> entries = new ArrayList<>();
    private final AppSettings settings;
    private final Consumer<String> onLabelClick;

    private static class LabelEntry {
        final StackPane badge;
        final Label label;
        String fullName;
        String code;
        final Tooltip tooltip;
        final BuildingPolygon polygon;

        LabelEntry(StackPane badge, Label label, String fullName, String code, Tooltip tooltip, BuildingPolygon polygon) {
            this.badge = badge;
            this.label = label;
            this.fullName = fullName;
            this.code = code;
            this.tooltip = tooltip;
            this.polygon = polygon;
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
                // Only add if not already registered via polygon
                boolean exists = entries.stream().anyMatch(e -> e.fullName.equalsIgnoreCase(name));
                if (!exists) {
                    addBuildingLabel(name, "", coords[0], coords[1], null);
                }
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
            if (e.polygon == bp || e.fullName.equalsIgnoreCase(bp.getName())) {
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
            String code = (bp.getCodeName() != null && !bp.getCodeName().isBlank()) ? bp.getCodeName().trim() : "";
            addBuildingLabel(bp.getName(), code, sumX / count, sumY / count, bp);
            refresh();
        }
    }

    private void addBuildingLabel(String fullName, String code, double centerX, double centerY, BuildingPolygon bp) {
        String displayCode = (code != null && !code.isBlank()) ? code : fullName;

        Label label = new Label(displayCode);
        label.setStyle("-fx-font-family: 'Segoe UI', Roboto, sans-serif; -fx-font-weight: bold; -fx-font-size: 11px;");

        StackPane badge = new StackPane(label);
        badge.setPadding(new Insets(3, 8, 3, 8));
        badge.setAlignment(Pos.CENTER);
        badge.setEffect(new DropShadow(4, 0, 2, Color.rgb(0, 0, 0, 0.25)));
        badge.setStyle("-fx-cursor: hand;");

        String tipText = (code != null && !code.isBlank()) ? (fullName + " (" + code + ")") : fullName;
        Tooltip tooltip = new Tooltip(tipText);
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

        LabelEntry entry = new LabelEntry(badge, label, fullName, code, tooltip, bp);
        entries.add(entry);
        container.getChildren().add(badge);
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
                // If code is not set, fallback to full name instead of fake letters
                String codeText = (e.code != null && !e.code.isBlank()) ? e.code : e.fullName;
                e.label.setText(codeText);
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
