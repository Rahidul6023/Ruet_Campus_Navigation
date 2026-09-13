package com.ruet.campusmap.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ruet.campusmap.model.BuildingPolygon;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

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
        return createJavaFXPolygon(model, null);
    }

    /**
     * Converts a BuildingPolygon data model into an interactive JavaFX Polygon with custom click callback.
     */
    public static Polygon createJavaFXPolygon(BuildingPolygon model, java.util.function.Consumer<BuildingPolygon> onClick) {
        Polygon polygon = new Polygon();

        // Flatten points from List<double[]> to JavaFX ObservableList<Double>
        if (model.getPoints() != null) {
            for (double[] point : model.getPoints()) {
                if (point.length >= 2) {
                    polygon.getPoints().addAll(point[0], point[1]);
                }
            }
        }

        // Style the polygon
        Color baseColor = Color.web(model.getColor() != null ? model.getColor() : "#3498DB");
        Color fillColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 0.45);

        polygon.setFill(fillColor);
        polygon.setStroke(baseColor);
        polygon.setStrokeWidth(2.0);

        // Tooltip showing building name
        if (model.getName() != null && !model.getName().isEmpty()) {
            Tooltip tooltip = new Tooltip(model.getName());
            Tooltip.install(polygon, tooltip);
        }

        // Subtle hover feedback
        polygon.setOnMouseEntered(e -> {
            polygon.setFill(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 0.7));
            polygon.setStrokeWidth(3.0);
        });

        polygon.setOnMouseExited(e -> {
            polygon.setFill(fillColor);
            polygon.setStrokeWidth(2.0);
        });

        if (onClick != null) {
            polygon.setOnMouseClicked(e -> {
                if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                    onClick.accept(model);
                    e.consume(); // Prevent click from triggering map pan/drag
                }
            });
        }

        return polygon;
    }
}
