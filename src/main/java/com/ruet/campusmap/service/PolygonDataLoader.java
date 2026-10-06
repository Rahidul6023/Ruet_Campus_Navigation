package com.ruet.campusmap.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.io.FileWriter;
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
    private static final String JSON_TARGET_PATH = "target/classes/data/campus.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<BuildingPolygon>>() {}.getType();

    /**
     * Loads the list of BuildingPolygon data models from campus.json.
     * Prefers the local disk file (for immediately saved edits), and falls back to classpath resource.
     * Also auto-detects existing building pictures from images/buildings/ if missing from JSON.
     */
    public static List<BuildingPolygon> loadBuildingPolygons() {
        List<BuildingPolygon> result = null;

        // 1. Try reading from working directory src/main/resources first (reflects new saves immediately)
        File file = new File(JSON_FILE_PATH);
        if (file.exists() && file.length() > 0) {
            try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                result = GSON.fromJson(reader, LIST_TYPE);
            } catch (Exception ignored) {
                // Fallback to classpath
            }
        }

        // 2. Fallback: Read from classpath resources
        if (result == null) {
            try (InputStream is = PolygonDataLoader.class.getResourceAsStream(JSON_RESOURCE_PATH)) {
                if (is != null) {
                    try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                        result = GSON.fromJson(reader, LIST_TYPE);
                    }
                }
            } catch (Exception e) {
                System.err.println("Could not load polygons from JSON: " + e.getMessage());
            }
        }

        if (result == null) {
            result = new ArrayList<>();
        }

        // 3. Auto-detect and reconcile any building images that exist on disk but were not in JSON
        boolean needsResave = false;
        for (BuildingPolygon bp : result) {
            if (bp != null && (bp.getImagePath() == null || bp.getImagePath().isBlank())) {
                String existingImg = BuildingImageStorage.findExistingImageForBuilding(bp.getName());
                if (existingImg != null) {
                    bp.setImagePath(existingImg);
                    needsResave = true;
                }
            }
        }
        if (needsResave) {
            saveBuildingPolygons(result);
        }

        return result;
    }

    /**
     * Persists the list of BuildingPolygon data models to campus.json on disk
     * in both src/main/resources and target/classes for immediate and restart persistence.
     */
    public static synchronized boolean saveBuildingPolygons(List<BuildingPolygon> buildings) {
        if (buildings == null) return false;

        boolean saved = false;
        File file = new File(JSON_FILE_PATH);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(buildings, writer);
            saved = true;
        } catch (Exception e) {
            System.err.println("Failed to save buildings to " + JSON_FILE_PATH + ": " + e.getMessage());
        }

        // Also write to target/classes if available for instant runtime sync
        File targetFile = new File(JSON_TARGET_PATH);
        if (targetFile.getParentFile() != null && targetFile.getParentFile().exists()) {
            try (FileWriter writer = new FileWriter(targetFile, StandardCharsets.UTF_8)) {
                GSON.toJson(buildings, writer);
                saved = true;
            } catch (Exception ignored) {}
        }

        return saved;
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
        return createJavaFXPolygon(model, onPolygonClick, null, isAdminMode);
    }

    /**
     * Converts a BuildingPolygon data model into an interactive JavaFX Polygon with single and double click callbacks.
     */
    public static Polygon createJavaFXPolygon(
        BuildingPolygon model,
        java.util.function.BiConsumer<BuildingPolygon, Polygon> onPolygonClick,
        java.util.function.Consumer<BuildingPolygon> onPolygonDoubleClick,
        boolean isAdminMode
    ) {
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

        polygon.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                if (e.getClickCount() == 2 && onPolygonDoubleClick != null) {
                    e.consume();
                    onPolygonDoubleClick.accept(model);
                } else if (onPolygonClick != null) {
                    onPolygonClick.accept(model, polygon);
                }
            }
        });

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
