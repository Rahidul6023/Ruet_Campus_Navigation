package com.ruet.campusmap;

import com.ruet.campusmap.model.BuildingPolygon;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Component responsible for creating an authentic Google Maps-style
 * pill-rounded floating search bar with icons and dropdown suggestions.
 *
 * It uses reactive listeners (textProperty, selectedItemProperty, setOnAction)
 * without any setOnClick functions.
 */
public class MapSearchBar {

    private final VBox wrapper;
    private final HBox searchCard;
    private final TextField searchField;
    private final ListView<String> suggestionListView;
    private final ObservableList<String> suggestions;
    private final SVGPath searchIcon;
    private final SVGPath clearIcon;
    private final SVGPath directionsIcon;
    private final Separator separator;
    private boolean isDark = false;

    // Searchable coordinates for campus locations: [X, Y] center point on the SVG map
    private final Map<String, double[]> locationCoordinates = new HashMap<>(Map.of(
        "Central Library", new double[]{410.0, 515.0},
        "CSE Department", new double[]{650.0, 480.0},
        "Auditorium", new double[]{800.0, 600.0},
        "Admin Building", new double[]{500.0, 700.0},
        "Cafeteria", new double[]{900.0, 450.0},
        "Shahid Shahidul Islam Hall", new double[]{350.0, 300.0}
    ));

    public void registerBuildings(List<BuildingPolygon> buildings) {
        if (buildings == null) return;
        for (BuildingPolygon bp : buildings) {
            if (bp == null || bp.getName() == null || bp.getPoints() == null || bp.getPoints().isEmpty()) continue;
            double sumX = 0, sumY = 0;
            int count = 0;
            for (double[] pt : bp.getPoints()) {
                if (pt != null && pt.length >= 2) {
                    sumX += pt[0];
                    sumY += pt[1];
                    count++;
                }
            }
            if (count > 0) {
                locationCoordinates.putIfAbsent(bp.getName(), new double[]{sumX / count, sumY / count});
            }
        }
    }

    public MapSearchBar(Group mapGroup, StackPane rootPane) {
        // --- 1. Left Magnifying Glass Icon (Google Maps Style) ---
        searchIcon = new SVGPath();
        searchIcon.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        searchIcon.setFill(Color.web("#5f6368"));
        searchIcon.setScaleX(0.9);
        searchIcon.setScaleY(0.9);

        // --- 2. Clean Borderless Input Field ---
        searchField = new TextField();
        searchField.setPromptText("Search RUET Campus");
        searchField.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-border-color: transparent; " +
            "-fx-font-size: 15px; " +
            "-fx-font-family: 'Segoe UI', Roboto, sans-serif; " +
            "-fx-text-fill: #202124; " +
            "-fx-prompt-text-fill: #70757a; " +
            "-fx-padding: 0 8px;"
        );
        HBox.setHgrow(searchField, Priority.ALWAYS);

        // --- 3. Clear (X) Icon (Appears dynamically when typing) ---
        clearIcon = new SVGPath();
        clearIcon.setContent("M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z");
        clearIcon.setFill(Color.web("#70757a"));
        clearIcon.setVisible(false);
        clearIcon.setManaged(false);

        // Clear search text on mouse press (no setOnClick used)
        clearIcon.setOnMousePressed(e -> {
            searchField.clear();
            searchField.requestFocus();
        });

        // Vertical divider between text and directions icon
        separator = new Separator();
        separator.setOrientation(javafx.geometry.Orientation.VERTICAL);
        separator.setPrefHeight(24);
        separator.setStyle("-fx-opacity: 0.4;");

        // --- 4. Right Google Maps Directions Icon (Blue diamond) ---
        directionsIcon = new SVGPath();
        directionsIcon.setContent("M21.71 11.29l-9-9a.996.996 0 0 0-1.41 0l-9 9a.996.996 0 0 0 0 1.41l9 9c.39.39 1.02.39 1.41 0l9-9a.996.996 0 0 0 0-1.41zM14 14.5V12h-4v3H8v-4c0-.55.45-1 1-1h5V7.5l3.5 3.5-3.5 3.5z");
        directionsIcon.setFill(Color.web("#1a73e8")); // Google Maps Blue
        directionsIcon.setScaleX(1.1);
        directionsIcon.setScaleY(1.1);

        // --- 5. The Rounded Pill Search Bar Card ---
        searchCard = new HBox(10, searchIcon, searchField, clearIcon, separator, directionsIcon);
        searchCard.setAlignment(Pos.CENTER_LEFT);
        searchCard.setPrefWidth(380);
        searchCard.setPrefHeight(48);
        searchCard.setPadding(new Insets(0, 16, 0, 16));

        // --- 6. Dropdown Suggestions List ---
        suggestions = FXCollections.observableArrayList();
        suggestionListView = new ListView<>(suggestions);
        suggestionListView.setPrefWidth(380);
        suggestionListView.setMaxHeight(180);
        suggestionListView.setVisible(false);
        suggestionListView.setManaged(false);

        // --- 7. Main Floating Wrapper Container ---
        wrapper = new VBox(searchCard, suggestionListView);
        wrapper.setMaxSize(420, VBox.USE_PREF_SIZE);
        wrapper.setEffect(new DropShadow(15, 0, 4, Color.rgb(60, 64, 67, 0.28)));
        wrapper.setPadding(new Insets(18, 0, 0, 0)); // Top offset

        // Prevent dragging the search bar from moving the campus map underneath
        wrapper.setOnMousePressed(javafx.event.Event::consume);
        wrapper.setOnMouseDragged(javafx.event.Event::consume);

        // Align floating at top-center
        StackPane.setAlignment(wrapper, Pos.TOP_CENTER);

        applyTheme(false);

        // --- 8. Reactive Event Listeners (Zero setOnClick) ---
        setupTextListener();
        setupSelectionListener(mapGroup, rootPane);
        setupEnterKeyListener(mapGroup, rootPane);
    }

    public void applyTheme(boolean isDark) {
        this.isDark = isDark;
        if (isDark) {
            searchField.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-border-color: transparent; " +
                "-fx-font-size: 15px; " +
                "-fx-font-family: 'Segoe UI', Roboto, sans-serif; " +
                "-fx-text-fill: #e8eaed; " +
                "-fx-prompt-text-fill: #9aa0a6; " +
                "-fx-padding: 0 8px;"
            );
            searchIcon.setFill(Color.web("#9aa0a6"));
            clearIcon.setFill(Color.web("#9aa0a6"));
            suggestionListView.setStyle(
                "-fx-background-color: #202124; " +
                "-fx-background-radius: 0 0 16px 16px; " +
                "-fx-border-color: #3c4043; " +
                "-fx-border-width: 0 1px 1px 1px; " +
                "-fx-border-radius: 0 0 16px 16px; " +
                "-fx-font-size: 14px; " +
                "-fx-font-family: 'Segoe UI', Roboto, sans-serif;"
            );
        } else {
            searchField.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-border-color: transparent; " +
                "-fx-font-size: 15px; " +
                "-fx-font-family: 'Segoe UI', Roboto, sans-serif; " +
                "-fx-text-fill: #202124; " +
                "-fx-prompt-text-fill: #70757a; " +
                "-fx-padding: 0 8px;"
            );
            searchIcon.setFill(Color.web("#5f6368"));
            clearIcon.setFill(Color.web("#70757a"));
            suggestionListView.setStyle(
                "-fx-background-color: #ffffff; " +
                "-fx-background-radius: 0 0 16px 16px; " +
                "-fx-border-color: #dadce0; " +
                "-fx-border-width: 0 1px 1px 1px; " +
                "-fx-border-radius: 0 0 16px 16px; " +
                "-fx-font-size: 14px; " +
                "-fx-font-family: 'Segoe UI', Roboto, sans-serif;"
            );
        }

        if (suggestionListView.isVisible()) {
            showDropdown();
        } else {
            hideDropdown();
        }
    }

    /**
     * Listens to text changes dynamically (Reactive auto-suggest & clear button toggle).
     */
    private void setupTextListener() {
        searchField.textProperty().addListener((observable, oldText, newText) -> {
            boolean hasText = (newText != null && !newText.trim().isEmpty());
            clearIcon.setVisible(hasText);
            clearIcon.setManaged(hasText);

            if (!hasText) {
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
                    showDropdown();
                }
            }
        });
    }

    /**
     * Listens to item selection in the suggestions dropdown (Reactive selection).
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
     * Listens to keyboard Enter in the search bar.
     */
    private void setupEnterKeyListener(Group mapGroup, StackPane rootPane) {
        searchField.setOnAction(event -> {
            String query = searchField.getText();
            if (query != null && !query.trim().isEmpty()) {
                String trimmed = query.trim();
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
     * Centers map view on selected location and applies smooth camera fly-to transition with 2.0x zoom.
     */
    private void focusOnLocation(String locationName, Group mapGroup, StackPane rootPane) {
        double[] coords = locationCoordinates.get(locationName);
        if (coords == null) return;

        double targetX = coords[0];
        double targetY = coords[1];
        double targetScale = 2.0;

        double viewWidth = rootPane.getWidth();
        double viewHeight = rootPane.getHeight();

        double targetTx = (500.0 - targetX) * targetScale;
        double targetTy = (500.0 - targetY) * targetScale;

        double scaledWidth = 1000.0 * targetScale;
        double scaledHeight = 1000.0 * targetScale;
        double halfExcessX = Math.max(0.0, (scaledWidth - viewWidth) / 2.0);
        double halfExcessY = Math.max(0.0, (scaledHeight - viewHeight) / 2.0);
        double margin = 200.0;
        targetTx = Math.max(-halfExcessX - margin, Math.min(halfExcessX + margin, targetTx));
        targetTy = Math.max(-halfExcessY - margin, Math.min(halfExcessY + margin, targetTy));

        Timeline flyToAnim = new Timeline(
            new KeyFrame(Duration.millis(450),
                new KeyValue(mapGroup.scaleXProperty(), targetScale, Interpolator.EASE_BOTH),
                new KeyValue(mapGroup.scaleYProperty(), targetScale, Interpolator.EASE_BOTH),
                new KeyValue(mapGroup.translateXProperty(), targetTx, Interpolator.EASE_BOTH),
                new KeyValue(mapGroup.translateYProperty(), targetTy, Interpolator.EASE_BOTH)
            )
        );
        flyToAnim.play();
    }

    private void showDropdown() {
        if (isDark) {
            searchCard.setStyle(
                "-fx-background-color: #202124; " +
                "-fx-background-radius: 24px 24px 0 0; " +
                "-fx-border-radius: 24px 24px 0 0; " +
                "-fx-border-color: #3c4043; " +
                "-fx-border-width: 1px 1px 0 1px;"
            );
        } else {
            searchCard.setStyle(
                "-fx-background-color: #ffffff; " +
                "-fx-background-radius: 24px 24px 0 0; " +
                "-fx-border-radius: 24px 24px 0 0; " +
                "-fx-border-color: #dadce0; " +
                "-fx-border-width: 1px 1px 0 1px;"
            );
        }
        suggestionListView.setVisible(true);
        suggestionListView.setManaged(true);
    }

    private void hideDropdown() {
        if (isDark) {
            searchCard.setStyle(
                "-fx-background-color: #202124; " +
                "-fx-background-radius: 24px; " +
                "-fx-border-radius: 24px; " +
                "-fx-border-color: #3c4043; " +
                "-fx-border-width: 1px;"
            );
        } else {
            searchCard.setStyle(
                "-fx-background-color: #ffffff; " +
                "-fx-background-radius: 24px; " +
                "-fx-border-radius: 24px; " +
                "-fx-border-color: #dadce0; " +
                "-fx-border-width: 1px;"
            );
        }
        suggestionListView.setVisible(false);
        suggestionListView.setManaged(false);
        suggestions.clear();
    }

    public VBox getContainer() {
        return wrapper;
    }
}
