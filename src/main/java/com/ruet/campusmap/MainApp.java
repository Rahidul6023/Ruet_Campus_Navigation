package com.ruet.campusmap;



import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.scene.web.WebView;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;


public class MainApp extends Application {

    private double lastMouseX;
    private double lastMouseY;

    @Override
    public void start(Stage stage) {
        String svgUrl = getClass().getResource("/maps/in.svg").toExternalForm();

        double mapWidth = 2000;
        double mapHeight = 1300;

        // SVG Map Integration
        WebView campusView = new WebView();
        campusView.getEngine().load(svgUrl);
        campusView.getEngine().getLoadWorker().stateProperty().addListener(((obs,oldState,newState)->{
            if(newState==javafx.concurrent.Worker.State.SUCCEEDED){
                campusView.getEngine().executeScript("document.body.style.overflow='hidden'");
            }
        }));
        campusView.getEngine().setUserStyleSheetLocation(
            "data:text/css;charset=utf-8," + 
            java.net.URLEncoder.encode("::-webkit-scrollbar { display: none !important; width: 0 !important; height: 0 !important; }", java.nio.charset.StandardCharsets.UTF_8)
        );
        campusView.getChildrenUnmodifiable().addListener((javafx.collections.ListChangeListener<javafx.scene.Node>) change -> {
            for (javafx.scene.Node node : campusView.lookupAll(".scroll-bar")) {
                node.setVisible(false);
                node.setManaged(false);
            }
        });

        // Map Size Defining
        campusView.setPrefSize(mapWidth,mapHeight);
        campusView.setMinSize(mapWidth,mapHeight);
        campusView.setMaxSize(mapWidth,mapHeight);
        campusView.setMouseTransparent(true);

        // Creating Polygon layer
        Pane polygonLayer = new Pane();
        polygonLayer.setPrefSize(mapWidth, mapHeight);

        // Test Polygon
        Polygon testPoly = new Polygon(
            320.0, 450.0,
            500.0, 450.0,
            500.0, 580.0,
            320.0, 580.0
        );
        testPoly.setFill(Color.rgb(0,180,255, 0.45));
        testPoly.setStroke(Color.rgb(0, 180, 255));
        testPoly.setStrokeWidth(2.5);
        polygonLayer.getChildren().add(testPoly);

        Group mapGroup = new Group(campusView,polygonLayer);

        Label label = new Label("RUET Campus Map");
        StackPane root = new StackPane(mapGroup, label);

        // Google Maps-style Search Bar (Modular & decoupled)
        MapSearchBar searchBar = new MapSearchBar(mapGroup, root);
        
        // Floating Settings & Admin Buttons (Top-Right)
        MapActionButtons actionButtons = new MapActionButtons();

        // Floating Zoom In/Out & Location Controls (Bottom-Center)
        MapBottomControls bottomControls = new MapBottomControls(mapGroup);

        root.getChildren().addAll(searchBar.getContainer(), actionButtons.getContainer(), bottomControls.getContainer());





        // Map Panning
        root.setOnMousePressed(event -> {
            lastMouseX = event.getSceneX();
            lastMouseY = event.getSceneY();
        });

        root.setOnMouseDragged(event -> {
            double deltaX = event.getSceneX() - lastMouseX;
            double deltaY = event.getSceneY() - lastMouseY;
        double newX = mapGroup.getTranslateX() + deltaX;
        double newY = mapGroup.getTranslateY() + deltaY;
        double viewWidth = root.getWidth();
        double viewHeight = root.getHeight();
        double minX = -(mapWidth-viewWidth) -400;
        double maxX = 400;
        double minY = -(mapHeight - viewHeight) -400;
        double maxY = 400;

        mapGroup.setTranslateX(Math.max(minX,Math.min(maxX,newX)));
        mapGroup.setTranslateY(Math.max(minY,Math.min(maxY,newY)));

        lastMouseX = event.getSceneX();
        lastMouseY = event.getSceneY();
        });

        // Map Zooming
        root.setOnScroll(event -> {
            double zoomFactor = (event.getDeltaY()>0)? 1.1:0.9;
            double currentScale = mapGroup.getScaleX();
            double newScale = currentScale*zoomFactor;

            if(newScale >= 0.5 && newScale <= 4.0) {
                mapGroup.setScaleX(newScale);
                mapGroup.setScaleY(newScale);
            }

            event.consume();
        });

        Scene scene = new Scene(root, 1200, 800);

        stage.setTitle("RUET Campus Map");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args){
        launch(args);
    }
}