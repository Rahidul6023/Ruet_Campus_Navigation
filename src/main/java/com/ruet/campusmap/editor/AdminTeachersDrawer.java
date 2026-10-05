package com.ruet.campusmap.editor;

import com.ruet.campusmap.model.Teacher;
import com.ruet.campusmap.service.TeacherDataLoader;
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
 * Slide-out / floating inspector drawer displaying all RUET faculty/teachers.
 * Provides quick search, add, edit, delete, and focus on building actions.
 */
public class AdminTeachersDrawer {

    private final VBox container;
    private final VBox listContainer;
    private final Label countBadge;
    private final TextField searchField;
    private final ScrollPane scrollPane;
    private final Button addTeacherBtn;

    private List<Teacher> allTeachers = new ArrayList<>();
    private Consumer<Teacher> onEditAction;
    private Consumer<Teacher> onDeleteAction;
    private Consumer<Teacher> onFocusAction;
    private Runnable onAddAction;

    private boolean isDark = false;

    public AdminTeachersDrawer() {
        container = new VBox(12);
        container.setPrefWidth(350);
        container.setMaxWidth(350);
        container.setMaxHeight(500);
        container.setPadding(new Insets(16));
        container.setEffect(new DropShadow(18, 0, 5, Color.rgb(0, 0, 0, 0.22)));
        container.setVisible(false);
        container.setManaged(false);

        // Prevent dragging on the drawer from panning the map underneath
        container.setOnMousePressed(javafx.event.Event::consume);
        container.setOnMouseDragged(javafx.event.Event::consume);

        // 1. Header (Title, Count Badge, + Add Button, Close Button)
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Teachers & Faculty");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-font-family: 'Segoe UI', Roboto, sans-serif;");
        HBox.setHgrow(title, Priority.ALWAYS);

        countBadge = new Label("0");
        countBadge.setStyle(
            "-fx-background-color: #e8f0fe; -fx-text-fill: #1a73e8; " +
            "-fx-background-radius: 10px; -fx-padding: 2 8; -fx-font-weight: bold; -fx-font-size: 11px;"
        );

        addTeacherBtn = new Button("+ Add");
        addTeacherBtn.setStyle(
            "-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-background-radius: 6px; -fx-font-size: 11px; -fx-padding: 4 10; -fx-cursor: hand;"
        );
        addTeacherBtn.setOnAction(e -> {
            if (onAddAction != null) {
                onAddAction.run();
            }
        });

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 13px; -fx-font-weight: bold;");
        closeBtn.setOnAction(e -> hide());

        header.getChildren().addAll(title, countBadge, addTeacherBtn, closeBtn);

        // 2. Search Filter Field
        searchField = new TextField();
        searchField.setPromptText("Filter by name, dept, building, room...");
        searchField.setStyle(
            "-fx-background-color: #f1f3f4; -fx-background-radius: 8px; " +
            "-fx-border-color: #dadce0; -fx-border-radius: 8px; -fx-padding: 6 10; -fx-font-size: 12px;"
        );
        searchField.textProperty().addListener((obs, oldText, newText) -> filterList(newText));

        // 3. Scrollable List of Teachers
        listContainer = new VBox(8);
        listContainer.setFillWidth(true);

        scrollPane = new ScrollPane(listContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        scrollPane.setPrefHeight(340);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        container.getChildren().addAll(header, searchField, scrollPane);

        StackPane.setAlignment(container, Pos.TOP_RIGHT);
        StackPane.setMargin(container, new Insets(74, 18, 0, 0));

        applyTheme(false);
    }

    public void setCallbacks(
        Consumer<Teacher> onEdit,
        Consumer<Teacher> onDelete,
        Consumer<Teacher> onFocus,
        Runnable onAdd
    ) {
        this.onEditAction = onEdit;
        this.onDeleteAction = onDelete;
        this.onFocusAction = onFocus;
        this.onAddAction = onAdd;
    }

    public void refreshData(List<Teacher> teachers) {
        this.allTeachers = new ArrayList<>(teachers);
        countBadge.setText(String.valueOf(allTeachers.size()));
        filterList(searchField.getText());
    }

    private void filterList(String filter) {
        listContainer.getChildren().clear();
        String query = filter != null ? filter.trim().toLowerCase() : "";

        List<Teacher> matches = allTeachers.stream()
            .filter(t -> query.isEmpty() ||
                (t.getName() != null && t.getName().toLowerCase().contains(query)) ||
                (t.getDepartment() != null && t.getDepartment().toLowerCase().contains(query)) ||
                (t.getDesignation() != null && t.getDesignation().toLowerCase().contains(query)) ||
                (t.getBuildingName() != null && t.getBuildingName().toLowerCase().contains(query)) ||
                (t.getRoomNumber() != null && t.getRoomNumber().toLowerCase().contains(query)))
            .toList();

        if (matches.isEmpty()) {
            VBox emptyBox = new VBox(6);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(24, 8, 24, 8));

            Label emptyLabel = new Label(query.isEmpty() ? "No faculty records found" : "No matching teachers");
            emptyLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #70757a; -fx-font-weight: bold;");

            Label emptySub = new Label(query.isEmpty() ? "Click '+ Add' above to create one" : "Try searching by name or department");
            emptySub.setStyle("-fx-font-size: 11px; -fx-text-fill: #9aa0a6;");

            emptyBox.getChildren().addAll(emptyLabel, emptySub);
            listContainer.getChildren().add(emptyBox);
            return;
        }

        for (Teacher teacher : matches) {
            listContainer.getChildren().add(createTeacherCard(teacher));
        }
    }

    private Node createTeacherCard(Teacher teacher) {
        HBox card = new HBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(8, 10, 8, 10));
        card.setStyle(
            "-fx-background-color: " + (isDark ? "#2d2f31;" : "#f8f9fa;") +
            "-fx-background-radius: 8px; " +
            "-fx-border-color: " + (isDark ? "#3c4043;" : "#e8eaed;") +
            "-fx-border-radius: 8px; -fx-border-width: 1px;"
        );

        // Avatar Circle with initial letter
        String initial = (teacher.getName() != null && !teacher.getName().isBlank())
            ? teacher.getName().trim().substring(0, 1).toUpperCase()
            : "T";
        StackPane avatar = new StackPane();
        Circle avatarBg = new Circle(14, Color.web("#1a73e8"));
        Label avatarText = new Label(initial);
        avatarText.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");
        avatar.getChildren().addAll(avatarBg, avatarText);

        // Info Column
        VBox textCol = new VBox(2);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        Label nameLabel = new Label(teacher.getName() != null ? teacher.getName() : "Unnamed Teacher");
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: " + (isDark ? "#e8eaed;" : "#202124;"));

        String desigDept = (teacher.getDesignation() != null ? teacher.getDesignation() : "") +
            (teacher.getDepartment() != null && !teacher.getDepartment().isBlank() ? " • " + teacher.getDepartment() : "");
        Label desigLabel = new Label(desigDept);
        desigLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + (isDark ? "#9aa0a6;" : "#70757a;"));

        String locText = (teacher.getBuildingName() != null ? teacher.getBuildingName() : "No building") +
            (teacher.getRoomNumber() != null && !teacher.getRoomNumber().isBlank() ? " [" + teacher.getRoomNumber() + "]" : "");
        Label locLabel = new Label(locText);
        locLabel.setStyle(
            "-fx-font-size: 10px; -fx-font-weight: bold; " +
            "-fx-text-fill: " + (isDark ? "#8ab4f8;" : "#1a73e8;")
        );

        textCol.getChildren().addAll(nameLabel, desigLabel, locLabel);

        // Action Buttons: Edit (Pencil), Delete (Trash)
        Button editBtn = createIconButton(
            "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z",
            "#1a73e8",
            "Edit Teacher"
        );
        editBtn.setOnAction(e -> {
            if (onEditAction != null) {
                onEditAction.accept(teacher);
            }
        });

        Button deleteBtn = createIconButton(
            "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z",
            "#d93025",
            "Delete Teacher"
        );
        deleteBtn.setOnAction(e -> {
            if (onDeleteAction != null) {
                onDeleteAction.accept(teacher);
            }
        });

        card.getChildren().addAll(avatar, textCol, editBtn, deleteBtn);

        // Hover effect
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

        // Clicking card triggers focus on the teacher's building
        card.setOnMouseClicked(e -> {
            if (e.getTarget() != editBtn && e.getTarget() != deleteBtn) {
                if (onFocusAction != null) {
                    onFocusAction.accept(teacher);
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
                "-fx-background-color: #202124; -fx-background-radius: 14px; " +
                "-fx-border-color: #3c4043; -fx-border-radius: 14px; -fx-border-width: 1px;"
            );
            searchField.setStyle(
                "-fx-background-color: #303134; -fx-background-radius: 8px; " +
                "-fx-border-color: #5f6368; -fx-border-radius: 8px; -fx-padding: 6 10; " +
                "-fx-font-size: 12px; -fx-text-fill: #e8eaed; -fx-prompt-text-fill: #9aa0a6;"
            );
        } else {
            container.setStyle(
                "-fx-background-color: #ffffff; -fx-background-radius: 14px; " +
                "-fx-border-color: #dadce0; -fx-border-radius: 14px; -fx-border-width: 1px;"
            );
            searchField.setStyle(
                "-fx-background-color: #f1f3f4; -fx-background-radius: 8px; " +
                "-fx-border-color: #dadce0; -fx-border-radius: 8px; -fx-padding: 6 10; " +
                "-fx-font-size: 12px; -fx-text-fill: #202124; -fx-prompt-text-fill: #70757a;"
            );
        }
        filterList(searchField.getText());
    }
}
