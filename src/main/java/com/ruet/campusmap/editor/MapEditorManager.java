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
import javafx.scene.layout.VBox;
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
    private boolean previewUserMode = false;

    // Drawing state
    private final List<Double> currentPoints = new ArrayList<>();
    private final Polygon previewPolygon = new Polygon();
    private final javafx.scene.shape.Line guideLine = new javafx.scene.shape.Line();
    private final Group markerGroup = new Group();

    // Permanent polygons in memory
    private final List<BuildingPolygon> savedBuildings = new ArrayList<>();

    // UI elements
    private HBox toolbar;
    private Button modeBtn;
    private Button finishBtn;
    private Button undoBtn;
    private Button cancelBtn;
    private Button previewBtn;

    private final java.util.function.Consumer<BuildingPolygon> onBuildingSelect;

    public MapEditorManager(StackPane root, Pane polygonLayer) {
        this(root, polygonLayer, null);
    }

    public MapEditorManager(StackPane root, Pane polygonLayer, java.util.function.Consumer<BuildingPolygon> onBuildingSelect) {
        this.root = root;
        this.polygonLayer = polygonLayer;
        this.onBuildingSelect = onBuildingSelect;

        // Load existing saved buildings so subsequent saves don't overwrite them
        List<BuildingPolygon> existing = com.ruet.campusmap.service.PolygonDataLoader.loadBuildingPolygons();
        savedBuildings.addAll(existing);

        setupPreviewLayer();
        setupToolbar();
        setupMouseListeners();
        setupKeyListeners();
    }

    private void setupPreviewLayer() {
        previewPolygon.setFill(Color.rgb(231, 76, 60, 0.4)); // Semi-transparent red
        previewPolygon.setStroke(Color.rgb(192, 57, 43));
        previewPolygon.setStrokeWidth(2.0);

        guideLine.setStroke(Color.rgb(231, 76, 60, 0.8));
        guideLine.setStrokeWidth(1.5);
        guideLine.getStrokeDashArray().addAll(6.0, 4.0);
        guideLine.setMouseTransparent(true);
        guideLine.setVisible(false);

        polygonLayer.getChildren().addAll(previewPolygon, guideLine, markerGroup);
    }

    private void setupToolbar() {
        toolbar = new HBox(10);
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

        undoBtn = new Button("Undo Point");
        undoBtn.setStyle("-fx-background-color: #f1f3f4; -fx-cursor: hand;");
        undoBtn.setDisable(true);
        undoBtn.setOnAction(e -> undoLastPoint());

        cancelBtn = new Button("Cancel Drawing");
        cancelBtn.setStyle("-fx-background-color: #f1f3f4; -fx-cursor: hand;");
        cancelBtn.setDisable(true);
        cancelBtn.setOnAction(e -> cancelCurrentDrawing());

        finishBtn = new Button("Finish Polygon");
        finishBtn.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
        finishBtn.setDisable(true);
        finishBtn.setOnAction(e -> finishCurrentPolygon());

        previewBtn = new Button("👁 Preview User View");
        previewBtn.setStyle("-fx-background-color: #f1f3f4; -fx-cursor: hand; -fx-font-weight: bold;");
        previewBtn.setOnAction(e -> togglePreviewUserMode());

        Label hiddenBadge = new Label("🔒 Boxes Hidden for Users");
        hiddenBadge.setStyle("-fx-font-size: 11px; -fx-text-fill: #5f6368; -fx-padding: 0 4px; -fx-font-style: italic;");

        Button saveBtn = new Button("Save to JSON");
        saveBtn.setStyle("-fx-background-color: #34a853; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
        saveBtn.setOnAction(e -> savePolygonsToJson());

        Button exitBtn = new Button("Exit Admin");
        exitBtn.setStyle("-fx-background-color: #ea4335; -fx-text-fill: white; -fx-cursor: hand;");
        exitBtn.setOnAction(e -> deactivate());

        // Prevent dragging on the toolbar from moving the map underneath
        toolbar.setOnMousePressed(javafx.event.Event::consume);
        toolbar.setOnMouseDragged(javafx.event.Event::consume);

        toolbar.getChildren().addAll(editorBadge, modeBtn, undoBtn, cancelBtn, finishBtn, previewBtn, hiddenBadge, saveBtn, exitBtn);
        StackPane.setAlignment(toolbar, Pos.BOTTOM_CENTER);
        StackPane.setMargin(toolbar, new Insets(0, 0, 30, 0));
    }

    private void togglePreviewUserMode() {
        previewUserMode = !previewUserMode;
        if (previewUserMode) {
            previewBtn.setText("🛠 Back to Editor View");
            previewBtn.setStyle("-fx-background-color: #e8f0fe; -fx-text-fill: #1a73e8; -fx-cursor: hand; -fx-font-weight: bold;");
            if (isDrawMode) {
                toggleDrawMode();
            }
            modeBtn.setDisable(true);
            undoBtn.setDisable(true);
            cancelBtn.setDisable(true);
            finishBtn.setDisable(true);
        } else {
            previewBtn.setText("👁 Preview User View");
            previewBtn.setStyle("-fx-background-color: #f1f3f4; -fx-cursor: hand; -fx-font-weight: bold;");
            modeBtn.setDisable(false);
            updateButtonStates();
        }
        refreshPolygonVisuals();
    }

    public void refreshPolygonVisuals() {
        boolean showAdminBoxes = active && !previewUserMode;
        for (javafx.scene.Node node : polygonLayer.getChildren()) {
            if (node instanceof Polygon && node.getUserData() instanceof BuildingPolygon) {
                Polygon poly = (Polygon) node;
                BuildingPolygon bp = (BuildingPolygon) node.getUserData();
                com.ruet.campusmap.service.PolygonDataLoader.applyPolygonStyle(poly, bp, showAdminBoxes);
            }
        }
        previewPolygon.setVisible(showAdminBoxes);
        markerGroup.setVisible(showAdminBoxes);
        guideLine.setVisible(showAdminBoxes && !currentPoints.isEmpty());
    }

    private void setupKeyListeners() {
        root.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (active && isDrawMode && event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                cancelCurrentDrawing();
                event.consume();
            }
        });
    }

    public void cancelCurrentDrawing() {
        if (!currentPoints.isEmpty()) {
            resetDrawingState();
        }
    }

    private void setupMouseListeners() {
        polygonLayer.setOnMouseClicked(event -> {
            if (!active || !isDrawMode || previewUserMode) {
                return;
            }

            // Right-click removes the last point while drawing
            if (event.getButton() == MouseButton.SECONDARY) {
                undoLastPoint();
                event.consume();
                return;
            }

            if (event.getButton() != MouseButton.PRIMARY) {
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

            // Update guide line start anchor
            guideLine.setStartX(mapX);
            guideLine.setStartY(mapY);
            guideLine.setEndX(mapX);
            guideLine.setEndY(mapY);
            guideLine.setVisible(true);

            updateButtonStates();
        });

        // Dynamic rubber-band guide line following the mouse cursor
        polygonLayer.setOnMouseMoved(event -> {
            if (!active || !isDrawMode || currentPoints.isEmpty() || previewUserMode) {
                guideLine.setVisible(false);
                return;
            }
            Point2D mapCoords = polygonLayer.sceneToLocal(event.getSceneX(), event.getSceneY());
            double lastX = currentPoints.get(currentPoints.size() - 2);
            double lastY = currentPoints.get(currentPoints.size() - 1);
            guideLine.setStartX(lastX);
            guideLine.setStartY(lastY);
            guideLine.setEndX(mapCoords.getX());
            guideLine.setEndY(mapCoords.getY());
            guideLine.setVisible(true);
        });
    }

    private void undoLastPoint() {
        if (currentPoints.size() >= 2) {
            currentPoints.remove(currentPoints.size() - 1);
            currentPoints.remove(currentPoints.size() - 1);

            int pSize = previewPolygon.getPoints().size();
            if (pSize >= 2) {
                previewPolygon.getPoints().remove(pSize - 1);
                previewPolygon.getPoints().remove(pSize - 2);
            }

            int mSize = markerGroup.getChildren().size();
            if (mSize > 0) {
                markerGroup.getChildren().remove(mSize - 1);
            }

            if (currentPoints.isEmpty()) {
                guideLine.setVisible(false);
            } else {
                guideLine.setStartX(currentPoints.get(currentPoints.size() - 2));
                guideLine.setStartY(currentPoints.get(currentPoints.size() - 1));
            }

            updateButtonStates();
        }
    }

    private void updateButtonStates() {
        if (undoBtn != null) {
            undoBtn.setDisable(currentPoints.isEmpty());
        }
        if (cancelBtn != null) {
            cancelBtn.setDisable(currentPoints.isEmpty());
        }
        if (finishBtn != null) {
            finishBtn.setDisable(currentPoints.size() < 6);
        }
    }

    private void toggleDrawMode() {
        isDrawMode = !isDrawMode;
        if (isDrawMode) {
            modeBtn.setText("Mode: DRAW (Click map to add points)");
            modeBtn.setStyle("-fx-background-color: #fce8e6; -fx-text-fill: #c5221f; -fx-font-weight: bold; -fx-cursor: hand;");
            polygonLayer.setCursor(javafx.scene.Cursor.CROSSHAIR);
        } else {
            modeBtn.setText("Mode: PAN");
            modeBtn.setStyle("-fx-background-color: #f1f3f4; -fx-cursor: hand; -fx-font-weight: bold;");
            polygonLayer.setCursor(javafx.scene.Cursor.DEFAULT);
            guideLine.setVisible(false);
        }
    }

    private void finishCurrentPolygon() {
        if (currentPoints.size() < 6) return;

        // Convert Flat List<Double> to List<double[]>
        List<double[]> pointList = new ArrayList<>();
        for (int i = 0; i < currentPoints.size(); i += 2) {
            pointList.add(new double[]{currentPoints.get(i), currentPoints.get(i + 1)});
        }

        Optional<BuildingPolygon> result = promptNewBuildingInfo(pointList);
        if (result.isPresent()) {
            BuildingPolygon bp = result.get();
            savedBuildings.add(bp);

            // Create Visual JavaFX Polygon on the map using PolygonDataLoader with active editor style
            boolean showAdminBoxes = active && !previewUserMode;
            Polygon finalPoly = com.ruet.campusmap.service.PolygonDataLoader.createJavaFXPolygon(bp, (clickedBp, p) -> {
                handlePolygonClick(clickedBp, p);
            }, showAdminBoxes);
            polygonLayer.getChildren().add(finalPoly);

            // Reset drawing state for next polygon
            resetDrawingState();
        }
    }

    private Optional<BuildingPolygon> promptNewBuildingInfo(List<double[]> points) {
        Dialog<BuildingPolygon> dialog = new Dialog<>();
        dialog.setTitle("New Building Information");
        dialog.setHeaderText("Polygon completed! Enter details:");

        ButtonType finishButtonType = new ButtonType("Add to Map", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(finishButtonType, ButtonType.CANCEL);

        VBox content = new VBox(10);
        content.setPadding(new Insets(16));

        Label nameLabel = new Label("Building / Location Name:");
        nameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5f6368;");

        TextField nameField = new TextField("New Building");
        nameField.setPromptText("e.g. CSE Department");

        Label colorLabel = new Label("Polygon Color:");
        colorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5f6368;");

        ColorPicker colorPicker = new ColorPicker(Color.web("#3498DB"));
        colorPicker.setMaxWidth(Double.MAX_VALUE);

        // Visibility checkbox (boxes default to hidden for regular users)
        CheckBox visibleCheck = new CheckBox("Visible to regular users");
        visibleCheck.setSelected(false);
        visibleCheck.setStyle("-fx-font-size: 12px; -fx-text-fill: #3c4043; -fx-cursor: hand;");

        Label visibleHint = new Label("Keep unchecked for invisible hitbox (clean campus map)");
        visibleHint.setStyle("-fx-font-size: 10px; -fx-text-fill: #80868b;");
        VBox visibilityBox = new VBox(2, visibleCheck, visibleHint);

        content.getChildren().addAll(nameLabel, nameField, colorLabel, colorPicker, visibilityBox);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == finishButtonType) {
                String name = nameField.getText().trim();
                if (name.isEmpty()) name = "New Building";
                Color c = colorPicker.getValue();
                String hex = String.format("#%02X%02X%02X",
                    (int)(c.getRed() * 255),
                    (int)(c.getGreen() * 255),
                    (int)(c.getBlue() * 255)
                );
                boolean visibleToUsers = visibleCheck.isSelected();
                return new BuildingPolygon(name, hex, points, visibleToUsers);
            }
            return null;
        });

        return dialog.showAndWait();
    }

    public void handlePolygonClick(BuildingPolygon bp, Polygon poly) {
        if (!active || previewUserMode) {
            if (onBuildingSelect != null) {
                onBuildingSelect.accept(bp);
            }
            return;
        }

        // In draw mode, clicks should place points, not open edit
        if (isDrawMode) {
            return;
        }

        openEditBuildingDialog(bp, poly);
    }

    private void openEditBuildingDialog(BuildingPolygon bp, Polygon poly) {
        EditBuildingDialog.show(
            bp,
            (newName, newColor, visibleToUsers) -> {
                bp.setName(newName);
                bp.setColor(newColor);
                bp.setVisibleToUsers(visibleToUsers);

                // Update visual polygon style and tooltip
                refreshPolygonVisuals();
                Tooltip.install(poly, new Tooltip(newName));

                if (onBuildingSelect != null) {
                    onBuildingSelect.accept(bp);
                }
            },
            () -> {
                savedBuildings.remove(bp);
                polygonLayer.getChildren().remove(poly);
            }
        );
    }

    private void resetDrawingState() {
        currentPoints.clear();
        previewPolygon.getPoints().clear();
        markerGroup.getChildren().clear();
        guideLine.setVisible(false);
        updateButtonStates();
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
            previewUserMode = false;
            if (previewBtn != null) {
                previewBtn.setText("👁 Preview User View");
                previewBtn.setStyle("-fx-background-color: #f1f3f4; -fx-cursor: hand; -fx-font-weight: bold;");
            }
            if (modeBtn != null) {
                modeBtn.setDisable(false);
            }
            if (!root.getChildren().contains(toolbar)) {
                root.getChildren().add(toolbar);
            }
            refreshPolygonVisuals();
        }
    }

    public void deactivate() {
        if (active) {
            active = false;
            previewUserMode = false;
            isDrawMode = false;
            polygonLayer.setCursor(javafx.scene.Cursor.DEFAULT);
            resetDrawingState();
            root.getChildren().remove(toolbar);
            refreshPolygonVisuals();
        }
    }

    public boolean isDrawMode() {
        return active && isDrawMode;
    }

    public boolean isActive() {
        return active;
    }
}
