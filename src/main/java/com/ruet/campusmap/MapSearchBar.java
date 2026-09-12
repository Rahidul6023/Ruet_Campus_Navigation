package com.ruet.campusmap;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

/**
 * Component responsible for creating and handling the Google Maps-style
 * floating search bar.
 *
 * It uses reactive property listeners (textProperty, selectedItemProperty, and setOnAction)
 * rather than any setOnClick listeners.
 */
public class MapSearchBar {

    private final VBox container;
    private final TextField searchField;
    private final ListView<String> suggestionListView;
    private final ObservableList<String> suggestions;

    // Sample coordinates for campus locations: [X, Y] center point on the SVG map
    private final Map<String, double[]> locationCoordinates = Map.of(
        "Central Library", new double[]{410.0, 515.0},
        "CSE Department", new double[]{650.0, 480.0},
        "Auditorium", new double[]{800.0, 600.0},
        "Admin Building", new double[]{500.0, 700.0},
        "Cafeteria", new double[]{900.0, 450.0},
        "Shahid Shahidul Islam Hall", new double[]{350.0, 300.0}
    );

    public MapSearchBar(Group mapGroup, StackPane rootPane) {
        // 1. Text Field setup (Google Maps look & feel)
        searchField = new TextField();
        searchField.setPromptText("Search RUET Campus (e.g. Library, CSE)...");
        searchField.setPrefWidth(320);
        searchField.setPrefHeight(44);
        searchField.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 8px; " +
            "-fx-padding: 0 14px; " +
            "-fx-font-size: 14px; " +
            "-fx-border-color: #dfe1e5; " +
            "-fx-border-radius: 8px;"
        );

        // 2. Dropdown List for search suggestions
        suggestions = FXCollections.observableArrayList();
        suggestionListView = new ListView<>(suggestions);
        suggestionListView.setPrefWidth(320);
        suggestionListView.setMaxHeight(160);
        suggestionListView.setVisible(false);
        suggestionListView.setManaged(false); // Does not take layout space when hidden
        suggestionListView.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 0 0 8px 8px; " +
            "-fx-border-color: #dfe1e5; " +
            "-fx-border-width: 0 1px 1px 1px;"
        );

        // 3. Container setup
        container = new VBox(searchField, suggestionListView);
        container.setMaxSize(320, VBox.USE_PREF_SIZE);
        container.setEffect(new DropShadow(12, Color.rgb(0, 0, 0, 0.20)));
        container.setPadding(new Insets(20, 0, 0, 20)); // Margin from top & left

        // Prevent dragging on the search bar from moving the map underneath
        container.setOnMousePressed(javafx.event.Event::consume);
        container.setOnMouseDragged(javafx.event.Event::consume);

        // Position floating at top-left
        StackPane.setAlignment(container, Pos.TOP_LEFT);

        // 4. Attach reactive listeners (NO setOnClick used)
        setupTextListener();
        setupSelectionListener(mapGroup, rootPane);
        setupEnterKeyListener(mapGroup, rootPane);
    }

    /**
     * Listens to text changes as the user types and updates suggestions list.
     */
    private void setupTextListener() {
        searchField.textProperty().addListener((observable, oldText, newText) -> {
            if (newText == null || newText.trim().isEmpty()) {
                hideDropdown();
            } else {
                String query = newText.trim().toLowerCase();
                List<String> matches = locationCoordinates.keySet().stream()
                    .filter(name -> name.toLowerCase().contains(query))
                    .toList();

                if (matches.isEmpty()) {
                    hideDropdown();
                } else {
                    suggestions.setAll(matches);
                    suggestionListView.setVisible(true);
                    suggestionListView.setManaged(true);
                }
            }
        });
    }

    /**
     * Listens to item selection changes in the dropdown (keyboard navigation or focus).
     */
    private void setupSelectionListener(Group mapGroup, StackPane rootPane) {
        suggestionListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selectedItem) -> {
            if (selectedItem != null) {
                searchField.setText(selectedItem);
                hideDropdown();
                focusOnLocation(selectedItem, mapGroup, rootPane);
            }
        });
    }

    /**
     * Triggered when the user presses Enter key in the search field.
     */
    private void setupEnterKeyListener(Group mapGroup, StackPane rootPane) {
        searchField.setOnAction(event -> {
            String query = searchField.getText();
            if (query != null && !query.trim().isEmpty()) {
                String trimmed = query.trim();
                // Find first match
                locationCoordinates.keySet().stream()
                    .filter(name -> name.equalsIgnoreCase(trimmed) || name.toLowerCase().contains(trimmed.toLowerCase()))
                    .findFirst()
                    .ifPresent(matched -> {
                        searchField.setText(matched);
                        hideDropdown();
                        focusOnLocation(matched, mapGroup, rootPane);
                    });
            }
        });
    }

    /**
     * Centers the map viewport smoothly on the selected location.
     */
    private void focusOnLocation(String locationName, Group mapGroup, StackPane rootPane) {
        double[] coords = locationCoordinates.get(locationName);
        if (coords == null) return;

        double targetX = coords[0];
        double targetY = coords[1];

        // Zoom in to 2.0x for focus
        mapGroup.setScaleX(2.0);
        mapGroup.setScaleY(2.0);

        // Center the selected point within current window bounds
        double viewWidth = rootPane.getWidth();
        double viewHeight = rootPane.getHeight();

        double newTranslateX = (viewWidth / 2.0) - (targetX * 2.0);
        double newTranslateY = (viewHeight / 2.0) - (targetY * 2.0);

        mapGroup.setTranslateX(newTranslateX);
        mapGroup.setTranslateY(newTranslateY);
    }

    private void hideDropdown() {
        suggestionListView.setVisible(false);
        suggestionListView.setManaged(false);
        suggestions.clear();
    }

    public VBox getContainer() {
        return container;
    }
}
