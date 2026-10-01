package com.ruet.campusmap.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ruet.campusmap.model.BuildingPolygon;
import com.ruet.campusmap.view.BuildingHoverTooltip;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

import javafx.scene.effect.DropShadow;
import javafx.scene.shape.StrokeLineJoin;
import javafx.util.Duration;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to load polygon definitions from JSON and convert them to JavaFX Polygons.
 */
public class PolygonDataLoader {

    private static final String JSON_RESOURCE_PATH = "/data/campus.json";
    private static final String JSON_FILE_PATH = "src/main/resources/data/campus.json";

    /**
     * Loads the list of BuildingPolygon data models from campus.json.
     * Prefers the local disk file (for immediately saved edits), and falls back to classpath resource.
     */
    public static List<BuildingPolygon> loadBuildingPolygons() {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<BuildingPolygon>>() {}.getType();

        // 1. Try reading from working directory src/main/resources first (reflects new saves immediately)
        File file = new File(JSON_FILE_PATH);
        if (file.exists() && file.length() > 0) {
            try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                List<BuildingPolygon> result = gson.fromJson(reader, listType);
                if (result != null) {
                    return result;
                }
            } catch (Exception ignored) {
                // Fallback to classpath
            }
        }

        // 2. Fallback: Read from classpath resources
        try (InputStream is = PolygonDataLoader.class.getResourceAsStream(JSON_RESOURCE_PATH)) {
            if (is != null) {
                try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    List<BuildingPolygon> result = gson.fromJson(reader, listType);
                    if (result != null) {
                        return result;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Could not load polygons from JSON: " + e.getMessage());
        }

        return new ArrayList<>();
    }

    /**
     * Converts a BuildingPolygon data model into an interactive JavaFX Polygon.
     */
    public static Polygon createJavaFXPolygon(BuildingPolygon model) {
        return createJavaFXPolygon(model, (java.util.function.BiConsumer<BuildingPolygon, Polygon>) null);
    }

    /**
     * Converts a BuildingPolygon data model into an interactive JavaFX Polygon with custom click callback.
     */
    public static Polygon createJavaFXPolygon(BuildingPolygon model, java.util.function.Consumer<BuildingPolygon> onClick) {
        return createJavaFXPolygon(model, (bp, poly) -> {
            if (onClick != null) {
                onClick.accept(bp);
            }
        });
    }

    /**
     * Converts a BuildingPolygon data model into an interactive JavaFX Polygon with (model, polygon) callback.
     * Defaults to user mode (invisible boxes).
     */
    public static Polygon createJavaFXPolygon(BuildingPolygon model, java.util.function.BiConsumer<BuildingPolygon, Polygon> onPolygonClick) {
        return createJavaFXPolygon(model, onPolygonClick, false);
    }

    /**
     * Converts a BuildingPolygon data model into an interactive JavaFX Polygon with (model, polygon) callback
     * and explicit admin mode state.
     */
    public static Polygon createJavaFXPolygon(BuildingPolygon model, java.util.function.BiConsumer<BuildingPolygon, Polygon> onPolygonClick, boolean isAdminMode) {
        Polygon polygon = new Polygon();
        polygon.setUserData(model);

        // Flatten points from List<double[]> to JavaFX ObservableList<Double>
        if (model.getPoints() != null) {
            for (double[] point : model.getPoints()) {
                if (point.length >= 2) {
                    polygon.getPoints().addAll(point[0], point[1]);
                }
            }
        }

        // Apply initial visual style (invisible for users by default, visible for admin)
        applyPolygonStyle(polygon, model, isAdminMode);

        // Hover feedback: gorgeous semi-transparent tinted glow effect on building hitboxes
        polygon.setOnMouseEntered(e -> {
            boolean currentAdmin = Boolean.TRUE.equals(polygon.getProperties().get("adminMode"));
            String color = (model != null && model.getColor() != null && !model.getColor().isEmpty())
                ? model.getColor()
                : "#1a73e8"; // Modern Google Blue
            Color baseColor = Color.web(color);

            if (currentAdmin) {
                // Admin hover feedback: high-contrast highlight
                polygon.setFill(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 0.75));
                polygon.setStroke(baseColor.brighter());
                polygon.setStrokeWidth(3.0);
                polygon.setEffect(new DropShadow(16, 0, 2, new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 0.60)));
            } else {
                // User hover effect: elegant semi-transparent tinted glass overlay with ambient glow
                Color hoverFill = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 0.35);
                Color hoverStroke = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 0.90);
                polygon.setFill(hoverFill);
                polygon.setStroke(hoverStroke);
                polygon.setStrokeWidth(2.5);
                polygon.setStrokeLineJoin(StrokeLineJoin.ROUND);
                polygon.setEffect(new DropShadow(14, 0, 2, new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 0.50)));
            }
            polygon.setCursor(javafx.scene.Cursor.HAND);

            if (model != null && model.getName() != null && !model.getName().trim().isEmpty()) {
                BuildingHoverTooltip.getInstance().show(model.getName(), e.getSceneX(), e.getSceneY());
            }
        });

        polygon.setOnMouseMoved(e -> {
            BuildingHoverTooltip.getInstance().updatePosition(e.getSceneX(), e.getSceneY());
        });

        polygon.setOnMouseExited(e -> {
            boolean currentAdmin = Boolean.TRUE.equals(polygon.getProperties().get("adminMode"));
            applyPolygonStyle(polygon, model, currentAdmin);
            polygon.setCursor(javafx.scene.Cursor.DEFAULT);
            BuildingHoverTooltip.getInstance().hide();
        });

        if (onPolygonClick != null) {
            polygon.setOnMouseClicked(e -> {
                if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                    onPolygonClick.accept(model, polygon);
                }
            });
        }

        return polygon;
    }

    /**
     * Installs a fast, beautifully styled tooltip on a building polygon.
     */
    public static void installBuildingTooltip(Polygon polygon, String buildingName) {
        if (buildingName == null || buildingName.trim().isEmpty()) return;
        Tooltip tooltip = new Tooltip(buildingName);
        tooltip.setShowDelay(Duration.millis(60));
        tooltip.setShowDuration(Duration.seconds(10));
        tooltip.setHideDelay(Duration.millis(80));
        tooltip.setStyle(
            "-fx-background-color: rgba(24, 27, 34, 0.95); " +
            "-fx-text-fill: #ffffff; " +
            "-fx-font-family: 'Segoe UI', 'Roboto', sans-serif; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 6px 12px; " +
            "-fx-background-radius: 8px; " +
            "-fx-border-color: rgba(255, 255, 255, 0.20); " +
            "-fx-border-radius: 8px; " +
            "-fx-border-width: 1px; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 3);"
        );
        Tooltip.install(polygon, tooltip);
    }

    /**
     * Updates the fill and stroke of a JavaFX polygon according to admin/user mode and model visibility.
     */
    public static void applyPolygonStyle(Polygon polygon, BuildingPolygon model, boolean isAdminMode) {
        polygon.getProperties().put("adminMode", isAdminMode);
        if (isAdminMode) {
            String hexColor = (model != null && model.getColor() != null && !model.getColor().isEmpty())
                ? model.getColor()
                : "#3498DB";
            Color baseColor = Color.web(hexColor);
            Color fillColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 0.45);
            polygon.setFill(fillColor);
            polygon.setStroke(baseColor);
            polygon.setStrokeWidth(2.0);
            polygon.setEffect(null);
        } else {
            // User mode: box is NOT visible to users unless explicitly configured
            if (model != null && model.isVisibleToUsers()) {
                String hexColor = model.getColor() != null && !model.getColor().isEmpty() ? model.getColor() : "#3498DB";
                Color baseColor = Color.web(hexColor);
                Color fillColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 0.45);
                polygon.setFill(fillColor);
                polygon.setStroke(baseColor);
                polygon.setStrokeWidth(2.0);
                polygon.setEffect(null);
            } else {
                // Invisible hitbox for users: clicks and hover still register, but no persistent visual box
                polygon.setFill(Color.TRANSPARENT);
                polygon.setStroke(Color.TRANSPARENT);
                polygon.setStrokeWidth(0.0);
                polygon.setEffect(null);
            }
        }
    }

    /**
     * Updates the fill and stroke of a JavaFX polygon according to a hex color (backwards compatible).
     */
    public static void applyPolygonStyle(Polygon polygon, String hexColor) {
        Color baseColor = Color.web(hexColor != null && !hexColor.isEmpty() ? hexColor : "#3498DB");
        Color fillColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 0.45);
        polygon.setFill(fillColor);
        polygon.setStroke(baseColor);
        polygon.setStrokeWidth(2.0);
    }
}
