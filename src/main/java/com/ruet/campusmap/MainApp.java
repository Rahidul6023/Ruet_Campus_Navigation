package com.ruet.campusmap;

import com.ruet.campusmap.editor.AdminLoginDialog;
import com.ruet.campusmap.editor.MapEditorManager;
import com.ruet.campusmap.model.AppSettings;
import com.ruet.campusmap.model.BuildingPolygon;
import com.ruet.campusmap.service.PolygonDataLoader;
import com.ruet.campusmap.view.BuildingHoverTooltip;
import com.ruet.campusmap.view.BuildingInfoCard;
import com.ruet.campusmap.view.BuildingLabelsLayer;
import com.ruet.campusmap.view.CampusBrandBadge;
import com.ruet.campusmap.view.MapPoiLayer;
import com.ruet.campusmap.view.SettingsCard;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class MainApp extends Application {

    private double lastMouseX;
    private double lastMouseY;

    @Override
    public void start(Stage stage) {
        double mapWidth = 4190;
        double mapHeight = 1720;
        double viewScaleFactor = 2.0;
        double webViewWidth = mapWidth / viewScaleFactor;
        double webViewHeight = mapHeight / viewScaleFactor;

        // Load SVG map resource
        String svgContent = "";
        java.io.InputStream stream = getClass().getResourceAsStream("/maps/ruet-campus-map-refined-v2.svg");
        if (stream == null) {
            stream = getClass().getResourceAsStream("/maps/Ruet_academic_campus.svg");
        }
        final java.io.InputStream mapStream = stream;
        if (mapStream != null) {
            try (mapStream) {
                svgContent = new String(mapStream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Settings Model
        AppSettings settings = new AppSettings();

        // SVG Map Integration via HTML5 wrapper (ensures document.body exists and responsive scaling)
        WebView campusView = new WebView();
        campusView.setPageFill(Color.TRANSPARENT);
        String mapCss = "* { margin: 0; padding: 0; box-sizing: border-box; } " +
                        "html, body { margin: 0 !important; padding: 0 !important; width: 100% !important; height: 100% !important; overflow: hidden !important; background: transparent !important; } " +
                        "::-webkit-scrollbar { display: none !important; width: 0 !important; height: 0 !important; } " +
                        "svg { width: 100% !important; height: 100% !important; display: block !important; }";
        String htmlContent = "<!DOCTYPE html><html><head><style>" + mapCss + "</style></head><body>" + svgContent + "</body></html>";
        campusView.getEngine().loadContent(htmlContent, "text/html");

        campusView.getChildrenUnmodifiable().addListener((javafx.collections.ListChangeListener<javafx.scene.Node>) change -> {
            for (javafx.scene.Node node : campusView.lookupAll(".scroll-bar")) {
                node.setVisible(false);
                node.setManaged(false);
            }
        });

        // Sized safely within GPU max texture limits (clamped to 4096 in D3D Prism) then scaled to 4190x1720
        campusView.setPrefSize(webViewWidth, webViewHeight);
        campusView.setMinSize(webViewWidth, webViewHeight);
        campusView.setMaxSize(webViewWidth, webViewHeight);
        campusView.getTransforms().setAll(new javafx.scene.transform.Scale(viewScaleFactor, viewScaleFactor, 0, 0));
        campusView.setMouseTransparent(true);

        // Creating Polygon layer
        Pane polygonLayer = new Pane();
        polygonLayer.setPrefSize(mapWidth, mapHeight);
        polygonLayer.setMinSize(mapWidth, mapHeight);
        polygonLayer.setMaxSize(mapWidth, mapHeight);

        // Modern floating building info card (Bottom-Left)
        BuildingInfoCard buildingInfoCard = new BuildingInfoCard();

        // Interactive Building Labels Layer (only displays authentic hitbox badges)
        BuildingLabelsLayer buildingLabelsLayer = new BuildingLabelsLayer(settings, buildingInfoCard::showBuilding);

        // Interactive POI Layer (Food, Printers, Restrooms, Parking, Accessibility, Shuttle)
        MapPoiLayer mapPoiLayer = new MapPoiLayer(settings, (name, category, description) -> {
            buildingInfoCard.showPoi(name, category, description);
        });

        // Group containing all map elements that scale and pan together
        Group mapGroup = new Group(campusView, polygonLayer, mapPoiLayer.getContainer(), buildingLabelsLayer.getContainer());
        StackPane root = new StackPane(mapGroup);

        // Modular Map Editor Manager
        MapEditorManager editorManager = new MapEditorManager(root, polygonLayer, clickedBp -> {
            buildingInfoCard.showBuilding(clickedBp);
        }, buildingLabelsLayer);

        // Load and display all saved building polygons from campus.json
        List<BuildingPolygon> initialBuildings = PolygonDataLoader.loadBuildingPolygons();
        for (BuildingPolygon bp : initialBuildings) {
            polygonLayer.getChildren().add(PolygonDataLoader.createJavaFXPolygon(bp, (clickedBp, poly) -> {
                editorManager.handlePolygonClick(clickedBp, poly);
            }));
            buildingLabelsLayer.addPolygonLabel(bp);
        }

        // RUET Campus Brand Badge with Logo SVG (Top-Left)
        CampusBrandBadge brandBadge = new CampusBrandBadge();

        // Google Maps-style Search Bar (Modular & centered at top)
        MapSearchBar searchBar = new MapSearchBar(mapGroup, root);
        searchBar.registerBuildings(initialBuildings);

        // When a teacher is selected from search -> Display teacher card & fly to their building
        searchBar.setOnTeacherSelected(teacher -> {
            buildingInfoCard.showTeacher(teacher, () -> {
                if (teacher.getBuildingName() != null && !teacher.getBuildingName().isBlank()) {
                    searchBar.flyToLocation(teacher.getBuildingName());
                }
            });
        });

        // Sync real-time building modifications between editor and search bar
        editorManager.setOnBuildingCreated(searchBar::addOrUpdateBuilding);
        editorManager.setOnBuildingUpdated(searchBar::addOrUpdateBuilding);
        editorManager.setOnBuildingDeleted(searchBar::removeBuilding);

        // When a location is selected from search -> Display building card
        searchBar.setOnLocationSelected(locationName -> {
            for (BuildingPolygon bp : editorManager.getSavedBuildings()) {
                if (bp.getName() != null && bp.getName().equalsIgnoreCase(locationName)) {
                    buildingInfoCard.showBuilding(bp);
                    return;
                }
            }
            buildingInfoCard.showPoi(locationName, "Campus Landmark", "RUET Campus Facilities");
        });
        
        // Floating Settings & Admin Buttons (Top-Right)
        MapActionButtons actionButtons = new MapActionButtons();

        // Floating Zoom In/Out & Location Controls (Bottom-Center)
        MapBottomControls bottomControls = new MapBottomControls(mapGroup);

        // Floating Settings Card
        SettingsCard settingsCard = new SettingsCard(settings, stage, root);
        settingsCard.setOnClose(() -> actionButtons.setSettingsActive(false));

        root.getChildren().addAll(
            brandBadge.getContainer(),
            searchBar.getContainer(),
            actionButtons.getContainer(),
            bottomControls.getContainer(),
            buildingInfoCard.getContainer(),
            settingsCard.getContainer()
        );

        // Attach in-scene mouse-transparent building hover tooltip
        BuildingHoverTooltip.getInstance().attachTo(root);

        // When Settings Button is clicked -> Toggle floating settings card
        actionButtons.setOnSettingsAction(() -> {
            settingsCard.toggle();
            actionButtons.setSettingsActive(settingsCard.isVisible());
        });

        // When Admin Button is clicked -> Show password modal -> Activate editor on success
        actionButtons.setOnAdminAction(() -> {
            if (settingsCard.isVisible()) {
                settingsCard.hide();
                actionButtons.setSettingsActive(false);
            }
            AdminLoginDialog.show(stage, () -> {
                editorManager.activate();
            });
        });

        // Theme management reactive listener
        Runnable applyThemeState = () -> {
            boolean isDark = settings.isEffectiveDarkMode();

            root.setStyle(isDark ? "-fx-background-color: #1a1d24;" : "-fx-background-color: #f5f7f2;");

            try {
                if (isDark) {
                    campusView.getEngine().executeScript(
                        "if (document.documentElement) document.documentElement.style.filter = 'invert(0.92) hue-rotate(180deg) brightness(0.9) contrast(1.15) saturate(1.2)';" +
                        "if (document.body) document.body.style.backgroundColor = 'transparent';"
                    );
                } else {
                    campusView.getEngine().executeScript(
                        "if (document.documentElement) document.documentElement.style.filter = 'none';" +
                        "if (document.body) document.body.style.backgroundColor = 'transparent';"
                    );
                }
            } catch (Exception ignored) {}

            searchBar.applyTheme(isDark);
            actionButtons.applyTheme(isDark);
            bottomControls.applyTheme(isDark);
            buildingInfoCard.applyTheme(isDark);
            buildingLabelsLayer.refresh();
            mapPoiLayer.refresh();
            settingsCard.applyTheme(isDark);
            editorManager.applyTheme(isDark);
            BuildingHoverTooltip.getInstance().applyTheme(isDark);
        };

        // Wire settings model changes
        settings.addListener(applyThemeState);

        // Run theme apply when SVG map finishes loading
        campusView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                try {
                    campusView.getEngine().executeScript("if (document.body) document.body.style.overflow='hidden';");
                } catch (Exception ignored) {}
                applyThemeState.run();
            }
        });

        // Clamps map position so it can NEVER be dragged away into empty void (zero margin, full-screen map)
        Runnable clampMapPosition = () -> {
            double viewWidth = root.getWidth();
            double viewHeight = root.getHeight();
            if (viewWidth <= 0 || viewHeight <= 0) return;

            double scale = mapGroup.getScaleX();
            double scaledWidth = mapWidth * scale;
            double scaledHeight = mapHeight * scale;

            double halfExcessX = Math.max(0.0, (scaledWidth - viewWidth) / 2.0);
            double halfExcessY = Math.max(0.0, (scaledHeight - viewHeight) / 2.0);

            // Zero margin: map edges stop exactly at screen edges, never floating like a box
            double minTx = -halfExcessX;
            double maxTx = halfExcessX;
            double minTy = -halfExcessY;
            double maxTy = halfExcessY;

            double clampedX = Math.max(minTx, Math.min(maxTx, mapGroup.getTranslateX()));
            double clampedY = Math.max(minTy, Math.min(maxTy, mapGroup.getTranslateY()));

            mapGroup.setTranslateX(clampedX);
            mapGroup.setTranslateY(clampedY);
        };

        // Sets default zoom: average between minimum (height fit) and maximum zoom
        final boolean[] hasCentered = new boolean[]{false};
        Runnable centerAndFitMap = () -> {
            double viewWidth = root.getWidth();
            double viewHeight = root.getHeight();
            if (viewWidth <= 0 || viewHeight <= 0) return;

            // Maximum zoomed out state: up and bottom sides fit properly in the screen
            double minScale = Math.max(viewHeight / mapHeight, viewWidth / mapWidth);
            double maxScale = 3.5;

            // Comfortable campus overview zoom (a little bit zoomed out from previous close zoom)
            double defaultScale = minScale + (maxScale - minScale) * 0.22;

            // Focus on RUET core academic & administrative hub
            double focusX = 1100.0;
            double focusY = 860.0;
            double targetTx = (mapWidth / 2.0 - focusX) * defaultScale;
            double targetTy = (mapHeight / 2.0 - focusY) * defaultScale;

            mapGroup.setScaleX(defaultScale);
            mapGroup.setScaleY(defaultScale);
            mapGroup.setTranslateX(targetTx);
            mapGroup.setTranslateY(targetTy);

            clampMapPosition.run();
            hasCentered[0] = true;
        };

        root.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (!hasCentered[0] && newVal.doubleValue() > 0 && root.getHeight() > 0) {
                Platform.runLater(centerAndFitMap);
            } else if (newVal.doubleValue() > 0 && root.getHeight() > 0) {
                double minScale = Math.max(root.getHeight() / mapHeight, newVal.doubleValue() / mapWidth);
                if (mapGroup.getScaleX() < minScale) {
                    mapGroup.setScaleX(minScale);
                    mapGroup.setScaleY(minScale);
                }
                clampMapPosition.run();
            }
        });
        root.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (!hasCentered[0] && newVal.doubleValue() > 0 && root.getWidth() > 0) {
                Platform.runLater(centerAndFitMap);
            } else if (newVal.doubleValue() > 0 && root.getWidth() > 0) {
                double minScale = Math.max(newVal.doubleValue() / mapHeight, root.getWidth() / mapWidth);
                if (mapGroup.getScaleX() < minScale) {
                    mapGroup.setScaleX(minScale);
                    mapGroup.setScaleY(minScale);
                }
                clampMapPosition.run();
            }
        });

        // Map Panning and dismiss settings card on click-outside
        root.setOnMousePressed(event -> {
            BuildingHoverTooltip.getInstance().hide();
            if (settingsCard.isVisible()) {
                settingsCard.hide();
                actionButtons.setSettingsActive(false);
            }
            if (!editorManager.isDrawMode() || event.isSecondaryButtonDown()) {
                root.setCursor(Cursor.CLOSED_HAND);
            }
            lastMouseX = event.getSceneX();
            lastMouseY = event.getSceneY();
        });

        root.setOnMouseReleased(event -> {
            if (!editorManager.isDrawMode()) {
                root.setCursor(Cursor.DEFAULT);
            }
        });

        root.setOnMouseDragged(event -> {
            // Only pan if NOT in draw mode or if dragging with right-click
            if (!editorManager.isDrawMode() || event.isSecondaryButtonDown()) {
                double deltaX = event.getSceneX() - lastMouseX;
                double deltaY = event.getSceneY() - lastMouseY;
                mapGroup.setTranslateX(mapGroup.getTranslateX() + deltaX);
                mapGroup.setTranslateY(mapGroup.getTranslateY() + deltaY);
                clampMapPosition.run();
            }

            lastMouseX = event.getSceneX();
            lastMouseY = event.getSceneY();
        });

        // Smooth cursor-centered focal zooming with boundary clamping
        root.setOnScroll(event -> {
            BuildingHoverTooltip.getInstance().hide();
            double viewWidth = root.getWidth();
            double viewHeight = root.getHeight();
            if (viewWidth <= 0 || viewHeight <= 0) return;

            // Maximum zoomed out state: up and bottom sides fit properly in the screen
            double minScale = Math.max(viewHeight / mapHeight, viewWidth / mapWidth);
            double maxScale = 3.5;

            double zoomFactor = (event.getDeltaY() > 0) ? 1.12 : 0.88;
            double currentScale = mapGroup.getScaleX();
            double newScale = currentScale * zoomFactor;

            // Clamped so user can NEVER zoom out beyond the maximum zoom-out limit (minScale)
            if (newScale < minScale) {
                newScale = minScale;
            }
            if (newScale > maxScale) {
                newScale = maxScale;
            }

            if (Math.abs(newScale - currentScale) > 0.0001) {
                Point2D mouseScene = new Point2D(event.getSceneX(), event.getSceneY());
                Point2D mouseLocal = mapGroup.sceneToLocal(mouseScene);

                mapGroup.setScaleX(newScale);
                mapGroup.setScaleY(newScale);

                Point2D afterScene = mapGroup.localToScene(mouseLocal);
                mapGroup.setTranslateX(mapGroup.getTranslateX() - (afterScene.getX() - mouseScene.getX()));
                mapGroup.setTranslateY(mapGroup.getTranslateY() - (afterScene.getY() - mouseScene.getY()));

                clampMapPosition.run();
            }

            event.consume();
        });

        // Double-click to smoothly zoom in towards cursor position
        root.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY && !editorManager.isDrawMode()) {
                double viewWidth = root.getWidth();
                double viewHeight = root.getHeight();
                double minScale = Math.max(viewHeight / mapHeight, viewWidth / mapWidth);
                double maxScale = 3.5;

                double currentScale = mapGroup.getScaleX();
                double targetScale = Math.min(maxScale, currentScale * 1.5);
                if (Math.abs(targetScale - currentScale) > 0.001) {
                    Point2D mouseScene = new Point2D(event.getSceneX(), event.getSceneY());
                    Point2D mouseLocal = mapGroup.sceneToLocal(mouseScene);

                    mapGroup.setScaleX(targetScale);
                    mapGroup.setScaleY(targetScale);
                    Point2D afterScene = mapGroup.localToScene(mouseLocal);
                    double targetTx = mapGroup.getTranslateX() - (afterScene.getX() - mouseScene.getX());
                    double targetTy = mapGroup.getTranslateY() - (afterScene.getY() - mouseScene.getY());

                    double scaledWidth = mapWidth * targetScale;
                    double scaledHeight = mapHeight * targetScale;
                    double halfExcessX = Math.max(0.0, (scaledWidth - viewWidth) / 2.0);
                    double halfExcessY = Math.max(0.0, (scaledHeight - viewHeight) / 2.0);
                    targetTx = Math.max(-halfExcessX, Math.min(halfExcessX, targetTx));
                    targetTy = Math.max(-halfExcessY, Math.min(halfExcessY, targetTy));

                    mapGroup.setScaleX(currentScale);
                    mapGroup.setScaleY(currentScale);

                    Timeline zoomTimeline = new Timeline(
                        new KeyFrame(Duration.millis(250),
                            new KeyValue(mapGroup.scaleXProperty(), targetScale, Interpolator.EASE_OUT),
                            new KeyValue(mapGroup.scaleYProperty(), targetScale, Interpolator.EASE_OUT),
                            new KeyValue(mapGroup.translateXProperty(), targetTx, Interpolator.EASE_OUT),
                            new KeyValue(mapGroup.translateYProperty(), targetTy, Interpolator.EASE_OUT)
                        )
                    );
                    zoomTimeline.play();
                }
            }
        });

        Scene scene = new Scene(root, 1200, 800);

        // Dismiss settings card on ESC key
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                if (settingsCard.isVisible()) {
                    settingsCard.hide();
                    actionButtons.setSettingsActive(false);
                }
            }
        });

        stage.setTitle("RUET Campus Map");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();

        // Initial theme application and center fit
        applyThemeState.run();
        Platform.runLater(centerAndFitMap);
    }

    public static void main(String[] args) {
        launch(args);
    }
}