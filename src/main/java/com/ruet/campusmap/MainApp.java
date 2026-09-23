package com.ruet.campusmap;

import com.ruet.campusmap.editor.AdminLoginDialog;
import com.ruet.campusmap.editor.MapEditorManager;
import com.ruet.campusmap.model.AppSettings;
import com.ruet.campusmap.model.BuildingPolygon;
import com.ruet.campusmap.service.PolygonDataLoader;
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
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
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
        String svgUrl = getClass().getResource("/maps/in.svg").toExternalForm();

        double mapWidth = 2000;
        double mapHeight = 1300;

        // Settings Model
        AppSettings settings = new AppSettings();

        // SVG Map Integration
        WebView campusView = new WebView();
        campusView.getEngine().load(svgUrl);
        campusView.getEngine().setUserStyleSheetLocation(
            "data:text/css;charset=utf-8," + 
            URLEncoder.encode("::-webkit-scrollbar { display: none !important; width: 0 !important; height: 0 !important; }", StandardCharsets.UTF_8)
        );
        campusView.getChildrenUnmodifiable().addListener((javafx.collections.ListChangeListener<javafx.scene.Node>) change -> {
            for (javafx.scene.Node node : campusView.lookupAll(".scroll-bar")) {
                node.setVisible(false);
                node.setManaged(false);
            }
        });

        // Map Size Defining
        campusView.setPrefSize(mapWidth, mapHeight);
        campusView.setMinSize(mapWidth, mapHeight);
        campusView.setMaxSize(mapWidth, mapHeight);
        campusView.setMouseTransparent(true);

        // Creating Polygon layer
        Pane polygonLayer = new Pane();
        polygonLayer.setPrefSize(mapWidth, mapHeight);

        // Modern floating building info card (Bottom-Left)
        BuildingInfoCard buildingInfoCard = new BuildingInfoCard();

        // Interactive Building Labels Layer
        BuildingLabelsLayer buildingLabelsLayer = new BuildingLabelsLayer(settings, buildingName -> {
            buildingInfoCard.showPoi(buildingName, "Campus Landmark", "RUET Campus");
        });

        // Known landmark coordinates on the SVG map canvas
        Map<String, double[]> knownCoordinates = Map.of(
            "Central Library", new double[]{410.0, 515.0},
            "CSE Department", new double[]{650.0, 480.0},
            "Auditorium", new double[]{800.0, 600.0},
            "Admin Building", new double[]{500.0, 700.0},
            "Cafeteria", new double[]{900.0, 450.0},
            "Shahid Shahidul Islam Hall", new double[]{350.0, 300.0}
        );
        buildingLabelsLayer.initKnownLandmarks(knownCoordinates);

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
        });

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

            root.setStyle(isDark ? "-fx-background-color: #121214;" : "-fx-background-color: #f1f3f4;");

            try {
                if (isDark) {
                    campusView.getEngine().executeScript(
                        "document.documentElement.style.filter = 'invert(0.92) hue-rotate(180deg) brightness(0.9) contrast(1.15) saturate(1.2)';" +
                        "document.body.style.backgroundColor = '#18191c';"
                    );
                } else {
                    campusView.getEngine().executeScript(
                        "document.documentElement.style.filter = 'none';" +
                        "document.body.style.backgroundColor = '#ffffff';"
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
        };

        // Wire settings model changes
        settings.addListener(applyThemeState);

        // Run theme apply when SVG map finishes loading
        campusView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                campusView.getEngine().executeScript("document.body.style.overflow='hidden'");
                applyThemeState.run();
            }
        });

        // Map Panning and dismiss settings card on click-outside
        root.setOnMousePressed(event -> {
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
                double newX = mapGroup.getTranslateX() + deltaX;
                double newY = mapGroup.getTranslateY() + deltaY;
                double viewWidth = root.getWidth();
                double viewHeight = root.getHeight();
                double currentScale = mapGroup.getScaleX();
                double extraMargin = 500 * currentScale;
                double minX = -(mapWidth * currentScale - viewWidth) - extraMargin;
                double maxX = extraMargin;
                double minY = -(mapHeight * currentScale - viewHeight) - extraMargin;
                double maxY = extraMargin;

                mapGroup.setTranslateX(Math.max(minX, Math.min(maxX, newX)));
                mapGroup.setTranslateY(Math.max(minY, Math.min(maxY, newY)));
            }

            lastMouseX = event.getSceneX();
            lastMouseY = event.getSceneY();
        });

        // Smooth cursor-centered focal zooming
        root.setOnScroll(event -> {
            double zoomFactor = (event.getDeltaY() > 0) ? 1.12 : 0.88;
            double currentScale = mapGroup.getScaleX();
            double newScale = currentScale * zoomFactor;

            if (newScale >= 0.35 && newScale <= 5.0) {
                Point2D mouseScene = new Point2D(event.getSceneX(), event.getSceneY());
                Point2D mouseLocal = mapGroup.sceneToLocal(mouseScene);

                mapGroup.setScaleX(newScale);
                mapGroup.setScaleY(newScale);

                Point2D afterScene = mapGroup.localToScene(mouseLocal);
                mapGroup.setTranslateX(mapGroup.getTranslateX() - (afterScene.getX() - mouseScene.getX()));
                mapGroup.setTranslateY(mapGroup.getTranslateY() - (afterScene.getY() - mouseScene.getY()));
            }

            event.consume();
        });

        // Double-click to smoothly zoom in towards cursor position
        root.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY && !editorManager.isDrawMode()) {
                double currentScale = mapGroup.getScaleX();
                double targetScale = Math.min(5.0, currentScale * 1.5);
                if (targetScale != currentScale) {
                    Point2D mouseScene = new Point2D(event.getSceneX(), event.getSceneY());
                    Point2D mouseLocal = mapGroup.sceneToLocal(mouseScene);

                    mapGroup.setScaleX(targetScale);
                    mapGroup.setScaleY(targetScale);
                    Point2D afterScene = mapGroup.localToScene(mouseLocal);
                    double targetTx = mapGroup.getTranslateX() - (afterScene.getX() - mouseScene.getX());
                    double targetTy = mapGroup.getTranslateY() - (afterScene.getY() - mouseScene.getY());

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

        // Initial theme application
        applyThemeState.run();
    }

    public static void main(String[] args) {
        launch(args);
    }
}