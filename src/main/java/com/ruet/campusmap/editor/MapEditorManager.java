package com.ruet.campusmap.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ruet.campusmap.model.BuildingPolygon;
import com.ruet.campusmap.model.Teacher;
import com.ruet.campusmap.service.TeacherDataLoader;
import com.ruet.campusmap.view.BuildingLabelsLayer;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.SVGPath;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Modern Map Editor Manager:
 * - Floating glassmorphic bottom dock (border-radius 28px, ambient drop shadow)
 * - Segmented groups: Mode Switcher, Drawing Actions, View/Layers, Save/Exit
 * - Dynamic context status pill at top center (points count, live instructions)
 * - Collapsible "Buildings & Layers" inspector drawer
 * - Full user-mode invisibility and theme awareness (Light & Dark mode)
 */
public class MapEditorManager {

    private final StackPane root;
    private final Pane polygonLayer;

    // State
    private boolean active = false;
    private boolean isDrawMode = false;
    private boolean previewUserMode = false;
    private boolean isDark = false;

    // Drawing state
    private final List<Double> currentPoints = new ArrayList<>();
    private final Polygon previewPolygon = new Polygon();
    private final javafx.scene.shape.Line guideLine = new javafx.scene.shape.Line();
    private final Group markerGroup = new Group();

    // Permanent polygons in memory
    private final List<BuildingPolygon> savedBuildings = new ArrayList<>();

    // UI Elements
    private HBox toolbar;
    private HBox statusPill;
    private Label statusLabel;

    // Segment 1: Modes
    private Button navBtn;
    private Button drawBtn;

    // Segment 2: Drawing Actions
    private Button undoBtn;
    private Button discardBtn;
    private Button finishBtn;

    // Segment 3: View & Layers
    private Button previewBtn;
    private Button layersBtn;
    private Button teachersBtn;

    // Segment 4: Save & Exit
    private Button saveBtn;
    private Button exitBtn;

    // Slide-out Drawers
    private AdminBuildingsDrawer buildingsDrawer;
    private AdminTeachersDrawer teachersDrawer;

    private final java.util.function.Consumer<BuildingPolygon> onBuildingSelect;
    private final BuildingLabelsLayer buildingLabelsLayer;

    public MapEditorManager(StackPane root, Pane polygonLayer) {
        this(root, polygonLayer, null, null);
    }

    public MapEditorManager(StackPane root, Pane polygonLayer, java.util.function.Consumer<BuildingPolygon> onBuildingSelect) {
        this(root, polygonLayer, onBuildingSelect, null);
    }

    public MapEditorManager(StackPane root, Pane polygonLayer, java.util.function.Consumer<BuildingPolygon> onBuildingSelect, BuildingLabelsLayer buildingLabelsLayer) {
        this.root = root;
        this.polygonLayer = polygonLayer;
        this.onBuildingSelect = onBuildingSelect;
        this.buildingLabelsLayer = buildingLabelsLayer;

        // Load existing saved buildings so subsequent saves don't overwrite them
        List<BuildingPolygon> existing = com.ruet.campusmap.service.PolygonDataLoader.loadBuildingPolygons();
        savedBuildings.addAll(existing);

        setupPreviewLayer();
        setupStatusPill();
        setupBuildingsDrawer();
        setupTeachersDrawer();
        setupToolbar();
        setupMouseListeners();
        setupKeyListeners();
    }

    private void setupPreviewLayer() {
        previewPolygon.setFill(Color.rgb(234, 67, 53, 0.35));
        previewPolygon.setStroke(Color.rgb(217, 48, 37));
        previewPolygon.setStrokeWidth(2.0);

        guideLine.setStroke(Color.rgb(234, 67, 53, 0.85));
        guideLine.setStrokeWidth(1.5);
        guideLine.getStrokeDashArray().addAll(6.0, 4.0);
        guideLine.setMouseTransparent(true);
        guideLine.setVisible(false);

        polygonLayer.getChildren().addAll(previewPolygon, guideLine, markerGroup);
    }

    private void setupStatusPill() {
        statusLabel = new Label("Select Mode: Click any building box to inspect or edit details");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-font-family: 'Segoe UI', Roboto, sans-serif;");

        statusPill = new HBox(8, statusLabel);
        statusPill.setAlignment(Pos.CENTER);
        statusPill.setEffect(new DropShadow(10, 0, 3, Color.rgb(0, 0, 0, 0.12)));
        statusPill.setMaxSize(HBox.USE_PREF_SIZE, HBox.USE_PREF_SIZE);

        StackPane.setAlignment(statusPill, Pos.TOP_CENTER);
        StackPane.setMargin(statusPill, new Insets(76, 0, 0, 0));

        updateStatusPill();
    }

    private void setupBuildingsDrawer() {
        buildingsDrawer = new AdminBuildingsDrawer();
        buildingsDrawer.setCallbacks(
            bp -> {
                Polygon poly = findPolygonNode(bp);
                if (poly != null) {
                    openEditBuildingDialog(bp, poly);
                }
            },
            this::deleteBuilding,
            bp -> {
                if (onBuildingSelect != null) {
                    onBuildingSelect.accept(bp);
                }
            }
        );
        buildingsDrawer.refreshData(savedBuildings);
    }

    private void setupTeachersDrawer() {
        teachersDrawer = new AdminTeachersDrawer();
        teachersDrawer.setCallbacks(
            this::openEditTeacherDialog,
            this::deleteTeacher,
            this::focusOnTeacherBuilding,
            this::openAddTeacherDialog
        );
        teachersDrawer.refreshData(TeacherDataLoader.loadTeachers());
    }

    private void openAddTeacherDialog() {
        EditTeacherDialog.show(null, savedTeacher -> {
            TeacherDataLoader.addTeacher(savedTeacher);
            teachersDrawer.refreshData(TeacherDataLoader.loadTeachers());
            updateTeachersBtn();
        }, null);
    }

    private void openEditTeacherDialog(Teacher teacher) {
        EditTeacherDialog.show(teacher, updated -> {
            TeacherDataLoader.updateTeacher(updated);
            teachersDrawer.refreshData(TeacherDataLoader.loadTeachers());
            updateTeachersBtn();
        }, () -> deleteTeacher(teacher));
    }

    private void deleteTeacher(Teacher teacher) {
        if (teacher == null) return;
        TeacherDataLoader.deleteTeacher(teacher.getId());
        teachersDrawer.refreshData(TeacherDataLoader.loadTeachers());
        updateTeachersBtn();
    }

    private void focusOnTeacherBuilding(Teacher teacher) {
        if (teacher == null || teacher.getBuildingName() == null) return;
        String bldName = teacher.getBuildingName().trim();
        for (BuildingPolygon bp : savedBuildings) {
            if (bp.getName() != null && bp.getName().equalsIgnoreCase(bldName)) {
                if (onBuildingSelect != null) {
                    onBuildingSelect.accept(bp);
                }
                break;
            }
        }
    }

    private void setupToolbar() {
        toolbar = new HBox(6);
        toolbar.setAlignment(Pos.CENTER);
        toolbar.setPadding(new Insets(5, 10, 5, 10));
        toolbar.setEffect(new DropShadow(22, 0, 6, Color.rgb(0, 0, 0, 0.18)));
        toolbar.setMaxSize(HBox.USE_PREF_SIZE, HBox.USE_PREF_SIZE);

        // --- SEGMENT 1: Mode Switcher (Select vs Draw) ---
        navBtn = createDockButton("M3 3l7 18 3-7 7-3L3 3z", "Select", "Navigate and click building boxes to edit");
        navBtn.setOnAction(e -> {
            if (isDrawMode) toggleDrawMode();
        });

        drawBtn = createDockButton("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z", "Draw Box", "Click on campus map to draw building boundary");
        drawBtn.setOnAction(e -> {
            if (!isDrawMode) toggleDrawMode();
        });

        HBox modeSegment = new HBox(3, navBtn, drawBtn);
        modeSegment.setAlignment(Pos.CENTER);

        // --- SEGMENT 2: Drawing Action Tools ---
        undoBtn = createDockButton("M12.5 8c-2.65 0-5.05.99-6.9 2.6L2 7v9h9l-3.62-3.62c1.39-1.16 3.16-1.88 5.12-1.88 3.54 0 6.55 2.31 7.6 5.5l2.37-.78C21.08 11.03 17.15 8 12.5 8z", "Undo", "Undo last placed corner point (Right-click)");
        undoBtn.setDisable(true);
        undoBtn.setOnAction(e -> undoLastPoint());

        discardBtn = createDockButton("M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z", "Clear", "Discard current drawing (ESC)");
        discardBtn.setDisable(true);
        discardBtn.setOnAction(e -> cancelCurrentDrawing());

        finishBtn = createDockButton("M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z", "Finish Box", "Complete building boundary (Min 3 corners)");
        finishBtn.setDisable(true);
        finishBtn.setOnAction(e -> finishCurrentPolygon());

        HBox drawActionsSegment = new HBox(3, undoBtn, discardBtn, finishBtn);
        drawActionsSegment.setAlignment(Pos.CENTER);

        // --- SEGMENT 3: View & Layers ---
        previewBtn = createDockButton("M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z", "Preview", "Toggle user view to inspect map without boxes");
        previewBtn.setOnAction(e -> togglePreviewUserMode());

        layersBtn = createDockButton("M11.99 18.54l-7.37-5.73L3 14.07l9 7 9-7-1.63-1.27-7.38 5.74zM12 16l7.36-5.73L21 9.07l-9-7-9 7 1.63 1.2L12 16z", "Buildings (" + savedBuildings.size() + ")", "Toggle building list inspector");
        layersBtn.setOnAction(e -> {
            if (teachersDrawer.isVisible()) {
                teachersDrawer.hide();
                updateTeachersBtn();
            }
            buildingsDrawer.toggle();
            updateLayersBtn();
        });

        int initialTeacherCount = TeacherDataLoader.loadTeachers().size();
        teachersBtn = createDockButton("M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z", "Teachers (" + initialTeacherCount + ")", "Manage faculty, teachers, and office locations");
        teachersBtn.setOnAction(e -> {
            if (buildingsDrawer.isVisible()) {
                buildingsDrawer.hide();
                updateLayersBtn();
            }
            teachersDrawer.toggle();
            updateTeachersBtn();
        });

        HBox viewSegment = new HBox(3, previewBtn, layersBtn, teachersBtn);
        viewSegment.setAlignment(Pos.CENTER);

        // --- SEGMENT 4: Save & Exit ---
        saveBtn = createDockButton("M17 3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V7l-4-4zm-5 16c-1.66 0-3-1.34-3-3s1.34-3 3-3 3 1.34 3 3-1.34 3-3 3zm3-10H5V5h10v4z", "Save", "Save all changes to campus.json");
        saveBtn.setOnAction(e -> savePolygonsToJson());

        exitBtn = createDockButton("M10.09 15.59L11.5 17l5-5-5-5-1.41 1.41L12.67 11H3v2h9.67l-2.58 2.59zM19 3H5c-1.11 0-2 .9-2 2v4h2V5h14v14H5v-4H3v4c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.89-2-2-2z", "Exit", "Exit editor and lock admin mode");
        exitBtn.setOnAction(e -> deactivate());

        HBox sessionSegment = new HBox(3, saveBtn, exitBtn);
        sessionSegment.setAlignment(Pos.CENTER);

        // Prevent dragging toolbar from panning map underneath
        toolbar.setOnMousePressed(javafx.event.Event::consume);
        toolbar.setOnMouseDragged(javafx.event.Event::consume);

        toolbar.getChildren().addAll(
            modeSegment,
            createDivider(),
            drawActionsSegment,
            createDivider(),
            viewSegment,
            createDivider(),
            sessionSegment
        );

        // Position: floats above the bottom zoom controls (margin 80px)
        StackPane.setAlignment(toolbar, Pos.BOTTOM_CENTER);
        StackPane.setMargin(toolbar, new Insets(0, 0, 80, 0));

        applyTheme(false);
    }

    private Button createDockButton(String svgPathData, String labelText, String tooltipText) {
        SVGPath icon = new SVGPath();
        icon.setContent(svgPathData);
        icon.setFill(Color.web("#5f6368"));
        icon.setScaleX(0.8);
        icon.setScaleY(0.8);

        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 12px; -fx-font-family: 'Segoe UI', Roboto, sans-serif; -fx-font-weight: bold;");

        HBox content = new HBox(6, icon, label);
        content.setAlignment(Pos.CENTER);

        Button btn = new Button();
        btn.setGraphic(content);
        btn.setPrefHeight(34);
        btn.setMinHeight(34);
        btn.setMaxHeight(34);
        btn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #5f6368; " +
            "-fx-background-radius: 17px; -fx-cursor: hand; -fx-padding: 4 12;"
        );
        Tooltip.install(btn, new Tooltip(tooltipText));

        btn.setUserData(new Object[]{icon, label});
        return btn;
    }

    private Separator createDivider() {
        Separator sep = new Separator(Orientation.VERTICAL);
        sep.setMaxHeight(20);
        sep.setStyle("-fx-opacity: 0.35; -fx-padding: 0 2;");
        return sep;
    }

    private void updateModeButtons() {
        if (navBtn == null || drawBtn == null) return;

        Object[] navData = (Object[]) navBtn.getUserData();
        SVGPath navIcon = (SVGPath) navData[0];
        Label navLabel = (Label) navData[1];

        Object[] drawData = (Object[]) drawBtn.getUserData();
        SVGPath drawIcon = (SVGPath) drawData[0];
        Label drawLabel = (Label) drawData[1];

        if (isDrawMode) {
            navBtn.setStyle("-fx-background-color: transparent; -fx-background-radius: 17px; -fx-cursor: hand; -fx-padding: 4 12;");
            navIcon.setFill(Color.web(isDark ? "#9aa0a6" : "#5f6368"));
            navLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + (isDark ? "#9aa0a6;" : "#5f6368;"));

            drawBtn.setStyle("-fx-background-color: #ea4335; -fx-background-radius: 17px; -fx-cursor: hand; -fx-padding: 4 14;");
            drawIcon.setFill(Color.WHITE);
            drawLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;");

            polygonLayer.setCursor(javafx.scene.Cursor.CROSSHAIR);
        } else {
            navBtn.setStyle("-fx-background-color: #1a73e8; -fx-background-radius: 17px; -fx-cursor: hand; -fx-padding: 4 14;");
            navIcon.setFill(Color.WHITE);
            navLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;");

            drawBtn.setStyle("-fx-background-color: transparent; -fx-background-radius: 17px; -fx-cursor: hand; -fx-padding: 4 12;");
            drawIcon.setFill(Color.web(isDark ? "#9aa0a6" : "#5f6368"));
            drawLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + (isDark ? "#9aa0a6;" : "#5f6368;"));

            polygonLayer.setCursor(javafx.scene.Cursor.DEFAULT);
            guideLine.setVisible(false);
        }
        updateStatusPill();
    }

    private void updateButtonStates() {
        int pointCount = currentPoints.size() / 2;

        if (undoBtn != null) {
            undoBtn.setDisable(currentPoints.isEmpty());
            Object[] data = (Object[]) undoBtn.getUserData();
            Label lbl = (Label) data[1];
            lbl.setText(pointCount > 0 ? "Undo (" + pointCount + ")" : "Undo");
        }

        if (discardBtn != null) {
            discardBtn.setDisable(currentPoints.isEmpty());
        }

        if (finishBtn != null) {
            boolean canFinish = currentPoints.size() >= 6;
            finishBtn.setDisable(!canFinish);

            Object[] data = (Object[]) finishBtn.getUserData();
            SVGPath icon = (SVGPath) data[0];
            Label lbl = (Label) data[1];

            if (canFinish) {
                finishBtn.setStyle("-fx-background-color: #1e8e3e; -fx-background-radius: 17px; -fx-cursor: hand; -fx-padding: 4 14;");
                icon.setFill(Color.WHITE);
                lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;");
            } else {
                finishBtn.setStyle("-fx-background-color: transparent; -fx-background-radius: 17px; -fx-padding: 4 12;");
                icon.setFill(Color.web("#9aa0a6"));
                lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #9aa0a6;");
            }
        }

        updateStatusPill();
    }

    private void updateStatusPill() {
        if (statusPill == null || statusLabel == null) return;

        if (previewUserMode) {
            statusLabel.setText("👁 User Preview: All boxes are invisible to regular users");
            statusPill.setStyle(
                "-fx-background-color: " + (isDark ? "#174ea6;" : "#e8f0fe;") +
                "-fx-background-radius: 20px; " +
                "-fx-border-color: " + (isDark ? "#8ab4f8;" : "#aecbfa;") +
                "-fx-border-radius: 20px; -fx-padding: 6 18;"
            );
            statusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + (isDark ? "#d2e3fc;" : "#1a73e8;"));
            statusPill.setVisible(true);
            return;
        }

        if (isDrawMode) {
            int corners = currentPoints.size() / 2;
            if (corners == 0) {
                statusLabel.setText("✏ Draw Mode: Click on campus map to place the first corner");
            } else if (corners < 3) {
                statusLabel.setText("✏ Drawing: " + corners + " corner" + (corners > 1 ? "s" : "") + " placed • Place " + (3 - corners) + " more to finish");
            } else {
                statusLabel.setText("✔ Shape Ready: " + corners + " corners placed • Click 'Finish Box' to name & save");
            }
            statusPill.setStyle(
                "-fx-background-color: " + (isDark ? "#5c1d1d;" : "#fce8e6;") +
                "-fx-background-radius: 20px; " +
                "-fx-border-color: " + (isDark ? "#f28b82;" : "#fad2cf;") +
                "-fx-border-radius: 20px; -fx-padding: 6 18;"
            );
            statusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + (isDark ? "#f6aea9;" : "#c5221f;"));
            statusPill.setVisible(true);
            return;
        }

        // Select mode: gentle status pill
        statusLabel.setText("🖐 Select Mode: Click any building box on map to edit • Click 'Draw Box' to add new");
        statusPill.setStyle(
            "-fx-background-color: " + (isDark ? "#202124;" : "rgba(255, 255, 255, 0.94);") +
            "-fx-background-radius: 20px; " +
            "-fx-border-color: " + (isDark ? "#3c4043;" : "#dadce0;") +
            "-fx-border-radius: 20px; -fx-padding: 6 18;"
        );
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (isDark ? "#bdc1c6;" : "#5f6368;"));
        statusPill.setVisible(true);
    }

    private void updateLayersBtn() {
        if (layersBtn == null) return;
        Object[] data = (Object[]) layersBtn.getUserData();
        Label lbl = (Label) data[1];
        SVGPath icon = (SVGPath) data[0];
        lbl.setText("Buildings (" + savedBuildings.size() + ")");

        if (buildingsDrawer.isVisible()) {
            layersBtn.setStyle("-fx-background-color: #e8f0fe; -fx-background-radius: 17px; -fx-cursor: hand; -fx-padding: 4 12;");
            icon.setFill(Color.web("#1a73e8"));
            lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1a73e8;");
        } else {
            layersBtn.setStyle("-fx-background-color: transparent; -fx-background-radius: 17px; -fx-cursor: hand; -fx-padding: 4 12;");
            icon.setFill(Color.web(isDark ? "#9aa0a6" : "#5f6368"));
            lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + (isDark ? "#9aa0a6;" : "#5f6368;"));
        }
    }

    private void updateTeachersBtn() {
        if (teachersBtn == null || teachersDrawer == null) return;
        Object[] data = (Object[]) teachersBtn.getUserData();
        Label lbl = (Label) data[1];
        SVGPath icon = (SVGPath) data[0];
        int count = TeacherDataLoader.loadTeachers().size();
        lbl.setText("Teachers (" + count + ")");

        if (teachersDrawer.isVisible()) {
            teachersBtn.setStyle("-fx-background-color: #e8f0fe; -fx-background-radius: 17px; -fx-cursor: hand; -fx-padding: 4 12;");
            icon.setFill(Color.web("#1a73e8"));
            lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1a73e8;");
        } else {
            teachersBtn.setStyle("-fx-background-color: transparent; -fx-background-radius: 17px; -fx-cursor: hand; -fx-padding: 4 12;");
            icon.setFill(Color.web(isDark ? "#9aa0a6" : "#5f6368"));
            lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + (isDark ? "#9aa0a6;" : "#5f6368;"));
        }
    }

    private void toggleDrawMode() {
        isDrawMode = !isDrawMode;
        if (!isDrawMode && !currentPoints.isEmpty()) {
            resetDrawingState();
        }
        updateModeButtons();
        updateButtonStates();
    }

    private void togglePreviewUserMode() {
        previewUserMode = !previewUserMode;
        Object[] data = (Object[]) previewBtn.getUserData();
        SVGPath icon = (SVGPath) data[0];
        Label lbl = (Label) data[1];

        if (previewUserMode) {
            previewBtn.setStyle("-fx-background-color: #1a73e8; -fx-background-radius: 17px; -fx-cursor: hand; -fx-padding: 4 12;");
            icon.setFill(Color.WHITE);
            lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;");
            lbl.setText("User View ON");

            if (isDrawMode) {
                toggleDrawMode();
            }
            navBtn.setDisable(true);
            drawBtn.setDisable(true);
            undoBtn.setDisable(true);
            discardBtn.setDisable(true);
            finishBtn.setDisable(true);
        } else {
            previewBtn.setStyle("-fx-background-color: transparent; -fx-background-radius: 17px; -fx-cursor: hand; -fx-padding: 4 12;");
            icon.setFill(Color.web(isDark ? "#9aa0a6" : "#5f6368"));
            lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + (isDark ? "#9aa0a6;" : "#5f6368;"));
            lbl.setText("Preview");

            navBtn.setDisable(false);
            drawBtn.setDisable(false);
            updateButtonStates();
        }
        refreshPolygonVisuals();
        updateStatusPill();
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
            if (!active) return;

            if (event.getCode() == KeyCode.ESCAPE) {
                if (teachersDrawer != null && teachersDrawer.isVisible()) {
                    teachersDrawer.hide();
                    updateTeachersBtn();
                    event.consume();
                    return;
                }
                if (buildingsDrawer.isVisible()) {
                    buildingsDrawer.hide();
                    updateLayersBtn();
                    event.consume();
                    return;
                }
                if (isDrawMode) {
                    if (!currentPoints.isEmpty()) {
                        cancelCurrentDrawing();
                    } else {
                        toggleDrawMode();
                    }
                    event.consume();
                }
            } else if (event.isControlDown() && event.getCode() == KeyCode.Z) {
                if (isDrawMode && !currentPoints.isEmpty()) {
                    undoLastPoint();
                    event.consume();
                }
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

            Point2D mapCoords = polygonLayer.sceneToLocal(event.getSceneX(), event.getSceneY());
            double mapX = mapCoords.getX();
            double mapY = mapCoords.getY();

            // Store point
            currentPoints.add(mapX);
            currentPoints.add(mapY);
            previewPolygon.getPoints().addAll(mapX, mapY);

            // Add visible vertex dot
            Circle dot = new Circle(mapX, mapY, 4.5, Color.web("#d93025"));
            dot.setStroke(Color.WHITE);
            dot.setStrokeWidth(1.5);
            markerGroup.getChildren().add(dot);

            // Update guide line start anchor
            guideLine.setStartX(mapX);
            guideLine.setStartY(mapY);
            guideLine.setEndX(mapX);
            guideLine.setEndY(mapY);
            guideLine.setVisible(true);

            updateButtonStates();
        });

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

    private void finishCurrentPolygon() {
        if (currentPoints.size() < 6) return;

        List<double[]> pointList = new ArrayList<>();
        for (int i = 0; i < currentPoints.size(); i += 2) {
            pointList.add(new double[]{currentPoints.get(i), currentPoints.get(i + 1)});
        }

        Optional<BuildingPolygon> result = promptNewBuildingInfo(pointList);
        if (result.isPresent()) {
            BuildingPolygon bp = result.get();
            savedBuildings.add(bp);

            boolean showAdminBoxes = active && !previewUserMode;
            Polygon finalPoly = com.ruet.campusmap.service.PolygonDataLoader.createJavaFXPolygon(bp, (clickedBp, p) -> {
                handlePolygonClick(clickedBp, p);
            }, showAdminBoxes);
            polygonLayer.getChildren().add(finalPoly);

            if (buildingLabelsLayer != null) {
                buildingLabelsLayer.addPolygonLabel(bp);
            }

            resetDrawingState();
            buildingsDrawer.refreshData(savedBuildings);
            updateLayersBtn();
        }
    }

    private Optional<BuildingPolygon> promptNewBuildingInfo(List<double[]> points) {
        Dialog<BuildingPolygon> dialog = new Dialog<>();
        dialog.setTitle("New Building");
        dialog.setHeaderText("Polygon boundary completed! Enter building details:");

        ButtonType finishButtonType = new ButtonType("Add to Map", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(finishButtonType, ButtonType.CANCEL);

        VBox content = new VBox(12);
        content.setPadding(new Insets(18));

        Label nameLabel = new Label("Building / Location Name:");
        nameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5f6368; -fx-font-weight: bold;");

        TextField nameField = new TextField("New Building");
        nameField.setPromptText("e.g. CSE Department");
        nameField.setStyle(
            "-fx-background-color: #f1f3f4; -fx-background-radius: 8px; " +
            "-fx-border-color: #dadce0; -fx-border-radius: 8px; -fx-padding: 8 12; -fx-font-size: 13px;"
        );

        Label codeLabel = new Label("Building Code Name (e.g. CSE, AUD, ME):");
        codeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5f6368; -fx-font-weight: bold;");

        TextField codeField = new TextField("");
        codeField.setPromptText("e.g. CSE (shown on map badges)");
        codeField.setStyle(
            "-fx-background-color: #f1f3f4; -fx-background-radius: 8px; " +
            "-fx-border-color: #dadce0; -fx-border-radius: 8px; -fx-padding: 8 12; -fx-font-size: 13px;"
        );

        Label colorLabel = new Label("Theme Color:");
        colorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5f6368; -fx-font-weight: bold;");

        ColorPicker colorPicker = new ColorPicker(Color.web("#3498DB"));
        colorPicker.setMaxWidth(Double.MAX_VALUE);
        colorPicker.setStyle("-fx-background-radius: 8px; -fx-cursor: hand;");

        // Color preset chips
        HBox presetChips = new HBox(6);
        presetChips.setAlignment(Pos.CENTER_LEFT);
        String[] presets = {"#1A73E8", "#00897B", "#E65100", "#D93025", "#8E24AA", "#1E8E3E", "#F9AB00"};
        for (String hex : presets) {
            Button chip = new Button();
            chip.setPrefSize(22, 22);
            chip.setMinSize(22, 22);
            chip.setMaxSize(22, 22);
            chip.setStyle("-fx-background-color: " + hex + "; -fx-background-radius: 50%; -fx-cursor: hand; -fx-border-color: #dadce0; -fx-border-radius: 50%;");
            chip.setOnAction(e -> colorPicker.setValue(Color.web(hex)));
            presetChips.getChildren().add(chip);
        }

        VBox colorSection = new VBox(6, colorLabel, colorPicker, presetChips);

        // Visibility checkbox (boxes default to hidden for regular users)
        CheckBox visibleCheck = new CheckBox("Visible to regular users");
        visibleCheck.setSelected(false);
        visibleCheck.setStyle("-fx-font-size: 12px; -fx-text-fill: #202124; -fx-cursor: hand; -fx-font-weight: bold;");

        Label visibleHint = new Label("Unchecked: Clean invisible hitbox for users. Checked: Shows outline on map.");
        visibleHint.setWrapText(true);
        visibleHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #70757a;");

        VBox visibilityCard = new VBox(4, visibleCheck, visibleHint);
        visibilityCard.setPadding(new Insets(10));
        visibilityCard.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8px; -fx-border-color: #dadce0; -fx-border-radius: 8px;");

        content.getChildren().addAll(nameLabel, nameField, codeLabel, codeField, colorSection, visibilityCard);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == finishButtonType) {
                String name = nameField.getText().trim();
                if (name.isEmpty()) name = "New Building";
                String code = codeField.getText() != null ? codeField.getText().trim().toUpperCase() : "";
                Color c = colorPicker.getValue();
                String hex = String.format("#%02X%02X%02X",
                    (int)(c.getRed() * 255),
                    (int)(c.getGreen() * 255),
                    (int)(c.getBlue() * 255)
                );
                boolean visibleToUsers = visibleCheck.isSelected();
                return new BuildingPolygon(name, code, hex, points, visibleToUsers);
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

        if (isDrawMode) {
            return;
        }

        openEditBuildingDialog(bp, poly);
    }

    private void openEditBuildingDialog(BuildingPolygon bp, Polygon poly) {
        EditBuildingDialog.show(
            bp,
            (newName, newCodeName, newColor, visibleToUsers) -> {
                bp.setName(newName);
                bp.setCodeName(newCodeName);
                bp.setColor(newColor);
                bp.setVisibleToUsers(visibleToUsers);

                refreshPolygonVisuals();
                if (buildingLabelsLayer != null) {
                    buildingLabelsLayer.updatePolygonLabel(bp);
                }
                buildingsDrawer.refreshData(savedBuildings);

                if (onBuildingSelect != null) {
                    onBuildingSelect.accept(bp);
                }
            },
            () -> {
                savedBuildings.remove(bp);
                polygonLayer.getChildren().remove(poly);
                if (buildingLabelsLayer != null) {
                    buildingLabelsLayer.removePolygonLabel(bp);
                }
                buildingsDrawer.refreshData(savedBuildings);
                updateLayersBtn();
            }
        );
    }

    private void deleteBuilding(BuildingPolygon bp) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete '" + bp.getName() + "'?");
        confirm.setContentText("Are you sure you want to delete this building polygon? This cannot be undone.");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            savedBuildings.remove(bp);
            Polygon poly = findPolygonNode(bp);
            if (poly != null) {
                polygonLayer.getChildren().remove(poly);
            }
            if (buildingLabelsLayer != null) {
                buildingLabelsLayer.removePolygonLabel(bp);
            }
            buildingsDrawer.refreshData(savedBuildings);
            updateLayersBtn();
        }
    }

    private Polygon findPolygonNode(BuildingPolygon bp) {
        for (javafx.scene.Node node : polygonLayer.getChildren()) {
            if (node instanceof Polygon && node.getUserData() == bp) {
                return (Polygon) node;
            }
        }
        return null;
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
            isDrawMode = false;

            updateModeButtons();
            updateButtonStates();
            updateLayersBtn();
            updateTeachersBtn();

            if (!root.getChildren().contains(toolbar)) {
                root.getChildren().add(toolbar);
            }
            if (!root.getChildren().contains(statusPill)) {
                root.getChildren().add(statusPill);
            }
            if (!root.getChildren().contains(buildingsDrawer.getContainer())) {
                root.getChildren().add(buildingsDrawer.getContainer());
            }
            if (!root.getChildren().contains(teachersDrawer.getContainer())) {
                root.getChildren().add(teachersDrawer.getContainer());
            }

            buildingsDrawer.refreshData(savedBuildings);
            teachersDrawer.refreshData(TeacherDataLoader.loadTeachers());
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
            buildingsDrawer.hide();
            teachersDrawer.hide();
            root.getChildren().remove(toolbar);
            root.getChildren().remove(statusPill);
            root.getChildren().remove(buildingsDrawer.getContainer());
            root.getChildren().remove(teachersDrawer.getContainer());
            refreshPolygonVisuals();
        }
    }

    public boolean isDrawMode() {
        return active && isDrawMode;
    }

    public boolean isActive() {
        return active;
    }

    public void applyTheme(boolean isDark) {
        this.isDark = isDark;
        if (toolbar != null) {
            toolbar.setStyle(
                "-fx-background-color: " + (isDark ? "rgba(32, 33, 36, 0.95);" : "rgba(255, 255, 255, 0.95);") +
                "-fx-background-radius: 28px; " +
                "-fx-border-color: " + (isDark ? "#3c4043;" : "#dadce0;") +
                "-fx-border-radius: 28px; " +
                "-fx-border-width: 1px;"
            );
        }
        if (buildingsDrawer != null) {
            buildingsDrawer.applyTheme(isDark);
        }
        if (teachersDrawer != null) {
            teachersDrawer.applyTheme(isDark);
        }
        updateModeButtons();
        updateButtonStates();
        updateLayersBtn();
        updateTeachersBtn();
    }
}
