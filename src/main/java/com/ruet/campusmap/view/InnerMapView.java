package com.ruet.campusmap.view;

import com.ruet.campusmap.model.AppSettings;
import com.ruet.campusmap.model.BuildingInnerMap;
import com.ruet.campusmap.model.FloorPlan;
import com.ruet.campusmap.service.InnerMapRegistry;
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
 * free panning, multi-floor switching, and responsive dark/light theme integration.
 */
public class InnerMapView {

    private final StackPane container;
    private final AppSettings settings;
    private final WebView floorWebView;
    private final Group floorMapGroup;
    private final StackPane viewport;

    private final Label buildingTitleLabel;
    private final Label floorBadge;
    private final Label floorDescLabel;
    private final HBox topHeaderBar;
    private final VBox floorSwitcherBox;
    private final HBox zoomControlsBox;
    private final Label zoomPercentLabel;
    private final Button backBtn;

    private final List<Button> floorButtons = new ArrayList<>();
    private BuildingInnerMap currentBuilding;
    private FloorPlan currentFloor;
    private Runnable onBackCallback;

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

        // --- 1. Floor Plan SVG Web Engine View ---
        floorWebView = new WebView();
        floorWebView.setPageFill(Color.TRANSPARENT);
        floorWebView.setPrefSize(mapWidth, mapHeight);
        floorWebView.setMinSize(mapWidth, mapHeight);
        floorWebView.setMaxSize(mapWidth, mapHeight);
        floorWebView.setMouseTransparent(true);

        floorWebView.getChildrenUnmodifiable().addListener((javafx.collections.ListChangeListener<javafx.scene.Node>) change -> {
            for (javafx.scene.Node node : floorWebView.lookupAll(".scroll-bar")) {
                node.setVisible(false);
                node.setManaged(false);
            }
        });

        floorMapGroup = new Group(floorWebView);
        viewport = new StackPane(floorMapGroup);
        viewport.setStyle("-fx-background-color: transparent;");

        // --- 2. Top Header Bar (Back button + Building info + Floor description) ---
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

        topHeaderBar = new HBox(14, backBtn, titleDetails, headerSpacer);
        topHeaderBar.setAlignment(Pos.CENTER_LEFT);
        topHeaderBar.setPadding(new Insets(10, 18, 10, 14));
        topHeaderBar.setEffect(new DropShadow(16, 0, 4, Color.rgb(0, 0, 0, 0.16)));
        topHeaderBar.setMaxHeight(Region.USE_PREF_SIZE);

        StackPane.setAlignment(topHeaderBar, Pos.TOP_LEFT);
        StackPane.setMargin(topHeaderBar, new Insets(18, 20, 0, 20));

        // --- 3. Floor Switcher (Google Maps Style floating vertical pill on right side) ---
        floorSwitcherBox = new VBox(8);
        floorSwitcherBox.setAlignment(Pos.CENTER);
        floorSwitcherBox.setPadding(new Insets(8, 6, 8, 6));
        floorSwitcherBox.setEffect(new DropShadow(16, 0, 4, Color.rgb(0, 0, 0, 0.16)));
        floorSwitcherBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        StackPane.setAlignment(floorSwitcherBox, Pos.CENTER_RIGHT);
        StackPane.setMargin(floorSwitcherBox, new Insets(0, 24, 0, 0));

        // --- 4. Floating Zoom Controls (+ / - / Reset / Zoom %) ---
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

        container.getChildren().addAll(viewport, topHeaderBar, floorSwitcherBox, zoomControlsBox);

        // Bind Pan and Zoom interactions
        setupPanZoomInteractions();

        // Responsive resize listeners: keep map clamped within view boundaries and maintain min scale
        container.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (container.isVisible() && newVal.doubleValue() > 0) {
                double minScale = getMinScale();
                if (floorMapGroup.getScaleX() < minScale) {
                    floorMapGroup.setScaleX(minScale);
                    floorMapGroup.setScaleY(minScale);
                    updateZoomLabel(minScale);
                }
                clampPosition();
            }
        });
        container.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (container.isVisible() && newVal.doubleValue() > 0) {
                double minScale = getMinScale();
                if (floorMapGroup.getScaleX() < minScale) {
                    floorMapGroup.setScaleX(minScale);
                    floorMapGroup.setScaleY(minScale);
                    updateZoomLabel(minScale);
                }
                clampPosition();
            }
        });

        applyTheme(settings != null && settings.isEffectiveDarkMode());
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
        Bounds bounds = floorMapGroup.getBoundsInParent();
        return (bounds.getWidth() > allowedWidth + 1.0) || (bounds.getHeight() > allowedHeight + 1.0);
    }

    private void setupPanZoomInteractions() {
        container.setOnMousePressed(event -> {
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
            if (isPannable()) {
                container.setCursor(Cursor.OPEN_HAND);
            } else {
                container.setCursor(Cursor.DEFAULT);
            }
        });

        container.setOnMouseMoved(event -> {
            if (isPannable()) {
                container.setCursor(Cursor.OPEN_HAND);
            } else {
                container.setCursor(Cursor.DEFAULT);
            }
        });

        container.setOnMouseDragged(event -> {
            double deltaX = event.getSceneX() - lastMouseX;
            double deltaY = event.getSceneY() - lastMouseY;
            floorMapGroup.setTranslateX(floorMapGroup.getTranslateX() + deltaX);
            floorMapGroup.setTranslateY(floorMapGroup.getTranslateY() + deltaY);
            clampPosition();
            lastMouseX = event.getSceneX();
            lastMouseY = event.getSceneY();
        });

        container.setOnScroll(event -> {
            double zoomFactor = (event.getDeltaY() > 0) ? 1.12 : 0.88;
            zoomAtScenePoint(zoomFactor, event.getSceneX(), event.getSceneY());
            event.consume();
        });

        container.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                zoomAtScenePoint(1.35, event.getSceneX(), event.getSceneY());
            }
        });
    }

    private double getViewWidth() {
        if (container.getWidth() > 0) return container.getWidth();
        if (container.getScene() != null && container.getScene().getWidth() > 0) return container.getScene().getWidth();
        if (container.getParent() instanceof Region) {
            double pw = ((Region) container.getParent()).getWidth();
            if (pw > 0) return pw;
        }
        return 1200;
    }

    private double getViewHeight() {
        if (container.getHeight() > 0) return container.getHeight();
        if (container.getScene() != null && container.getScene().getHeight() > 0) return container.getScene().getHeight();
        if (container.getParent() instanceof Region) {
            double ph = ((Region) container.getParent()).getHeight();
            if (ph > 0) return ph;
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

        // Center map within the available gap-bounded area using boundsInParent
        Bounds bounds = floorMapGroup.getBoundsInParent();
        double currentCenterX = (bounds.getMinX() + bounds.getMaxX()) / 2.0;
        double currentCenterY = (bounds.getMinY() + bounds.getMaxY()) / 2.0;

        double viewWidth = getViewWidth();
        double viewHeight = getViewHeight();
        double allowedWidth = Math.max(100.0, viewWidth - GAP_LEFT - GAP_RIGHT);
        double allowedHeight = Math.max(100.0, viewHeight - GAP_TOP - GAP_BOTTOM);

        double targetCenterX = GAP_LEFT + allowedWidth / 2.0;
        double targetCenterY = GAP_TOP + allowedHeight / 2.0;

        double currentTx = floorMapGroup.getTranslateX();
        double currentTy = floorMapGroup.getTranslateY();
        floorMapGroup.setTranslateX(currentTx + (targetCenterX - currentCenterX));
        floorMapGroup.setTranslateY(currentTy + (targetCenterY - currentCenterY));

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

        Bounds bounds = floorMapGroup.getBoundsInParent();
        double scaledWidth = bounds.getWidth();
        double scaledHeight = bounds.getHeight();

        double currentCenterX = (bounds.getMinX() + bounds.getMaxX()) / 2.0;
        double currentCenterY = (bounds.getMinY() + bounds.getMaxY()) / 2.0;

        double targetCenterX = GAP_LEFT + allowedWidth / 2.0;
        double targetCenterY = GAP_TOP + allowedHeight / 2.0;

        // Base translation that places the map center at the target center
        double currentTx = floorMapGroup.getTranslateX();
        double currentTy = floorMapGroup.getTranslateY();
        double baseTx = currentTx + (targetCenterX - currentCenterX);
        double baseTy = currentTy + (targetCenterY - currentCenterY);

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

        double clampedX = Math.max(minTx, Math.min(maxTx, currentTx));
        double clampedY = Math.max(minTy, Math.min(maxTy, currentTy));

        floorMapGroup.setTranslateX(clampedX);
        floorMapGroup.setTranslateY(clampedY);
    }

    /**
     * Opens the inner map view for a building by name or alias.
     */
    public boolean openBuilding(String buildingName) {
        BuildingInnerMap bim = InnerMapRegistry.findInnerMap(buildingName);
        if (bim == null || bim.getFloors().isEmpty()) {
            return false;
        }

        this.currentBuilding = bim;
        buildingTitleLabel.setText(bim.getBuildingName());

        // Build floor switcher buttons (sorted top floor to bottom floor)
        buildFloorSwitcher();

        // Load default or first floor
        FloorPlan defaultFloor = bim.getDefaultFloor();
        selectFloor(defaultFloor != null ? defaultFloor : bim.getFloors().get(0));

        // Immediately fit to screen BEFORE animation begins so it never shows at 1.0!
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

        // Re-fit after layout completes in next pulse
        javafx.application.Platform.runLater(this::fitToScreen);
        return true;
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
        this.currentFloor = floor;

        floorBadge.setText(floor.getFloorName().toUpperCase());
        floorDescLabel.setText(floor.getDescription() != null && !floor.getDescription().isBlank()
            ? floor.getDescription()
            : (currentBuilding.getBuildingName() + " • " + floor.getFloorName()));

        updateFloorButtonsStyle();

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
