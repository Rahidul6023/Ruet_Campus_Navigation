package com.ruet.campusmap.view;

import com.ruet.campusmap.model.AppSettings;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.List;

/**
 * Layer that renders Points of Interest (POI) markers on the map canvas.
 * Handles quick toggles for:
 * - Food (Cafeterias & Canteens)
 * - Printers (Photocopy & Printing)
 * - Restrooms (Public Facilities)
 * - Parking (Vehicle & Bicycle Stands)
 * - Accessibility (Ramps & Level entrances when Avoid Stairs is ON)
 * - Shuttle (Campus shuttle stops when Shuttle mode is selected)
 */
public class MapPoiLayer {

    public interface OnPoiClickListener {
        void onPoiClick(String name, String category, String description);
    }

    public enum PoiCategory {
        FOOD,
        PRINTER,
        RESTROOM,
        PARKING,
        ACCESSIBILITY,
        SHUTTLE
    }

    private static class PoiItem {
        final String name;
        final PoiCategory category;
        final String description;
        final double x;
        final double y;
        final StackPane markerNode;

        PoiItem(String name, PoiCategory category, String description, double x, double y, StackPane markerNode) {
            this.name = name;
            this.category = category;
            this.description = description;
            this.x = x;
            this.y = y;
            this.markerNode = markerNode;
        }
    }

    private final Pane container;
    private final List<PoiItem> poiItems = new ArrayList<>();
    private final AppSettings settings;
    private final OnPoiClickListener clickListener;

    public MapPoiLayer(AppSettings settings, OnPoiClickListener clickListener) {
        this.settings = settings;
        this.clickListener = clickListener;
        this.container = new Pane();
        this.container.setPickOnBounds(false);
        initDefaultPois();
        refresh();
    }

    private void initDefaultPois() {
        // --- 1. Food POIs (Warm Orange / Amber) ---
        addPoi("Central Cafeteria", PoiCategory.FOOD, "Open 8:00 AM - 9:00 PM • Breakfast, Lunch & Meals", 900.0, 450.0, "🍽️", "#ea8600");
        addPoi("Shahidul Islam Hall Canteen", PoiCategory.FOOD, "Open 7:30 AM - 10:30 PM • Dining Hall", 330.0, 280.0, "🍽️", "#ea8600");
        addPoi("Campus Tea & Snacks Tong", PoiCategory.FOOD, "Student hangout, hot tea, bakery snacks", 680.0, 520.0, "☕", "#ea8600");
        addPoi("West Gate Food Court", PoiCategory.FOOD, "Fast food, juices, light meals", 250.0, 600.0, "🍽️", "#ea8600");

        // --- 2. Printers & Stationery POIs (Blue) ---
        addPoi("Central Library Print & Copy", PoiCategory.PRINTER, "High-speed document printing, spiral binding", 430.0, 500.0, "🖨️", "#1a73e8");
        addPoi("CSE Dept Document Corner", PoiCategory.PRINTER, "Lab manuals, photocopies, project prints", 670.0, 460.0, "🖨️", "#1a73e8");
        addPoi("Campus Market Stationery", PoiCategory.PRINTER, "Thesis printing, supplies, stationery", 520.0, 750.0, "🖨️", "#1a73e8");

        // --- 3. Restrooms POIs (Teal) ---
        addPoi("Central Library Restroom", PoiCategory.RESTROOM, "Ground floor, accessible stalls available", 395.0, 525.0, "🚻", "#00897b");
        addPoi("CSE Building Restroom (1st Fl)", PoiCategory.RESTROOM, "East wing washrooms", 630.0, 490.0, "🚻", "#00897b");
        addPoi("Auditorium Restroom Wing", PoiCategory.RESTROOM, "Public event facilities", 820.0, 580.0, "🚻", "#00897b");
        addPoi("Admin Building Public Washroom", PoiCategory.RESTROOM, "Visitor washrooms near lobby", 485.0, 715.0, "🚻", "#00897b");

        // --- 4. Parking POIs (Green) ---
        addPoi("Main Gate Vehicle Parking", PoiCategory.PARKING, "Cars, motorcycles & visitors", 180.0, 550.0, "🅿️", "#1e8e3e");
        addPoi("Admin Building Reserved Parking", PoiCategory.PARKING, "Faculty & administrative parking", 480.0, 670.0, "🅿️", "#1e8e3e");
        addPoi("Auditorium Parking Area", PoiCategory.PARKING, "Open parking ground", 830.0, 640.0, "🅿️", "#1e8e3e");
        addPoi("Department Cycle Stand", PoiCategory.PARKING, "Dedicated bicycle lock stands", 620.0, 440.0, "🚲", "#1e8e3e");

        // --- 5. Accessibility Ramps & Entrances (Wheelchair Accessible Mode) ---
        addPoi("Library Wheelchair Ramp", PoiCategory.ACCESSIBILITY, "Gentle slope ramp to Main Library Lobby", 405.0, 530.0, "♿", "#188038");
        addPoi("CSE Building Level Entry", PoiCategory.ACCESSIBILITY, "Zero-step accessible entrance with automatic doors", 640.0, 495.0, "♿", "#188038");
        addPoi("Admin Building West Ramp", PoiCategory.ACCESSIBILITY, "Ramp entrance connecting to elevator bank", 495.0, 685.0, "♿", "#188038");
        addPoi("Auditorium Ramp Entrance", PoiCategory.ACCESSIBILITY, "Barrier-free access to hall seating", 790.0, 610.0, "♿", "#188038");

        // --- 6. Campus Shuttle Stops ---
        addPoi("Main Gate Shuttle Stop", PoiCategory.SHUTTLE, "Campus Circular Shuttle route - Gate 1", 195.0, 520.0, "🚐", "#7b1fa2");
        addPoi("Academic Quad Shuttle Point", PoiCategory.SHUTTLE, "Connecting Library & Engineering Departments", 510.0, 510.0, "🚐", "#7b1fa2");
        addPoi("Auditorium Shuttle Terminal", PoiCategory.SHUTTLE, "Shuttle stop serving Halls & Sports Complex", 840.0, 615.0, "🚐", "#7b1fa2");
    }

    private void addPoi(String name, PoiCategory category, String description, double x, double y, String symbol, String hexColor) {
        Circle bgCircle = new Circle(14);
        bgCircle.setFill(Color.web(hexColor));
        bgCircle.setStroke(Color.WHITE);
        bgCircle.setStrokeWidth(2.0);

        Label iconLabel = new Label(symbol);
        iconLabel.setStyle("-fx-font-size: 12px;");

        StackPane marker = new StackPane(bgCircle, iconLabel);
        marker.setAlignment(Pos.CENTER);
        marker.setPrefSize(28, 28);
        marker.setMaxSize(28, 28);
        marker.setLayoutX(x - 14);
        marker.setLayoutY(y - 14);
        marker.setEffect(new DropShadow(8, 0, 3, Color.rgb(0, 0, 0, 0.35)));
        marker.setStyle("-fx-cursor: hand;");

        Tooltip tooltip = new Tooltip(name + "\n" + formatCategory(category));
        Tooltip.install(marker, tooltip);

        marker.setOnMouseClicked(e -> {
            if (clickListener != null) {
                clickListener.onPoiClick(name, formatCategory(category), description);
            }
            e.consume();
        });

        // Hover animation
        marker.setOnMouseEntered(e -> {
            marker.setScaleX(1.2);
            marker.setScaleY(1.2);
        });
        marker.setOnMouseExited(e -> {
            marker.setScaleX(1.0);
            marker.setScaleY(1.0);
        });

        PoiItem item = new PoiItem(name, category, description, x, y, marker);
        poiItems.add(item);
        container.getChildren().add(marker);
    }

    private String formatCategory(PoiCategory category) {
        return switch (category) {
            case FOOD -> "Food & Dining";
            case PRINTER -> "Print & Photocopy";
            case RESTROOM -> "Restroom Facility";
            case PARKING -> "Parking & Cycle Stand";
            case ACCESSIBILITY -> "Wheelchair Accessible Ramp";
            case SHUTTLE -> "Campus Shuttle Stop";
        };
    }

    public void refresh() {
        for (PoiItem item : poiItems) {
            boolean visible = switch (item.category) {
                case FOOD -> settings.isShowFood();
                case PRINTER -> settings.isShowPrinters();
                case RESTROOM -> settings.isShowRestrooms();
                case PARKING -> settings.isShowParking() || (settings.getTransportMode() == AppSettings.TransportMode.BIKE && item.name.contains("Cycle"));
                case ACCESSIBILITY -> settings.isAvoidStairs();
                case SHUTTLE -> settings.getTransportMode() == AppSettings.TransportMode.SHUTTLE;
            };
            item.markerNode.setVisible(visible);
            item.markerNode.setManaged(visible);
        }
    }

    public Pane getContainer() {
        return container;
    }
}
