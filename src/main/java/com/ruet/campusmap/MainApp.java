package com.ruet.campusmap;



import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.web.WebView;

public class MainApp extends Application {

    private double lastMouseX;
    private double lastMouseY;

    @Override
    public void start(Stage stage) {
        String svgUrl = getClass().getResource("/maps/in.svg").toExternalForm();
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

        campusView.setOnMousePressed(event ->{
            lastMouseX  = event.getScreenX();
            lastMouseY = event.getScreenY();
        });
        campusView.setOnMouseDragged(event -> {
            double deltaX = event.getScreenX()-lastMouseX;
            double deltaY = event.getScreenY()-lastMouseY;
            campusView.getEngine().executeScript(
                String.format(java.util.Locale.US,"window.scrollBy(%f, %f);",-deltaX,-deltaY)
            );
            lastMouseX = event.getScreenX();
            lastMouseY = event.getScreenY();
        });
        Label label = new Label("RUET Campus Map");
        StackPane root = new StackPane(campusView,label);
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