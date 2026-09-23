package com.ruet.campusmap.editor;

import com.ruet.campusmap.model.BuildingPolygon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Slide-out / floating inspector drawer displaying all campus buildings and layers.
 * Provides quick search, visibility badges, edit, delete, and focus actions.
 */
public class AdminBuildingsDrawer {

    private final VBox container;
    private final VBox listContainer;
    private final Label countBadge;
    private final TextField searchField;
    private final ScrollPane scrollPane;

    private List<BuildingPolygon> allBuildings = new ArrayList<>();
    private Consumer<BuildingPolygon> onEditAction;
    private Consumer<BuildingPolygon> onDeleteAction;
    private Consumer<BuildingPolygon> onFocusAction;

    private boolean isDark = false;

    public AdminBuildingsDrawer() {
        container = new VBox(12);
        container.setPrefWidth(330);
        container.setMaxWidth(330);
        container.setMaxHeight(480);
        container.setPadding(new Insets(16));
        container.setEffect(new DropShadow(18, 0, 5, Color.rgb(0, 0, 0, 0.22)));
        container.setVisible(false);
        container.setManaged(false);

        // Prevent dragging on the drawer from panning the map underneath
        container.setOnMousePressed(javafx.event.Event::consume);
        container.setOnMouseDragged(javafx.event.Event::consume);

        // 1. Header (Title, Count Badge, Close)
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Campus Buildings");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-font-family: 'Segoe UI', Roboto, sans-serif;");
        HBox.setHgrow(title, Priority.ALWAYS);

        countBadge = new Label("0");
        countBadge.setStyle(
            "-fx-background-color: #e8f0fe; -fx-text-fill: #1a73e8; " +
            "-fx-background-radius: 10px; -fx-padding: 2 8; -fx-font-weight: bold; -fx-font-size: 11px;"
        );

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 13px; -fx-font-weight: bold;");
        closeBtn.setOnAction(e -> hide());

        header.getChildren().addAll(title, countBadge, closeBtn);

        // 2. Search Filter Field
        searchField = new TextField();
        searchField.setPromptText("Filter buildings by name...");
        searchField.setStyle(
            "-fx-background-color: #f1f3f4; -fx-background-radius: 8px; " +
            "-fx-border-color: #dadce0; -fx-border-radius: 8px; -fx-padding: 6 10; -fx-font-size: 12px;"
        );
        searchField.textProperty().addListener((obs, oldText, newText) -> filterList(newText));

        // 3. Scrollable List of Buildings
        listContainer = new VBox(8);
        listContainer.setFillWidth(true);

        scrollPane = new ScrollPane(listContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        scrollPane.setPrefHeight(320);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        container.getChildren().addAll(header, searchField, scrollPane);

        StackPane.setAlignment(container, Pos.TOP_RIGHT);
        StackPane.setMargin(container, new Insets(74, 18, 0, 0));

        applyTheme(false);
    }

    public void setCallbacks(
        Consumer<BuildingPolygon> onEdit,
        Consumer<BuildingPolygon> onDelete,
        Consumer<BuildingPolygon> onFocus
    ) {
        this.onEditAction = onEdit;
        this.onDeleteAction = onDelete;
        this.onFocusAction = onFocus;
    }

    public void refreshData(List<BuildingPolygon> buildings) {
        this.allBuildings = new ArrayList<>(buildings);
        countBadge.setText(String.valueOf(allBuildings.size()));
        filterList(searchField.getText());
    }

    private void filterList(String filter) {
        listContainer.getChildren().clear();
        String query = filter != null ? filter.trim().toLowerCase() : "";

        List<BuildingPolygon> matches = allBuildings.stream()
            .filter(b -> query.isEmpty() || (b.getName() != null && b.getName().toLowerCase().contains(query)))
            .toList();

        if (matches.isEmpty()) {
            VBox emptyBox = new VBox(6);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(24, 8, 24, 8));

            Label emptyLabel = new Label(query.isEmpty() ? "No buildings defined yet" : "No matching buildings");
            emptyLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #70757a; -fx-font-weight: bold;");

            Label emptySub = new Label(query.isEmpty() ? "Click 'Draw Box' in the toolbar to create one" : "Try a different search term");
            emptySub.setStyle("-fx-font-size: 11px; -fx-text-fill: #9aa0a6;");

            emptyBox.getChildren().addAll(emptyLabel, emptySub);
            listContainer.getChildren().add(emptyBox);
            return;
        }

        for (BuildingPolygon bp : matches) {
            listContainer.getChildren().add(createBuildingCard(bp));
        }
    }

    private Node createBuildingCard(BuildingPolygon bp) {
        HBox card = new HBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(8, 10, 8, 10));
        card.setStyle(
            "-fx-background-color: " + (isDark ? "#2d2f31;" : "#f8f9fa;") +
            "-fx-background-radius: 8px; " +
            "-fx-border-color: " + (isDark ? "#3c4043;" : "#e8eaed;") +
            "-fx-border-radius: 8px; -fx-border-width: 1px;"
        );

        // Color Swatch Circle
        Color color;
        try {
            color = Color.web(bp.getColor() != null ? bp.getColor() : "#3498DB");
        } catch (Exception e) {
            color = Color.web("#3498DB");
        }
        Circle swatch = new Circle(7, color);
        swatch.setStroke(Color.WHITE);
        swatch.setStrokeWidth(1.0);

        // Name & Meta
        VBox textCol = new VBox(2);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        Label nameLabel = new Label(bp.getName() != null ? bp.getName() : "Unnamed");
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: " + (isDark ? "#e8eaed;" : "#202124;"));

        int ptCount = bp.getPoints() != null ? bp.getPoints().size() : 0;
        String metaText = ptCount + " corners • " + (bp.isVisibleToUsers() ? "Visible to users" : "Hidden from users");
        Label metaLabel = new Label(metaText);
        metaLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + (isDark ? "#9aa0a6;" : "#70757a;"));

        textCol.getChildren().addAll(nameLabel, metaLabel);

        // Action Buttons: Edit (Pencil), Delete (Trash)
        Button editBtn = createIconButton("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z", "#1a73e8", "Edit Building");
        editBtn.setOnAction(e -> {
            if (onEditAction != null) {
                onEditAction.accept(bp);
            }
        });

        Button deleteBtn = createIconButton("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z", "#d93025", "Delete Building");
        deleteBtn.setOnAction(e -> {
            if (onDeleteAction != null) {
                onDeleteAction.accept(bp);
            }
        });

        card.getChildren().addAll(swatch, textCol, editBtn, deleteBtn);

        // Hover effect on the card
        card.setOnMouseEntered(e -> {
            card.setStyle(
                "-fx-background-color: " + (isDark ? "#35373a;" : "#f1f3f4;") +
                "-fx-background-radius: 8px; " +
                "-fx-border-color: " + (isDark ? "#8ab4f8;" : "#1a73e8;") +
                "-fx-border-radius: 8px; -fx-border-width: 1px; -fx-cursor: hand;"
            );
        });

        card.setOnMouseExited(e -> {
            card.setStyle(
                "-fx-background-color: " + (isDark ? "#2d2f31;" : "#f8f9fa;") +
                "-fx-background-radius: 8px; " +
                "-fx-border-color: " + (isDark ? "#3c4043;" : "#e8eaed;") +
                "-fx-border-radius: 8px; -fx-border-width: 1px;"
            );
        });

        // Clicking card focuses on building
        card.setOnMouseClicked(e -> {
            if (e.getTarget() != editBtn && e.getTarget() != deleteBtn) {
                if (onFocusAction != null) {
                    onFocusAction.accept(bp);
                }
            }
        });

        return card;
    }

    private Button createIconButton(String svgPathData, String colorHex, String tooltipText) {
        SVGPath icon = new SVGPath();
        icon.setContent(svgPathData);
        icon.setFill(Color.web(colorHex));
        icon.setScaleX(0.75);
        icon.setScaleY(0.75);

        Button btn = new Button();
        btn.setGraphic(icon);
        btn.setPrefSize(28, 28);
        btn.setMinSize(28, 28);
        btn.setMaxSize(28, 28);
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;");
        Tooltip.install(btn, new Tooltip(tooltipText));

        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: rgba(0,0,0,0.06); -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 0;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;"));

        return btn;
    }

    public void show() {
        container.setVisible(true);
        container.setManaged(true);
        container.toFront();
    }

    public void hide() {
        container.setVisible(false);
        container.setManaged(false);
    }

    public void toggle() {
        if (isVisible()) {
            hide();
        } else {
            show();
        }
    }

    public boolean isVisible() {
        return container.isVisible();
    }

    public VBox getContainer() {
        return container;
    }

    public void applyTheme(boolean isDark) {
        this.isDark = isDark;
        if (isDark) {
            container.setStyle(
                "-fx-background-color: #202124; " +
                "-fx-background-radius: 14px; " +
                "-fx-border-color: #3c4043; " +
                "-fx-border-radius: 14px; " +
                "-fx-border-width: 1px;"
            );
            searchField.setStyle(
                "-fx-background-color: #2d2f31; -fx-background-radius: 8px; " +
                "-fx-border-color: #3c4043; -fx-border-radius: 8px; -fx-padding: 6 10; -fx-font-size: 12px; -fx-text-fill: #e8eaed;"
            );
        } else {
            container.setStyle(
                "-fx-background-color: #ffffff; " +
                "-fx-background-radius: 14px; " +
                "-fx-border-color: #dadce0; " +
                "-fx-border-radius: 14px; " +
                "-fx-border-width: 1px;"
            );
            searchField.setStyle(
                "-fx-background-color: #f1f3f4; -fx-background-radius: 8px; " +
                "-fx-border-color: #dadce0; -fx-border-radius: 8px; -fx-padding: 6 10; -fx-font-size: 12px; -fx-text-fill: #202124;"
            );
        }
        filterList(searchField.getText());
    }
}
