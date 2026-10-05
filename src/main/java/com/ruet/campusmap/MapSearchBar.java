package com.ruet.campusmap;

import com.ruet.campusmap.model.BuildingPolygon;
import com.ruet.campusmap.model.RoomLabel;
import com.ruet.campusmap.model.RoomLocation;
import com.ruet.campusmap.model.SearchSuggestion;
import com.ruet.campusmap.model.Teacher;
import com.ruet.campusmap.service.RoomRegistry;
import com.ruet.campusmap.service.TeacherDataLoader;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Component responsible for creating an authentic Google Maps-style
 * pill-rounded floating search bar with icons and dropdown suggestions
 * supporting campus places, faculty/teachers, and floor plan rooms.
 */
public class MapSearchBar {

    private final VBox wrapper;
    private final HBox searchCard;
    private final TextField searchField;
    private final ListView<SearchSuggestion> suggestionListView;
    private final ObservableList<SearchSuggestion> suggestions;
    private final SVGPath searchIcon;
    private final SVGPath clearIcon;
    private final SVGPath directionsIcon;
    private final Separator separator;
    private final Group mapGroup;
    private final StackPane rootPane;

    private Consumer<Teacher> onTeacherSelected;
    private Consumer<String> onLocationSelected;
    private Consumer<RoomLocation> onRoomSelected;
    private boolean isDark = false;
    private final List<BuildingPolygon> registeredBuildings = new ArrayList<>();

    // Searchable coordinates for campus locations: [X, Y] center point on the SVG map (calibrated for ruet-campus-map-refined-v2.svg)
    private final Map<String, double[]> locationCoordinates = new HashMap<>(Map.ofEntries(
        Map.entry("Central Library", new double[]{1362.0, 883.0}),
        Map.entry("CSE Department", new double[]{811.0, 388.0}),
        Map.entry("Auditorium", new double[]{1283.0, 838.0}),
        Map.entry("Admin Building", new double[]{570.0, 798.0}),
        Map.entry("Cafeteria", new double[]{414.0, 1222.0}),
        Map.entry("Shahid Shahidul Islam Hall", new double[]{1706.0, 574.0}),
        Map.entry("Academic Building 1", new double[]{721.0, 1299.0}),
        Map.entry("Academic Building 2 (EEE)", new double[]{908.0, 1321.0}),
        Map.entry("Academic Building 3 (CME)", new double[]{1102.0, 1314.0}),
        Map.entry("Academic Building 4 (Civil)", new double[]{791.0, 560.0}),
        Map.entry("Central Field", new double[]{1750.0, 1068.0}),
        Map.entry("Central Pond", new double[]{960.0, 780.0}),
        Map.entry("Main Gate", new double[]{268.0, 904.0}),
        Map.entry("Shahid Minar", new double[]{317.0, 1055.0}),
        Map.entry("Shahid Ziaur Rahman Hall", new double[]{1353.0, 444.0}),
        Map.entry("Shahid Abdul Hamid Hall", new double[]{1606.0, 440.0}),
        Map.entry("Sher-e-Bangla Hall", new double[]{1955.0, 613.0}),
        Map.entry("Shahid Lt. Selim Hall", new double[]{2268.0, 520.0}),
        Map.entry("Food Court", new double[]{2268.0, 801.0}),
        Map.entry("Medical Center", new double[]{2267.0, 953.0})
    ));

    public void registerBuildings(List<BuildingPolygon> buildings) {
        if (buildings == null) return;
        registeredBuildings.clear();
        registeredBuildings.addAll(buildings);
        for (BuildingPolygon bp : buildings) {
            indexBuildingCoordinates(bp);
        }
    }

    public void addOrUpdateBuilding(BuildingPolygon bp) {
        if (bp == null) return;
        registeredBuildings.removeIf(b -> b == bp || (b.getName() != null && b.getName().equalsIgnoreCase(bp.getName())));
        registeredBuildings.add(bp);
        indexBuildingCoordinates(bp);
    }

    public void removeBuilding(BuildingPolygon bp) {
        if (bp == null) return;
        registeredBuildings.removeIf(b -> b == bp || (b.getName() != null && b.getName().equalsIgnoreCase(bp.getName())));
        if (bp.getName() != null) {
            locationCoordinates.remove(bp.getName());
        }
    }

    private void indexBuildingCoordinates(BuildingPolygon bp) {
        if (bp == null || bp.getName() == null || bp.getPoints() == null || bp.getPoints().isEmpty()) return;
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
            locationCoordinates.put(bp.getName(), new double[]{sumX / count, sumY / count});
        }
    }

    public MapSearchBar(Group mapGroup, StackPane rootPane) {
        this.mapGroup = mapGroup;
        this.rootPane = rootPane;

        // --- 1. Left Magnifying Glass Icon (Google Maps Style) ---
        searchIcon = new SVGPath();
        searchIcon.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        searchIcon.setFill(Color.web("#5f6368"));
        searchIcon.setScaleX(0.9);
        searchIcon.setScaleY(0.9);

        // --- 2. Clean Borderless Input Field ---
        searchField = new TextField();
        searchField.setPromptText("Search RUET campus, buildings, or teachers...");
        searchField.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-border-color: transparent; " +
            "-fx-font-size: 14px; " +
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
        directionsIcon.setFill(Color.web("#1a73e8"));
        directionsIcon.setScaleX(1.1);
        directionsIcon.setScaleY(1.1);

        // --- 5. The Rounded Pill Search Bar Card ---
        searchCard = new HBox(10, searchIcon, searchField, clearIcon, separator, directionsIcon);
        searchCard.setAlignment(Pos.CENTER_LEFT);
        searchCard.setPrefWidth(400);
        searchCard.setPrefHeight(48);
        searchCard.setPadding(new Insets(0, 16, 0, 16));

        // --- 6. Dropdown Suggestions List with Rich Visual Cells ---
        suggestions = FXCollections.observableArrayList();
        suggestionListView = new ListView<>(suggestions);
        suggestionListView.setPrefWidth(400);
        suggestionListView.setMaxHeight(220);
        suggestionListView.setVisible(false);
        suggestionListView.setManaged(false);

        suggestionListView.setCellFactory(lv -> new ListCell<SearchSuggestion>() {
            @Override
            protected void updateItem(SearchSuggestion item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    HBox row = new HBox(8);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(4, 6, 4, 6));

                    String icon = switch (item.getType()) {
                        case TEACHER -> "👨‍🏫";
                        case ROOM -> "🚪";
                        case LOCATION -> "📍";
                    };
                    Label iconLabel = new Label(icon);
                    iconLabel.setStyle("-fx-font-size: 13px;");

                    VBox textCol = new VBox(1);
                    HBox.setHgrow(textCol, Priority.ALWAYS);

                    Label title = new Label(item.getTitle());
                    title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + (isDark ? "#e8eaed;" : "#202124;"));

                    Label sub = new Label(item.getSubtitle());
                    sub.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (isDark ? "#9aa0a6;" : "#70757a;"));
                    textCol.getChildren().addAll(title, sub);

                    String badgeText = switch (item.getType()) {
                        case TEACHER -> "FACULTY";
                        case ROOM -> "ROOM";
                        case LOCATION -> "PLACE";
                    };
                    Label badge = new Label(badgeText);
                    badge.setStyle(
                        "-fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-background-radius: 6px; " +
                        switch (item.getType()) {
                            case TEACHER -> (isDark ? "-fx-background-color: #173154; -fx-text-fill: #8ab4f8;" : "-fx-background-color: #e8f0fe; -fx-text-fill: #1a73e8;");
                            case ROOM -> (isDark ? "-fx-background-color: #0d3c26; -fx-text-fill: #81c995;" : "-fx-background-color: #e6f4ea; -fx-text-fill: #137333;");
                            case LOCATION -> (isDark ? "-fx-background-color: #35373a; -fx-text-fill: #9aa0a6;" : "-fx-background-color: #f1f3f4; -fx-text-fill: #5f6368;");
                        }
                    );

                    row.getChildren().addAll(iconLabel, textCol, badge);
                    setGraphic(row);
                    setText(null);
                    setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                }
            }
        });

        // --- 7. Main Floating Wrapper Container ---
        wrapper = new VBox(searchCard, suggestionListView);
        wrapper.setMaxSize(440, VBox.USE_PREF_SIZE);
        wrapper.setEffect(new DropShadow(15, 0, 4, Color.rgb(60, 64, 67, 0.28)));
        wrapper.setPadding(new Insets(18, 0, 0, 0));

        // Prevent dragging search bar from moving campus map underneath
        wrapper.setOnMousePressed(javafx.event.Event::consume);
        wrapper.setOnMouseDragged(javafx.event.Event::consume);

        // Align floating at top-center
        StackPane.setAlignment(wrapper, Pos.TOP_CENTER);

        applyTheme(false);

        // --- 8. Reactive Event Listeners ---
        setupTextListener();
        setupSelectionListener();
        setupEnterKeyListener();
    }

    public void setOnTeacherSelected(Consumer<Teacher> onTeacherSelected) {
        this.onTeacherSelected = onTeacherSelected;
    }

    public void setOnLocationSelected(Consumer<String> onLocationSelected) {
        this.onLocationSelected = onLocationSelected;
    }

    public void setOnRoomSelected(Consumer<RoomLocation> onRoomSelected) {
        this.onRoomSelected = onRoomSelected;
    }

    public void applyTheme(boolean isDark) {
        this.isDark = isDark;
        if (isDark) {
            searchField.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-border-color: transparent; " +
                "-fx-font-size: 14px; " +
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
                "-fx-font-size: 13px; " +
                "-fx-font-family: 'Segoe UI', Roboto, sans-serif;"
            );
        } else {
            searchField.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-border-color: transparent; " +
                "-fx-font-size: 14px; " +
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
                "-fx-font-size: 13px; " +
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
     * Listens to text changes dynamically (Auto-suggest teachers and locations).
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
                List<SearchSuggestion> matches = new ArrayList<>();

                // 1. Search faculty/teachers first
                List<Teacher> facultyMatches = TeacherDataLoader.searchTeachers(query);
                for (Teacher t : facultyMatches) {
                    String sub = (t.getDesignation() != null ? t.getDesignation() : "") +
                        (t.getDepartment() != null ? " • " + t.getDepartment() : "") +
                        (t.getBuildingName() != null ? " [" + t.getBuildingName() + "]" : "") +
                        (t.getRoomNumber() != null ? " • " + t.getRoomNumber() : "");
                    matches.add(new SearchSuggestion(SearchSuggestion.Type.TEACHER, t.getName(), sub, t));
                }

                // 2. Search inner rooms (room number, room name, type, occupants)
                List<RoomLocation> roomMatches = RoomRegistry.searchRooms(query);
                for (RoomLocation rl : roomMatches) {
                    RoomLabel r = rl.room();
                    String title = r.getRoomNumber() != null && !r.getRoomNumber().isBlank()
                        ? (r.getName() != null && !r.getName().isBlank() ? "Room " + r.getRoomNumber() + " - " + r.getName() : "Room " + r.getRoomNumber())
                        : (r.getName() != null ? r.getName() : "Room");
                    String sub = rl.building().getBuildingName() + " • " + rl.floor().getFloorName();
                    if (!r.getOccupants().isEmpty()) {
                        sub += " • 👤 " + String.join(", ", r.getOccupants());
                    }
                    matches.add(new SearchSuggestion(SearchSuggestion.Type.ROOM, title, sub, rl));
                }

                // 3. Search campus buildings (by name or code name)
                java.util.Set<String> matchedBuildingNames = new java.util.HashSet<>();
                for (BuildingPolygon bp : registeredBuildings) {
                    if (bp == null || bp.getName() == null) continue;
                    boolean matchesName = bp.getName().toLowerCase().contains(query);
                    boolean matchesCode = bp.getCodeName() != null && !bp.getCodeName().isBlank() && bp.getCodeName().toLowerCase().contains(query);

                    if (matchesName || matchesCode) {
                        matchedBuildingNames.add(bp.getName().toLowerCase());
                        String sub = (bp.getCodeName() != null && !bp.getCodeName().isBlank())
                            ? "Campus Building [" + bp.getCodeName() + "]"
                            : "Campus Building / Landmark";
                        matches.add(new SearchSuggestion(SearchSuggestion.Type.LOCATION, bp.getName(), sub, bp.getName()));
                    }
                }

                // 4. Search other campus landmarks / POIs
                for (String locName : locationCoordinates.keySet()) {
                    if (!matchedBuildingNames.contains(locName.toLowerCase()) && locName.toLowerCase().contains(query)) {
                        matches.add(new SearchSuggestion(SearchSuggestion.Type.LOCATION, locName, "Campus Building / Landmark", locName));
                    }
                }

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
     * Listens to item selection in the suggestions dropdown.
     */
    private void setupSelectionListener() {
        suggestionListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selectedItem) -> {
            if (selectedItem != null) {
                handleSuggestionChosen(selectedItem);
            }
        });
    }

    /**
     * Listens to keyboard Enter in the search bar.
     */
    private void setupEnterKeyListener() {
        searchField.setOnAction(event -> {
            String query = searchField.getText();
            if (query != null && !query.trim().isEmpty()) {
                String trimmed = query.trim().toLowerCase();
                if (!suggestions.isEmpty()) {
                    handleSuggestionChosen(suggestions.get(0));
                    return;
                }

                // Fallback check
                List<Teacher> teachers = TeacherDataLoader.searchTeachers(trimmed);
                if (!teachers.isEmpty()) {
                    Teacher t = teachers.get(0);
                    handleSuggestionChosen(new SearchSuggestion(SearchSuggestion.Type.TEACHER, t.getName(), t.getBuildingName(), t));
                    return;
                }

                List<RoomLocation> roomLocs = RoomRegistry.searchRooms(trimmed);
                if (!roomLocs.isEmpty()) {
                    RoomLocation rl = roomLocs.get(0);
                    handleSuggestionChosen(new SearchSuggestion(SearchSuggestion.Type.ROOM, rl.room().getDisplayTitle(), rl.building().getBuildingName(), rl));
                    return;
                }

                locationCoordinates.keySet().stream()
                    .filter(name -> name.equalsIgnoreCase(trimmed) || name.toLowerCase().contains(trimmed))
                    .findFirst()
                    .ifPresent(matched -> handleSuggestionChosen(new SearchSuggestion(SearchSuggestion.Type.LOCATION, matched, "Campus Location", matched)));
            }
        });
    }

    private void handleSuggestionChosen(SearchSuggestion item) {
        if (item == null) return;
        hideDropdown();

        if (item.getType() == SearchSuggestion.Type.TEACHER) {
            Teacher teacher = (Teacher) item.getPayload();
            searchField.setText(teacher.getName());
            if (teacher.getBuildingName() != null && !teacher.getBuildingName().isBlank()) {
                focusOnLocation(teacher.getBuildingName(), mapGroup, rootPane);
            }
            if (onTeacherSelected != null) {
                onTeacherSelected.accept(teacher);
            }
        } else if (item.getType() == SearchSuggestion.Type.ROOM) {
            RoomLocation roomLoc = (RoomLocation) item.getPayload();
            searchField.setText(roomLoc.room().getDisplayTitle());
            if (onRoomSelected != null) {
                onRoomSelected.accept(roomLoc);
            }
        } else {
            String locName = (String) item.getPayload();
            searchField.setText(locName);
            focusOnLocation(locName, mapGroup, rootPane);
            if (onLocationSelected != null) {
                onLocationSelected.accept(locName);
            }
        }
    }

    /**
     * Centers map view on selected location and applies smooth camera fly-to transition with 2.0x zoom.
     */
    public void flyToLocation(String locationName) {
        focusOnLocation(locationName, mapGroup, rootPane);
    }

    private void focusOnLocation(String locationName, Group mapGroup, StackPane rootPane) {
        if (locationName == null) return;
        double[] coords = locationCoordinates.get(locationName);
        if (coords == null) {
            // Fuzzy search for matched building coordinates
            for (Map.Entry<String, double[]> entry : locationCoordinates.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(locationName) ||
                    entry.getKey().toLowerCase().contains(locationName.toLowerCase()) ||
                    locationName.toLowerCase().contains(entry.getKey().toLowerCase())) {
                    coords = entry.getValue();
                    break;
                }
            }
        }
        if (coords == null) return;

        double targetX = coords[0];
        double targetY = coords[1];
        double mapWidth = 4190.0;
        double mapHeight = 1720.0;
        double targetScale = 1.35;

        double viewWidth = rootPane.getWidth();
        double viewHeight = rootPane.getHeight();

        double targetTx = (mapWidth / 2.0 - targetX) * targetScale;
        double targetTy = (mapHeight / 2.0 - targetY) * targetScale;

        double scaledWidth = mapWidth * targetScale;
        double scaledHeight = mapHeight * targetScale;
        double halfExcessX = Math.max(0.0, (scaledWidth - viewWidth) / 2.0);
        double halfExcessY = Math.max(0.0, (scaledHeight - viewHeight) / 2.0);
        targetTx = Math.max(-halfExcessX, Math.min(halfExcessX, targetTx));
        targetTy = Math.max(-halfExcessY, Math.min(halfExcessY, targetTy));

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
