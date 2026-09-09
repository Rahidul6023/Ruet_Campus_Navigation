package com.ruet.nav;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/ruet/nav/main-view.fxml"));
        Scene scene = new Scene(loader.load(), 1100, 750);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/com/ruet/nav/style.css")).toExternalForm());

        stage.setTitle("RUET Navigation System");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
