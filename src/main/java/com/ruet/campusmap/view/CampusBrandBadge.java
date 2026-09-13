package com.ruet.campusmap.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;

/**
 * Clean floating RUET logo badge on the top-left corner of the window.
 * Displays only the official ruet-seeklogo.svg cleanly styled without any extra text.
 */
public class CampusBrandBadge {

    private final StackPane container;

    public CampusBrandBadge() {
        container = new StackPane();
        container.setPrefSize(52, 52);
        container.setMinSize(52, 52);
        container.setMaxSize(52, 52);
        container.setAlignment(Pos.CENTER);
        container.setStyle(
            "-fx-background-color: #ffffff; " +
            "-fx-background-radius: 50%; " +
            "-fx-border-radius: 50%; " +
            "-fx-border-color: #dadce0; " +
            "-fx-border-width: 1px;"
        );
        container.setEffect(new DropShadow(10, 0, 3, Color.rgb(60, 64, 67, 0.25)));

        // Prevent dragging on the logo from panning the map underneath
        container.setOnMousePressed(javafx.event.Event::consume);
        container.setOnMouseDragged(javafx.event.Event::consume);

        // Load ruet-seeklogo.svg in an embedded WebView
        WebView logoView = new WebView();
        logoView.setPrefSize(40, 40);
        logoView.setMinSize(40, 40);
        logoView.setMaxSize(40, 40);
        logoView.setMouseTransparent(true);

        String logoUrl = getClass().getResource("/maps/ruet-seeklogo.svg").toExternalForm();
        logoView.getEngine().load(logoUrl);

        // Scale and fit the SVG perfectly inside the circular container
        logoView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                logoView.getEngine().executeScript(
                    "document.body.style.margin = '0';" +
                    "document.body.style.padding = '0';" +
                    "document.body.style.overflow = 'hidden';" +
                    "document.body.style.background = 'transparent';" +
                    "document.body.style.display = 'flex';" +
                    "document.body.style.alignItems = 'center';" +
                    "document.body.style.justifyContent = 'center';" +
                    "var svg = document.querySelector('svg');" +
                    "if (svg) {" +
                    "  svg.style.width = '100%';" +
                    "  svg.style.height = '100%';" +
                    "  svg.style.display = 'block';" +
                    "}"
                );
            }
        });

        container.getChildren().add(logoView);

        // Position floating top-left
        StackPane.setAlignment(container, Pos.TOP_LEFT);
        StackPane.setMargin(container, new Insets(18, 0, 0, 20));
    }

    public StackPane getContainer() {
        return container;
    }
}
