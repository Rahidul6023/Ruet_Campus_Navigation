package com.ruet.campusmap.editor;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MapEditorApp extends Application{
    @Override 
    public void start(Stage stage){

        // Root Container
        StackPane root = new StackPane();

        Scene scene = new Scene(root,1200,800);
        stage.setTitle("RUET Digital Campus- Editor Mode");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args){
        launch(args);
    }
}
