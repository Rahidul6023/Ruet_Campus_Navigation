package com.ruet.campusmap.view;

import com.ruet.campusmap.editor.EditRoomDialog;
import com.ruet.campusmap.model.AppSettings;
import com.ruet.campusmap.model.BuildingInnerMap;
import com.ruet.campusmap.model.FloorPlan;
import com.ruet.campusmap.model.RoomLabel;
import com.ruet.campusmap.model.RoomLocation;
import com.ruet.campusmap.service.InnerMapRegistry;
import com.ruet.campusmap.service.RoomRegistry;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.web.WebView;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Full-screen interactive indoor floor plan explorer for campus buildings.
 * Supports automatic fit-to-screen overview, wide-range smooth zooming,
 * free panning, multi-floor switching, room label display & editing,
 * and responsive dark/light theme integration.
 */
public class InnerMapView {

    private final StackPane container;
    private final AppSettings settings;
    private final WebView floorWebView;
    private final Group floorMapGroup;
    private final StackPane viewport;

    private final RoomLabelsLayer roomLabelsLayer;
    private final RoomHoverTooltip roomHoverTooltip;
    private final List<RoomLabel> currentRooms = new ArrayList<>();

    private final Label buildingTitleLabel;
    private final Label floorBadge;
    private final Label floorDescLabel;
    private final Button editRoomsBtn;
    private final HBox topHeaderBar;
    private final HBox editModeBanner;
    private final Label editInstructionLabel;
    private final VBox floorSwitcherBox;
    private final HBox zoomControlsBox;
    private final Label zoomPercentLabel;
    private final Button backBtn;

    private final List<Button> floorButtons = new ArrayList<>();
    private BuildingInnerMap currentBuilding;
    private FloorPlan currentFloor;
    private Runnable onBackCallback;
    private Runnable onAdminLoginRequested;
    private boolean adminMode = false;
    private boolean roomEditMode = false;

    // Reserved layout gaps around the inner map for UI controls
    // Top and bottom have space for the header bar and zoom controls
    // Left and right have less gap, with right leaving room for the floor switcher
    private static final double GAP_TOP = 85.0;
    private static final double GAP_BOTTOM = 85.0;
    private static final double GAP_LEFT = 35.0;
    private static final double GAP_RIGHT = 80.0;

    private final double mapWidth = 2300;
    private final double mapHeight = 1700;
    private double lastMouseX;
    private double lastMouseY;
    private boolean isDark = false;

    public InnerMapView(AppSettings settings) {
        this.settings = settings;

        container = new StackPane();
        container.setVisible(false);
        container.setManaged(false);
        container.setMinSize(0, 0);
        container.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        container.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // --- 1. Floor Plan SVG Web Engine View ---
        floorWebView = new WebView();
        floorWebView.setPageFill(Color.TRANSPARENT);
        floorWebView.setPrefSize(mapWidth, mapHeight);
        floorWebView.setMinSize(mapWidth, mapHeight);
        floorWebView.setMaxSize(mapWidth, mapHeight);
        floorWebView.resize(mapWidth, mapHeight);
        floorWebView.setMouseTransparent(true);

        floorWebView.getChildrenUnmodifiable().addListener((javafx.collections.ListChangeListener<javafx.scene.Node>) change -> {
            for (javafx.scene.Node node : floorWebView.lookupAll(".scroll-bar")) {
                node.setVisible(false);
                node.setManaged(false);
            }
        });

        roomLabelsLayer = new RoomLabelsLayer(mapWidth, mapHeight);
        roomLabelsLayer.setCallbacks(this::openEditRoomDialog, this::handleRoomMoved);

        roomHoverTooltip = new RoomHoverTooltip();
        roomLabelsLayer.setHoverTooltip(roomHoverTooltip);

        floorMapGroup = new Group(floorWebView, roomLabelsLayer.getPane());
        viewport = new StackPane(floorMapGroup);
        viewport.setStyle("-fx-background-color: transparent;");
        viewport.setMinSize(0, 0);
        viewport.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        viewport.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // --- 2. Top Header Bar (Back button + Building info + Floor description + Label Rooms btn) ---
        SVGPath backArrow = new SVGPath();
        backArrow.setContent("M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z");
        backArrow.setScaleX(0.85);
        backArrow.setScaleY(0.85);

        backBtn = new Button("Campus Map", backArrow);
        backBtn.setStyle(
            "-fx-font-size: 13px; -fx-font-weight: bold; " +
            "-fx-background-radius: 20px; -fx-cursor: hand; -fx-padding: 7 16;"
        );
        backBtn.setOnAction(e -> close());

        buildingTitleLabel = new Label("Building Inner Map");
        buildingTitleLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-font-family: 'Segoe UI', Roboto, sans-serif;");

        floorBadge = new Label("FLOOR 0");
        floorBadge.setStyle(
            "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 8; " +
            "-fx-background-radius: 10px;"
        );

        HBox titleRow = new HBox(8, buildingTitleLabel, floorBadge);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        floorDescLabel = new Label("Floor plan overview");
        floorDescLabel.setStyle("-fx-font-size: 12px; -fx-font-family: 'Segoe UI', Roboto, sans-serif;");

        VBox titleDetails = new VBox(2, titleRow, floorDescLabel);
        titleDetails.setAlignment(Pos.CENTER_LEFT);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        editRoomsBtn = new Button();
        updateEditRoomsBtn();
        editRoomsBtn.setOnAction(e -> {
            if (!adminMode) {
                if (onAdminLoginRequested != null) {
                    onAdminLoginRequested.run();
                }
            } else {
                setRoomEditMode(!roomEditMode);
            }
        });

        topHeaderBar = new HBox(14, backBtn, titleDetails, headerSpacer, editRoomsBtn);
        topHeaderBar.setAlignment(Pos.CENTER_LEFT);
        topHeaderBar.setPadding(new Insets(10, 18, 10, 14));
        topHeaderBar.setEffect(new DropShadow(16, 0, 4, Color.rgb(0, 0, 0, 0.16)));
        topHeaderBar.setMaxHeight(Region.USE_PREF_SIZE);

        StackPane.setAlignment(topHeaderBar, Pos.TOP_LEFT);
        StackPane.setMargin(topHeaderBar, new Insets(18, 20, 0, 20));

        // --- 3. Room Edit Mode Floating Banner ---
        editModeBanner = new HBox(14);
        editModeBanner.setAlignment(Pos.CENTER);
        editModeBanner.setPadding(new Insets(8, 18, 8, 18));
        editModeBanner.setVisible(false);
        editModeBanner.setManaged(false);
        editModeBanner.setEffect(new DropShadow(16, 0, 4, Color.rgb(0, 0, 0, 0.22)));
        editModeBanner.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        Label editBadge = new Label("✏️ ROOM EDIT MODE");
        editBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: #e8f0fe; -fx-text-fill: #1a73e8; -fx-background-radius: 10px; -fx-padding: 3 8;");

        editInstructionLabel = new Label("Click map to add room • Drag badges to move • Click badge to edit");
        editInstructionLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 500; -fx-font-family: 'Segoe UI', Roboto, sans-serif;");

        Button addRoomDirectBtn = new Button("+ Add Room");
        addRoomDirectBtn.setStyle(
            "-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-background-radius: 16px; -fx-padding: 5 14; -fx-cursor: hand; -fx-font-size: 12px;"
        );
        addRoomDirectBtn.setOnAction(e -> {
            double cx = mapWidth / 2.0;
            double cy = mapHeight / 2.0;
            try {
                Point2D p = floorMapGroup.sceneToLocal(getViewWidth() / 2.0, getViewHeight() / 2.0);
                if (p != null && p.getX() >= 0 && p.getY() >= 0 && p.getX() <= mapWidth && p.getY() <= mapHeight) {
                    cx = p.getX();
                    cy = p.getY();
                }
            } catch (Exception ignored) {}
            openAddRoomDialog(cx, cy);
        });

        Button exitEditDirectBtn = new Button("✓ Done");
        exitEditDirectBtn.setStyle(
            "-fx-background-color: #34a853; -fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-background-radius: 16px; -fx-padding: 5 14; -fx-cursor: hand; -fx-font-size: 12px;"
        );
        exitEditDirectBtn.setOnAction(e -> setRoomEditMode(false));

        editModeBanner.getChildren().addAll(editBadge, editInstructionLabel, addRoomDirectBtn, exitEditDirectBtn);
        StackPane.setAlignment(editModeBanner, Pos.TOP_CENTER);
        StackPane.setMargin(editModeBanner, new Insets(84, 0, 0, 0));

        // --- 4. Floor Switcher (Google Maps Style floating vertical pill on right side) ---
        floorSwitcherBox = new VBox(8);
        floorSwitcherBox.setAlignment(Pos.CENTER);
        floorSwitcherBox.setPadding(new Insets(8, 6, 8, 6));
        floorSwitcherBox.setEffect(new DropShadow(16, 0, 4, Color.rgb(0, 0, 0, 0.16)));
        floorSwitcherBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        StackPane.setAlignment(floorSwitcherBox, Pos.CENTER_RIGHT);
        StackPane.setMargin(floorSwitcherBox, new Insets(0, 24, 0, 0));

        // --- 5. Floating Zoom Controls (+ / - / Reset / Zoom %) ---
        Button zoomInBtn = createCircleButton("+", "Zoom In (+15%)", () -> zoomRelative(1.15));
        Button zoomOutBtn = createCircleButton("−", "Zoom Out (-15%)", () -> zoomRelative(0.85));
        Button resetZoomBtn = createCircleButton("⊙", "Fit entire floor plan to window", this::fitToScreen);

        zoomPercentLabel = new Label("100%");
        zoomPercentLabel.setStyle(
            "-fx-font-size: 11px; -fx-font-weight: bold; " +
            "-fx-min-width: 46px; -fx-alignment: center;"
        );

        zoomControlsBox = new HBox(6, zoomOutBtn, zoomPercentLabel, zoomInBtn, resetZoomBtn);
        zoomControlsBox.setAlignment(Pos.CENTER);
        zoomControlsBox.setPadding(new Insets(6, 12, 6, 12));
        zoomControlsBox.setEffect(new DropShadow(14, 0, 4, Color.rgb(0, 0, 0, 0.16)));
        zoomControlsBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        StackPane.setAlignment(zoomControlsBox, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(zoomControlsBox, new Insets(0, 24, 24, 0));

        container.getChildren().addAll(viewport, topHeaderBar, editModeBanner, floorSwitcherBox, zoomControlsBox);
        roomHoverTooltip.attachTo(container);

        // Bind Pan and Zoom interactions
        setupPanZoomInteractions();

        // Ensure container is constrained to scene bounds and handles window resizing dynamically
        container.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                container.maxWidthProperty().bind(newScene.widthProperty());
                container.maxHeightProperty().bind(newScene.heightProperty());
                container.prefWidthProperty().bind(newScene.widthProperty());
                container.prefHeightProperty().bind(newScene.heightProperty());

                newScene.widthProperty().addListener((o, ov, nv) -> handleResize());
                newScene.heightProperty().addListener((o, ov, nv) -> handleResize());
            }
        });
        container.widthProperty().addListener((obs, oldVal, newVal) -> handleResize());
        container.heightProperty().addListener((obs, oldVal, newVal) -> handleResize());

        applyTheme(settings != null && settings.isEffectiveDarkMode());
    }

    private void handleResize() {
        if (!container.isVisible()) return;
        double minScale = getMinScale();
        if (floorMapGroup.getScaleX() <= minScale + 0.05) {
            fitToScreen();
        } else {
            clampPosition();
        }
    }

    private Button createCircleButton(String text, String tooltipText, Runnable action) {
        Button btn = new Button(text);
        btn.setPrefSize(34, 34);
        btn.setMinSize(34, 34);
        btn.setMaxSize(34, 34);
        btn.setStyle(
            "-fx-font-size: 15px; -fx-font-weight: bold; " +
            "-fx-background-radius: 17px; -fx-cursor: hand; -fx-padding: 0;"
        );
        Tooltip.install(btn, new Tooltip(tooltipText));
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private boolean isPannable() {
        double viewWidth = getViewWidth();
        double viewHeight = getViewHeight();
        double allowedWidth = Math.max(100.0, viewWidth - GAP_LEFT - GAP_RIGHT);
        double allowedHeight = Math.max(100.0, viewHeight - GAP_TOP - GAP_BOTTOM);
        double scale = floorMapGroup.getScaleX();
        return (mapWidth * scale > allowedWidth + 1.0) || (mapHeight * scale > allowedHeight + 1.0);
    }

    private void setupPanZoomInteractions() {
        final double[] pressPoint = new double[2];
        final boolean[] wasDragged = new boolean[]{false};

        container.setOnMousePressed(event -> {
            pressPoint[0] = event.getSceneX();
            pressPoint[1] = event.getSceneY();
            wasDragged[0] = false;
            if (event.getTarget() == container || event.getTarget() == viewport || event.getTarget() == floorMapGroup) {
                if (isPannable()) {
                    container.setCursor(Cursor.CLOSED_HAND);
                } else {
                    container.setCursor(Cursor.DEFAULT);
                }
                lastMouseX = event.getSceneX();
                lastMouseY = event.getSceneY();
            }
        });

        container.setOnMouseReleased(event -> {
            if (roomEditMode) {
                container.setCursor(Cursor.CROSSHAIR);
            } else if (isPannable()) {
                container.setCursor(Cursor.OPEN_HAND);
            } else {
                container.setCursor(Cursor.DEFAULT);
            }
        });

        container.setOnMouseMoved(event -> {
            if (roomEditMode) {
                container.setCursor(Cursor.CROSSHAIR);
            } else if (isPannable()) {
                container.setCursor(Cursor.OPEN_HAND);
            } else {
                container.setCursor(Cursor.DEFAULT);
            }
        });

        container.setOnMouseDragged(event -> {
            if (roomHoverTooltip != null) {
                roomHoverTooltip.hide();
            }
            if (Math.hypot(event.getSceneX() - pressPoint[0], event.getSceneY() - pressPoint[1]) > 5) {
                wasDragged[0] = true;
            }
            double deltaX = event.getSceneX() - lastMouseX;
            double deltaY = event.getSceneY() - lastMouseY;
            floorMapGroup.setTranslateX(floorMapGroup.getTranslateX() + deltaX);
            floorMapGroup.setTranslateY(floorMapGroup.getTranslateY() + deltaY);
            clampPosition();
            lastMouseX = event.getSceneX();
            lastMouseY = event.getSceneY();
        });

        container.setOnScroll(event -> {
            if (roomHoverTooltip != null) {
                roomHoverTooltip.hide();
            }
            double zoomFactor = (event.getDeltaY() > 0) ? 1.12 : 0.88;
            zoomAtScenePoint(zoomFactor, event.getSceneX(), event.getSceneY());
            event.consume();
        });

        container.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                if (roomEditMode && !wasDragged[0] && event.getClickCount() == 1) {
                    Point2D mapPt = roomLabelsLayer.sceneToMap(event.getSceneX(), event.getSceneY());
                    if (mapPt != null) {
                        openAddRoomDialog(mapPt.getX(), mapPt.getY());
                        return;
                    }
                }
                if (!roomEditMode && event.getClickCount() == 2) {
                    zoomAtScenePoint(1.35, event.getSceneX(), event.getSceneY());
                }
            }
        });
    }

    private double getViewWidth() {
        if (container.getScene() != null && container.getScene().getWidth() > 0) {
            return container.getScene().getWidth();
        }
        if (container.getParent() instanceof Region parent && parent.getWidth() > 0 && parent.getWidth() < 2200) {
            return parent.getWidth();
        }
        if (container.getWidth() > 0 && container.getWidth() < 2200) {
            return container.getWidth();
        }
        return 1200;
    }

    private double getViewHeight() {
        if (container.getScene() != null && container.getScene().getHeight() > 0) {
            return container.getScene().getHeight();
        }
        if (container.getParent() instanceof Region parent && parent.getHeight() > 0 && parent.getHeight() < 1600) {
            return parent.getHeight();
        }
        if (container.getHeight() > 0 && container.getHeight() < 1600) {
            return container.getHeight();
        }
        return 800;
    }

    /**
     * Calculates the maximum zoomed-out scale such that the floor plan fits within
     * the view area while preserving the top/bottom button gaps and side gaps.
     */
    public double getMinScale() {
        double viewWidth = getViewWidth();
        double viewHeight = getViewHeight();
        double allowedWidth = Math.max(100.0, viewWidth - GAP_LEFT - GAP_RIGHT);
        double allowedHeight = Math.max(100.0, viewHeight - GAP_TOP - GAP_BOTTOM);
        return Math.min(allowedWidth / mapWidth, allowedHeight / mapHeight);
    }

    private void zoomAtScenePoint(double factor, double sceneX, double sceneY) {
        double currentScale = floorMapGroup.getScaleX();
        double minScale = getMinScale();
        double maxScale = 4.5;
        double newScale = Math.max(minScale, Math.min(maxScale, currentScale * factor));

        if (Math.abs(newScale - currentScale) > 0.0001) {
            Point2D mouseScene = new Point2D(sceneX, sceneY);
            Point2D mouseLocal = floorMapGroup.sceneToLocal(mouseScene);

            floorMapGroup.setScaleX(newScale);
            floorMapGroup.setScaleY(newScale);
            roomLabelsLayer.setMapScale(newScale);

            Point2D afterScene = floorMapGroup.localToScene(mouseLocal);
            floorMapGroup.setTranslateX(floorMapGroup.getTranslateX() - (afterScene.getX() - mouseScene.getX()));
            floorMapGroup.setTranslateY(floorMapGroup.getTranslateY() - (afterScene.getY() - mouseScene.getY()));

            updateZoomLabel(newScale);
            clampPosition();
        }
    }

    private void zoomRelative(double factor) {
        double centerX = getViewWidth() / 2.0;
        double centerY = getViewHeight() / 2.0;
        zoomAtScenePoint(factor, centerX, centerY);
    }

    private void updateZoomLabel(double scale) {
        if (zoomPercentLabel != null) {
            int pct = (int) Math.round(scale * 100);
            zoomPercentLabel.setText(pct + "%");
        }
    }

    /**
     * Fits the inner map to the screen at maximum zoom-out, centered within
     * the area bounded by the top/bottom button gaps and side gaps.
     */
    public void fitToScreen() {
        double fitScale = getMinScale();

        floorMapGroup.setScaleX(fitScale);
        floorMapGroup.setScaleY(fitScale);
        roomLabelsLayer.setMapScale(fitScale);

        // Center map within the available gap-bounded area
        double baseTx = (GAP_LEFT - GAP_RIGHT) / 2.0;
        double baseTy = (GAP_TOP - GAP_BOTTOM) / 2.0;

        floorMapGroup.setTranslateX(baseTx);
        floorMapGroup.setTranslateY(baseTy);

        updateZoomLabel(fitScale);
        clampPosition();
    }

    /**
     * Clamps map position so it can NEVER be pushed into the button/margin gaps.
     * When maximum zoomed out, scrolling is locked completely (zero scrolling).
     * At higher zoom levels, the map can be panned, but its edges stop strictly at the gap boundaries.
     */
    private void clampPosition() {
        double viewWidth = getViewWidth();
        double viewHeight = getViewHeight();
        if (viewWidth <= 0 || viewHeight <= 0) return;

        double allowedWidth = Math.max(100.0, viewWidth - GAP_LEFT - GAP_RIGHT);
        double allowedHeight = Math.max(100.0, viewHeight - GAP_TOP - GAP_BOTTOM);

        double scale = floorMapGroup.getScaleX();
        double scaledWidth = mapWidth * scale;
        double scaledHeight = mapHeight * scale;

        // Base center offset in viewport to place map center midway within the allowed bounds
        double baseTx = (GAP_LEFT - GAP_RIGHT) / 2.0;
        double baseTy = (GAP_TOP - GAP_BOTTOM) / 2.0;

        // Excess dimensions beyond the allowed area
        double excessX = Math.max(0.0, scaledWidth - allowedWidth);
        double excessY = Math.max(0.0, scaledHeight - allowedHeight);

        // Translation bounds:
        // If scaled <= allowed: excess is 0, minTx == maxTx == baseTx (locked to target center, zero scrolling)
        // If scaled > allowed: map edges stop exactly at the gap borders without entering them
        double minTx = baseTx - excessX / 2.0;
        double maxTx = baseTx + excessX / 2.0;
        double minTy = baseTy - excessY / 2.0;
        double maxTy = baseTy + excessY / 2.0;

        double clampedX = Math.max(minTx, Math.min(maxTx, floorMapGroup.getTranslateX()));
        double clampedY = Math.max(minTy, Math.min(maxTy, floorMapGroup.getTranslateY()));

        floorMapGroup.setTranslateX(clampedX);
        floorMapGroup.setTranslateY(clampedY);
    }

    /**
     * Opens the inner map view for a building by name or alias.
     */
    public boolean openBuilding(String buildingName) {
        return openBuilding(buildingName, null, null);
    }

    /**
     * Opens the inner map view for a building, selecting a specific floor and optionally focusing on a room.
     */
    public boolean openBuilding(String buildingName, String targetFloorId, String highlightRoomId) {
        BuildingInnerMap bim = InnerMapRegistry.findInnerMap(buildingName);
        if (bim == null || bim.getFloors().isEmpty()) {
            return false;
        }

        this.currentBuilding = bim;
        buildingTitleLabel.setText(bim.getBuildingName());

        // Build floor switcher buttons (sorted top floor to bottom floor)
        buildFloorSwitcher();

        // Determine floor to load: targetFloorId if specified, else default or first floor
        FloorPlan floorToSelect = null;
        if (targetFloorId != null) {
            for (FloorPlan fp : bim.getFloors()) {
                if (fp.getFloorId().equalsIgnoreCase(targetFloorId)) {
                    floorToSelect = fp;
                    break;
                }
            }
        }
        if (floorToSelect == null) {
            FloorPlan defaultFloor = bim.getDefaultFloor();
            floorToSelect = defaultFloor != null ? defaultFloor : bim.getFloors().get(0);
        }

        selectFloor(floorToSelect);

        // Immediately fit to screen BEFORE animation begins
        fitToScreen();

        // Animate appearance
        container.setVisible(true);
        container.setManaged(true);
        container.toFront();
        container.setOpacity(0.0);

        FadeTransition fade = new FadeTransition(Duration.millis(200), container);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_OUT);
        fade.play();

        // Re-fit or focus on room after layout completes in next pulse
        if (highlightRoomId != null) {
            javafx.application.Platform.runLater(() -> {
                RoomLabel match = currentRooms.stream()
                    .filter(r -> (r.getId() != null && r.getId().equals(highlightRoomId)) ||
                                (r.getRoomNumber() != null && r.getRoomNumber().equalsIgnoreCase(highlightRoomId)))
                    .findFirst().orElse(null);
                if (match != null) {
                    focusOnRoom(match);
                } else {
                    fitToScreen();
                }
            });
        } else {
            javafx.application.Platform.runLater(this::fitToScreen);
        }
        return true;
    }

    /**
     * Directly opens the building and floor for a given RoomLocation and pulses the room badge.
     */
    public boolean openRoom(RoomLocation loc) {
        if (loc == null || loc.building() == null) return false;
        String bName = loc.building().getBuildingName();
        String floorId = loc.floor() != null ? loc.floor().getFloorId() : null;
        String roomId = loc.room() != null ? loc.room().getId() : null;
        return openBuilding(bName, floorId, roomId);
    }

    /**
     * Centers the viewport smoothly on a specific room and highlights it.
     */
    public void focusOnRoom(RoomLabel room) {
        if (room == null) return;
        double targetScale = Math.max(1.2, getMinScale() * 1.8);
        floorMapGroup.setScaleX(targetScale);
        floorMapGroup.setScaleY(targetScale);
        updateZoomLabel(targetScale);
        roomLabelsLayer.setMapScale(targetScale);

        double baseTx = (GAP_LEFT - GAP_RIGHT) / 2.0;
        double baseTy = (GAP_TOP - GAP_BOTTOM) / 2.0;

        double offsetX = (room.getX() - (mapWidth / 2.0)) * targetScale;
        double offsetY = (room.getY() - (mapHeight / 2.0)) * targetScale;

        floorMapGroup.setTranslateX(baseTx - offsetX);
        floorMapGroup.setTranslateY(baseTy - offsetY);
        clampPosition();

        roomLabelsLayer.highlight(room);
    }

    private void buildFloorSwitcher() {
        floorSwitcherBox.getChildren().clear();
        floorButtons.clear();

        if (currentBuilding == null || currentBuilding.getFloors() == null) return;

        // Sort descending so higher floors appear on top
        List<FloorPlan> sortedFloors = new ArrayList<>(currentBuilding.getFloors());
        sortedFloors.sort((a, b) -> Integer.compare(b.getFloorNumber(), a.getFloorNumber()));

        for (FloorPlan floor : sortedFloors) {
            Button btn = new Button(floor.getShortLabel());
            btn.setPrefSize(42, 42);
            btn.setMinSize(42, 42);
            btn.setMaxSize(42, 42);
            btn.setUserData(floor);
            Tooltip.install(btn, new Tooltip(floor.getFloorName() + (floor.getDescription().isEmpty() ? "" : (" — " + floor.getDescription()))));

            btn.setOnAction(e -> selectFloor(floor));
            floorButtons.add(btn);
            floorSwitcherBox.getChildren().add(btn);
        }

        updateFloorButtonsStyle();
    }

    /**
     * Switches the active floor plan.
     */
    public void selectFloor(FloorPlan floor) {
        if (floor == null) return;
        if (roomHoverTooltip != null) {
            roomHoverTooltip.hide();
        }
        this.currentFloor = floor;

        floorBadge.setText(floor.getFloorName().toUpperCase());
        floorDescLabel.setText(floor.getDescription() != null && !floor.getDescription().isBlank()
            ? floor.getDescription()
            : (currentBuilding.getBuildingName() + " • " + floor.getFloorName()));

        updateFloorButtonsStyle();

        // Load rooms for this floor
        loadCurrentFloorRooms();

        // Load SVG map content
        String svgContent = InnerMapRegistry.loadSvgContent(floor.getMapFile());
        String mapCss = "* { margin: 0; padding: 0; box-sizing: border-box; } " +
                        "html, body { margin: 0 !important; padding: 0 !important; width: 100% !important; height: 100% !important; overflow: hidden !important; background: transparent !important; } " +
                        "::-webkit-scrollbar { display: none !important; width: 0 !important; height: 0 !important; } " +
                        "svg { width: 100% !important; height: 100% !important; display: block !important; }";
        String htmlContent = "<!DOCTYPE html><html><head><style>" + mapCss + "</style></head><body>" + svgContent + "</body></html>";
        floorWebView.getEngine().loadContent(htmlContent, "text/html");

        floorWebView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                applyWebTheme();
            }
        });
    }

    private void loadCurrentFloorRooms() {
        currentRooms.clear();
        if (currentBuilding != null && currentFloor != null) {
            currentRooms.addAll(RoomRegistry.loadRooms(currentBuilding.getBuildingId(), currentFloor.getFloorId()));
        }
        roomLabelsLayer.setRooms(currentRooms);
    }

    private void saveCurrentFloorRooms() {
        if (currentBuilding != null && currentFloor != null) {
            RoomRegistry.saveRooms(currentBuilding.getBuildingId(), currentFloor.getFloorId(), currentRooms);
        }
    }

    private void openAddRoomDialog(double x, double y) {
        String floorTitle = (currentBuilding != null ? currentBuilding.getBuildingName() : "Building") +
                            " • " + (currentFloor != null ? currentFloor.getFloorName() : "Floor");
        EditRoomDialog.show(null, x, y, floorTitle, room -> {
            currentRooms.add(room);
            roomLabelsLayer.setRooms(currentRooms);
            saveCurrentFloorRooms();
        }, null);
    }

    private void openEditRoomDialog(RoomLabel room) {
        if (room == null) return;
        String floorTitle = (currentBuilding != null ? currentBuilding.getBuildingName() : "Building") +
                            " • " + (currentFloor != null ? currentFloor.getFloorName() : "Floor");
        EditRoomDialog.show(room, room.getX(), room.getY(), floorTitle,
            updated -> {
                roomLabelsLayer.setRooms(currentRooms);
                saveCurrentFloorRooms();
            },
            () -> {
                currentRooms.remove(room);
                roomLabelsLayer.setRooms(currentRooms);
                saveCurrentFloorRooms();
            }
        );
    }

    private void handleRoomMoved(RoomLabel room) {
        saveCurrentFloorRooms();
    }

    public void setAdminMode(boolean adminMode) {
        this.adminMode = adminMode;
        if (!adminMode && roomEditMode) {
            setRoomEditMode(false);
        }
        updateEditRoomsBtn();
    }

    public boolean isAdminMode() {
        return adminMode;
    }

    public void setRoomEditMode(boolean editMode) {
        if (roomHoverTooltip != null) {
            roomHoverTooltip.hide();
        }
        this.roomEditMode = editMode;
        editModeBanner.setVisible(editMode);
        editModeBanner.setManaged(editMode);
        roomLabelsLayer.setEditMode(editMode);
        updateEditRoomsBtn();
        if (editMode) {
            container.setCursor(Cursor.CROSSHAIR);
        } else {
            container.setCursor(Cursor.DEFAULT);
        }
    }

    public boolean isRoomEditMode() {
        return roomEditMode;
    }

    public void setOnAdminLoginRequested(Runnable r) {
        this.onAdminLoginRequested = r;
    }

    private void updateEditRoomsBtn() {
        if (editRoomsBtn == null) return;
        if (!adminMode) {
            editRoomsBtn.setText("🏷️ Label Rooms (Admin)");
            editRoomsBtn.setStyle(
                "-fx-font-size: 12px; -fx-font-weight: bold; " +
                "-fx-background-color: " + (isDark ? "#3c4043;" : "#f1f3f4;") +
                "-fx-text-fill: " + (isDark ? "#8ab4f8;" : "#1a73e8;") +
                "-fx-background-radius: 18px; -fx-cursor: hand; -fx-padding: 6 12; " +
                "-fx-border-color: " + (isDark ? "#5f6368;" : "#dadce0;") +
                "-fx-border-radius: 18px; -fx-border-width: 1px;"
            );
            Tooltip.install(editRoomsBtn, new Tooltip("Login as Admin to label and manage floor rooms"));
        } else if (roomEditMode) {
            editRoomsBtn.setText("✓ Done Editing");
            editRoomsBtn.setStyle(
                "-fx-font-size: 12px; -fx-font-weight: bold; " +
                "-fx-background-color: #34a853; -fx-text-fill: white; " +
                "-fx-background-radius: 18px; -fx-cursor: hand; -fx-padding: 6 14;"
            );
            Tooltip.install(editRoomsBtn, new Tooltip("Exit room editing mode"));
        } else {
            editRoomsBtn.setText("✏️ Edit Floor Labels");
            editRoomsBtn.setStyle(
                "-fx-font-size: 12px; -fx-font-weight: bold; " +
                "-fx-background-color: #1a73e8; -fx-text-fill: white; " +
                "-fx-background-radius: 18px; -fx-cursor: hand; -fx-padding: 6 14;"
            );
            Tooltip.install(editRoomsBtn, new Tooltip("Enter edit mode to add, move, or rename rooms"));
        }
    }

    private void updateFloorButtonsStyle() {
        for (Button btn : floorButtons) {
            FloorPlan fp = (FloorPlan) btn.getUserData();
            boolean isActive = currentFloor != null && fp.getFloorId().equalsIgnoreCase(currentFloor.getFloorId());

            if (isActive) {
                btn.setStyle(
                    "-fx-background-color: #1a73e8; " +
                    "-fx-text-fill: #ffffff; " +
                    "-fx-font-size: 14px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-background-radius: 21px; " +
                    "-fx-cursor: hand;"
                );
            } else {
                btn.setStyle(
                    "-fx-background-color: " + (isDark ? "#303134;" : "#ffffff;") +
                    "-fx-text-fill: " + (isDark ? "#e8eaed;" : "#3c4043;") +
                    "-fx-font-size: 13px; " +
                    "-fx-font-weight: 500; " +
                    "-fx-background-radius: 21px; " +
                    "-fx-border-color: " + (isDark ? "#5f6368;" : "#dadce0;") +
                    "-fx-border-radius: 21px; " +
                    "-fx-border-width: 1px; " +
                    "-fx-cursor: hand;"
                );
            }
        }
    }

    public void close() {
        if (!container.isVisible()) return;
        if (roomHoverTooltip != null) {
            roomHoverTooltip.hide();
        }
        if (roomEditMode) {
            setRoomEditMode(false);
        }

        FadeTransition fade = new FadeTransition(Duration.millis(200), container);
        fade.setFromValue(container.getOpacity());
        fade.setToValue(0.0);
        fade.setInterpolator(Interpolator.EASE_IN);
        fade.setOnFinished(e -> {
            container.setVisible(false);
            container.setManaged(false);
            if (onBackCallback != null) {
                onBackCallback.run();
            }
        });
        fade.play();
    }

    public boolean isShowing() {
        return container.isVisible();
    }

    public void setOnBack(Runnable callback) {
        this.onBackCallback = callback;
    }

    public void applyTheme(boolean isDark) {
        this.isDark = isDark;
        container.setStyle(isDark ? "-fx-background-color: #1a1d24;" : "-fx-background-color: #f1f3f4;");

        roomLabelsLayer.applyTheme(isDark);
        if (roomHoverTooltip != null) {
            roomHoverTooltip.applyTheme(isDark);
        }
        updateEditRoomsBtn();

        if (editModeBanner != null) {
            editModeBanner.setStyle(
                "-fx-background-color: " + (isDark ? "rgba(32, 33, 36, 0.96);" : "rgba(255, 255, 255, 0.96);") +
                "-fx-background-radius: 20px; " +
                "-fx-border-color: " + (isDark ? "#3c4043;" : "#dadce0;") +
                "-fx-border-radius: 20px; -fx-border-width: 1px;"
            );
        }
        if (editInstructionLabel != null) {
            editInstructionLabel.setTextFill(Color.web(isDark ? "#e8eaed" : "#202124"));
        }

        // Top bar
        topHeaderBar.setStyle(
            "-fx-background-color: " + (isDark ? "rgba(32, 33, 36, 0.95);" : "rgba(255, 255, 255, 0.95);") +
            "-fx-background-radius: 16px; " +
            "-fx-border-color: " + (isDark ? "#3c4043;" : "#dadce0;") +
            "-fx-border-radius: 16px; -fx-border-width: 1px;"
        );
        buildingTitleLabel.setTextFill(Color.web(isDark ? "#e8eaed" : "#202124"));
        floorDescLabel.setTextFill(Color.web(isDark ? "#9aa0a6" : "#5f6368"));

        floorBadge.setStyle(
            "-fx-background-color: " + (isDark ? "#173154;" : "#e8f0fe;") +
            "-fx-text-fill: " + (isDark ? "#8ab4f8;" : "#1a73e8;") +
            "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 8; " +
            "-fx-background-radius: 10px;"
        );

        backBtn.setStyle(
            "-fx-background-color: " + (isDark ? "#303134;" : "#e8f0fe;") +
            "-fx-text-fill: " + (isDark ? "#e8eaed;" : "#1a73e8;") +
            "-fx-font-size: 13px; -fx-font-weight: bold; " +
            "-fx-background-radius: 20px; -fx-cursor: hand; -fx-padding: 7 14;"
        );

        // Floor switcher box
        floorSwitcherBox.setStyle(
            "-fx-background-color: " + (isDark ? "rgba(32, 33, 36, 0.95);" : "rgba(255, 255, 255, 0.95);") +
            "-fx-background-radius: 24px; " +
            "-fx-border-color: " + (isDark ? "#3c4043;" : "#dadce0;") +
            "-fx-border-radius: 24px; -fx-border-width: 1px;"
        );

        // Zoom controls
        zoomControlsBox.setStyle(
            "-fx-background-color: " + (isDark ? "rgba(32, 33, 36, 0.95);" : "rgba(255, 255, 255, 0.95);") +
            "-fx-background-radius: 20px; " +
            "-fx-border-color: " + (isDark ? "#3c4043;" : "#dadce0;") +
            "-fx-border-radius: 20px; -fx-border-width: 1px;"
        );

        if (zoomPercentLabel != null) {
            zoomPercentLabel.setTextFill(Color.web(isDark ? "#e8eaed" : "#3c4043"));
        }

        for (javafx.scene.Node n : zoomControlsBox.getChildren()) {
            if (n instanceof Button) {
                n.setStyle(
                    "-fx-background-color: " + (isDark ? "#303134;" : "#f1f3f4;") +
                    "-fx-text-fill: " + (isDark ? "#e8eaed;" : "#3c4043;") +
                    "-fx-font-size: 15px; -fx-font-weight: bold; " +
                    "-fx-background-radius: 17px; -fx-cursor: hand;"
                );
            }
        }

        updateFloorButtonsStyle();
        applyWebTheme();
    }

    private void applyWebTheme() {
        try {
            if (isDark) {
                floorWebView.getEngine().executeScript(
                    "if (document.documentElement) document.documentElement.style.filter = 'invert(0.92) hue-rotate(180deg) brightness(0.9) contrast(1.15) saturate(1.2)';" +
                    "if (document.body) document.body.style.backgroundColor = 'transparent';"
                );
            } else {
                floorWebView.getEngine().executeScript(
                    "if (document.documentElement) document.documentElement.style.filter = 'none';" +
                    "if (document.body) document.body.style.backgroundColor = 'transparent';"
                );
            }
        } catch (Exception ignored) {}
    }

    public StackPane getContainer() {
        return container;
    }
}
