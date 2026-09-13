package com.ruet.campusmap.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;

/**
 * Modern floating RUET Campus Brand Badge placed on the top-left corner of the window.
 * Displays the official RUET logo SVG alongside title typography.
 */
public class CampusBrandBadge {

    private final HBox container;

    public CampusBrandBadge() {
        container = new HBox(12);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(8, 16, 8, 12));
        container.setStyle(
            "-fx-background-color: #ffffff; " +
            "-fx-background-radius: 28px; " +
            "-fx-border-radius: 28px; " +
            "-fx-border-color: #dadce0; " +
            "-fx-border-width: 1px;"
        );
        container.setEffect(new DropShadow(12, 0, 3, Color.rgb(60, 64, 67, 0.22)));
        container.setMaxSize(HBox.USE_PREF_SIZE, HBox.USE_PREF_SIZE);

        // Prevent dragging on badge from panning map underneath
        container.setOnMousePressed(javafx.event.Event::consume);
        container.setOnMouseDragged(javafx.event.Event::consume);

        // 1. Logo container loading the ruet-seeklogo.svg
        WebView logoView = new WebView();
        logoView.setPrefSize(42, 42);
        logoView.setMinSize(42, 42);
        logoView.setMaxSize(42, 42);
        logoView.setMouseTransparent(true);

        String logoUrl = getClass().getResource("/maps/ruet-seeklogo.svg").toExternalForm();
        logoView.getEngine().load(logoUrl);

        // Hide scrollbars & fit SVG inside 42x42
        logoView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                logoView.getEngine().executeScript(
                    "document.body.style.margin = '0';" +
                    "document.body.style.overflow = 'hidden';" +
                    "document.body.style.display = 'flex';" +
                    "document.body.style.alignItems = 'center';" +
                    "document.body.style.justifyContent = 'center';" +
                    "var svg = document.querySelector('svg');" +
                    "if (svg) { svg.style.width = '100% '; svg.style.height = '100% '; svg.style.objectFit = 'contain'; }"
                );
            }
        });

        // 2. RUET Titles
        VBox textContainer = new VBox(2);
        textContainer.setAlignment(Pos.CENTER_LEFT);

        Label mainTitle = new Label("RUET");
        mainTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a73e8;");

        Label subTitle = new Label("Digital Campus");
        subTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #5f6368;");

        textContainer.getChildren().addAll(mainTitle, subTitle);

        container.getChildren().addAll(logoView, textContainer);

        // Align floating top-left
        StackPane.setAlignment(container, Pos.TOP_LEFT);
        StackPane.setMargin(container, new Insets(18, 0, 0, 20));
    }

    public HBox getContainer() {
        return container;
    }
}
