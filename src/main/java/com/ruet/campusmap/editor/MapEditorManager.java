package com.ruet.campusmap.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ruet.campusmap.model.BuildingPolygon;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MapEditorManager {

    private final StackPane root;
    private final Pane polygonLayer;

    // State
    private boolean active = false;
    private boolean isDrawMode = false;

    // Drawing state
    private final List<Double> currentPoints = new ArrayList<>();
    private final Polygon previewPolygon = new Polygon();
    private final Group markerGroup = new Group();

    // Permanent polygons in memory
    private final List<BuildingPolygon> savedBuildings = new ArrayList<>();

    // UI elements
    private HBox toolbar;
    private Button modeBtn;
    private Button finishBtn;

    public MapEditorManager(StackPane root, Pane polygonLayer) {
        this.root = root;
        this.polygonLayer = polygonLayer;

        // Load existing saved buildings so subsequent saves don't overwrite them
        List<BuildingPolygon> existing = com.ruet.campusmap.service.PolygonDataLoader.loadBuildingPolygons();
        savedBuildings.addAll(existing);

        setupPreviewLayer();
        setupToolbar();
        setupMouseListeners();
    }

    private void setupPreviewLayer() {
        previewPolygon.setFill(Color.rgb(231, 76, 60, 0.4)); // Semi-transparent red
        previewPolygon.setStroke(Color.rgb(192, 57, 43));
        previewPolygon.setStrokeWidth(2.0);

        polygonLayer.getChildren().addAll(previewPolygon, markerGroup);
    }

    private void setupToolbar() {
        toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(10, 16, 10, 16));
        toolbar.setStyle(
            "-fx-background-color: #ffffff; " +
            "-fx-background-radius: 8px; " +
            "-fx-border-color: #dadce0; " +
            "-fx-border-radius: 8px; " +
            "-fx-border-width: 1px;"
        );
        toolbar.setEffect(new DropShadow(10, 0, 4, Color.rgb(0, 0, 0, 0.2)));
        toolbar.setMaxSize(HBox.USE_PREF_SIZE, HBox.USE_PREF_SIZE);

        Label editorBadge = new Label("🛠 EDITOR MODE");
        editorBadge.setStyle("-fx-font-weight: bold; -fx-text-fill: #e65100; -fx-font-size: 13px;");

        modeBtn = new Button("Mode: PAN");
        modeBtn.setStyle("-fx-background-color: #f1f3f4; -fx-cursor: hand; -fx-font-weight: bold;");
        modeBtn.setOnAction(e -> toggleDrawMode());

        finishBtn = new Button("Finish Polygon");
        finishBtn.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
        finishBtn.setDisable(true);
        finishBtn.setOnAction(e -> finishCurrentPolygon());

        Button saveBtn = new Button("Save to JSON");
        saveBtn.setStyle("-fx-background-color: #34a853; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
        saveBtn.setOnAction(e -> savePolygonsToJson());

        Button exitBtn = new Button("Exit Admin");
        exitBtn.setStyle("-fx-background-color: #ea4335; -fx-text-fill: white; -fx-cursor: hand;");
        exitBtn.setOnAction(e -> deactivate());

        // Prevent dragging on the toolbar from moving the map underneath
        toolbar.setOnMousePressed(javafx.event.Event::consume);
        toolbar.setOnMouseDragged(javafx.event.Event::consume);

        toolbar.getChildren().addAll(editorBadge, modeBtn, finishBtn, saveBtn, exitBtn);
        StackPane.setAlignment(toolbar, Pos.BOTTOM_CENTER);
        StackPane.setMargin(toolbar, new Insets(0, 0, 30, 0));
    }

    private void setupMouseListeners() {
        polygonLayer.setOnMouseClicked(event -> {
            if (!active || !isDrawMode || event.getButton() != MouseButton.PRIMARY) {
                return;
            }

            // Convert screen coordinates into Map coordinates
            Point2D mapCoords = polygonLayer.sceneToLocal(event.getSceneX(), event.getSceneY());
            double mapX = mapCoords.getX();
            double mapY = mapCoords.getY();

            // Store point
            currentPoints.add(mapX);
            currentPoints.add(mapY);
            previewPolygon.getPoints().addAll(mapX, mapY);

            // Add visible vertex dot
            Circle dot = new Circle(mapX, mapY, 4, Color.RED);
            dot.setStroke(Color.WHITE);
            dot.setStrokeWidth(1.0);
            markerGroup.getChildren().add(dot);

            // Enable finish button once we have at least 3 vertices (6 coordinates)
            if (currentPoints.size() >= 6) {
                finishBtn.setDisable(false);
            }
        });
    }

    private void toggleDrawMode() {
        isDrawMode = !isDrawMode;
        if (isDrawMode) {
            modeBtn.setText("Mode: DRAW (Click map to add points)");
            modeBtn.setStyle("-fx-background-color: #fce8e6; -fx-text-fill: #c5221f; -fx-font-weight: bold; -fx-cursor: hand;");
        } else {
            modeBtn.setText("Mode: PAN");
            modeBtn.setStyle("-fx-background-color: #f1f3f4; -fx-cursor: hand; -fx-font-weight: bold;");
        }
    }

    private void finishCurrentPolygon() {
        if (currentPoints.size() < 6) return;

        // Prompt user for Building Name
        TextInputDialog nameDialog = new TextInputDialog("New Building");
        nameDialog.setTitle("Building Information");
        nameDialog.setHeaderText("Polygon completed!");
        nameDialog.setContentText("Enter Building / Location Name:");

        Optional<String> result = nameDialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String name = result.get().trim();
            String defaultColor = "#3498DB";

            // Convert Flat List<Double> to List<double[]>
            List<double[]> pointList = new ArrayList<>();
            for (int i = 0; i < currentPoints.size(); i += 2) {
                pointList.add(new double[]{currentPoints.get(i), currentPoints.get(i + 1)});
            }

            // Create Data Model
            BuildingPolygon bp = new BuildingPolygon(name, defaultColor, pointList);
            savedBuildings.add(bp);

            // Create Visual JavaFX Polygon on the map using PolygonDataLoader
            Polygon finalPoly = com.ruet.campusmap.service.PolygonDataLoader.createJavaFXPolygon(bp);
            polygonLayer.getChildren().add(finalPoly);

            // Reset drawing state for next polygon
            resetDrawingState();
        }
    }

    private void resetDrawingState() {
        currentPoints.clear();
        previewPolygon.getPoints().clear();
        markerGroup.getChildren().clear();
        finishBtn.setDisable(true);
    }

    private void savePolygonsToJson() {
        File file = new File("src/main/resources/data/campus.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(savedBuildings, writer);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Successfully saved " + savedBuildings.size() + " buildings to campus.json!");
            alert.showAndWait();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Save Error");
            alert.setContentText("Could not save to JSON: " + e.getMessage());
            alert.showAndWait();
        }
    }

    public void activate() {
        if (!active) {
            active = true;
            if (!root.getChildren().contains(toolbar)) {
                root.getChildren().add(toolbar);
            }
        }
    }

    public void deactivate() {
        if (active) {
            active = false;
            isDrawMode = false;
            resetDrawingState();
            root.getChildren().remove(toolbar);
        }
    }

    public boolean isDrawMode() {
        return active && isDrawMode;
    }
}
