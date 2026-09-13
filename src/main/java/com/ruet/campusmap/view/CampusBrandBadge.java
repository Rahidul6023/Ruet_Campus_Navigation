package com.ruet.campusmap.view;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;

/**
 * Clean floating RUET logo on the top-left corner of the window.
 * Displays only the standalone official ruet-seeklogo.svg cleanly rendered
 * with transparent background and no surrounding circle or borders.
 */
public class CampusBrandBadge {

    private final StackPane container;

    public CampusBrandBadge() {
        // Natural aspect ratio for 1647x2000 logo is ~0.8235 (e.g., 48x58)
        double width = 48;
        double height = 58;

        container = new StackPane();
        container.setPrefSize(width, height);
        container.setMinSize(width, height);
        container.setMaxSize(width, height);
        container.setAlignment(Pos.CENTER);
        container.setStyle("-fx-background-color: transparent;");

        // Prevent dragging on the logo from panning the map underneath
        container.setOnMousePressed(javafx.event.Event::consume);
        container.setOnMouseDragged(javafx.event.Event::consume);

        // Load ruet-seeklogo.svg in an embedded WebView with 100% transparent background
        WebView logoView = new WebView();
        logoView.setPrefSize(width, height);
        logoView.setMinSize(width, height);
        logoView.setMaxSize(width, height);
        logoView.setMouseTransparent(true);
        logoView.setPageFill(Color.TRANSPARENT);
        logoView.setStyle("-fx-background-color: transparent;");

        // Subtle drop shadow behind the logo icon itself to elevate it above the map
        logoView.setEffect(new DropShadow(6, 0, 2, Color.rgb(0, 0, 0, 0.30)));

        // Completely suppress any JavaFX scrollbar nodes in the WebView hierarchy
        logoView.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> hideScrollBars(logoView));

        // Inject CSS stylesheet into WebEngine to disable scrollbars and ensure full transparency
        String css = "* { margin: 0; padding: 0; box-sizing: border-box; } " +
                     "html, body { margin: 0 !important; padding: 0 !important; width: 100% !important; height: 100% !important; overflow: hidden !important; background: transparent !important; display: flex !important; align-items: center !important; justify-content: center !important; } " +
                     "::-webkit-scrollbar { display: none !important; width: 0 !important; height: 0 !important; } " +
                     "svg { width: 100% !important; height: 100% !important; max-width: 100% !important; max-height: 100% !important; display: block !important; margin: auto !important; }";
        try {
            logoView.getEngine().setUserStyleSheetLocation(
                "data:text/css;charset=utf-8," + URLEncoder.encode(css, StandardCharsets.UTF_8)
            );
        } catch (Exception ignored) {}

        // Load SVG content directly as HTML5 with responsive 100% scaling
        String svgContent = loadSvgResource();
        if (svgContent != null) {
            int svgStart = svgContent.indexOf("<svg");
            if (svgStart != -1) {
                svgContent = svgContent.substring(svgStart);
            }
            // Replace fixed width/height attributes so SVG scales responsively via its viewBox="0 0 1647 2000"
            String responsiveSvg = svgContent.replaceFirst("width=\"[^\"]*\"", "width=\"100%\"")
                                             .replaceFirst("height=\"[^\"]*\"", "height=\"100%\"");
            String html = "<!DOCTYPE html><html><head><style>" + css + "</style></head><body>" + responsiveSvg + "</body></html>";
            logoView.getEngine().loadContent(html, "text/html");
        } else {
            var url = getClass().getResource("/maps/ruet-seeklogo.svg");
            if (url != null) {
                logoView.getEngine().load(url.toExternalForm());
            }
        }

        logoView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                hideScrollBars(logoView);
            }
        });

        container.getChildren().add(logoView);

        // Position floating top-left
        StackPane.setAlignment(container, Pos.TOP_LEFT);
        StackPane.setMargin(container, new Insets(14, 0, 0, 20));
    }

    private void hideScrollBars(WebView webView) {
        for (Node node : webView.lookupAll(".scroll-bar")) {
            node.setVisible(false);
            node.setManaged(false);
            node.setOpacity(0);
        }
    }

    private String loadSvgResource() {
        try (InputStream is = getClass().getResourceAsStream("/maps/ruet-seeklogo.svg")) {
            if (is != null) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public StackPane getContainer() {
        return container;
    }
}
