package com.ruet.campusmap.editor;

import com.ruet.campusmap.model.BuildingPolygon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Modal dialog to edit or delete an existing building polygon hitbox.
 */
public class EditBuildingDialog {

    public interface SaveCallback {
        void onSave(String newName, String newCodeName, String newColor, boolean visibleToUsers);
    }

    public static void show(
        BuildingPolygon model,
        SaveCallback onSave,
        Runnable onDelete
    ) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Edit Building");

        Label titleLabel = new Label("Edit Building Information");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #202124;");

        // 1. Name input
        Label nameLabel = new Label("Building Name *:");
        nameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5f6368; -fx-font-weight: bold;");

        TextField nameField = new TextField(model.getName() != null ? model.getName() : "");
        nameField.setPromptText("Enter building name");
        nameField.setStyle("-fx-font-size: 13px; -fx-background-radius: 8px; -fx-border-color: #dadce0; -fx-border-radius: 8px; -fx-padding: 6 10;");

        // 2. Code Name input
        Label codeLabel = new Label("Building Code Name (e.g. CSE, AUD, ME):");
        codeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5f6368; -fx-font-weight: bold;");

        TextField codeField = new TextField(model.getCodeName() != null ? model.getCodeName() : "");
        codeField.setPromptText("e.g. CSE (shown on map badges)");
        codeField.setStyle("-fx-font-size: 13px; -fx-background-radius: 8px; -fx-border-color: #dadce0; -fx-border-radius: 8px; -fx-padding: 6 10;");

        // 3. Color input
        Label colorLabel = new Label("Polygon Color:");
        colorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5f6368; -fx-font-weight: bold;");

        Color initialColor;
        try {
            initialColor = Color.web(model.getColor() != null ? model.getColor() : "#3498DB");
        } catch (Exception e) {
            initialColor = Color.web("#3498DB");
        }

        ColorPicker colorPicker = new ColorPicker(initialColor);
        colorPicker.setMaxWidth(Double.MAX_VALUE);
        colorPicker.setStyle("-fx-background-radius: 8px; -fx-cursor: hand;");

        // Color preset chips for quick theme selection
        HBox presetChips = new HBox(6);
        presetChips.setAlignment(Pos.CENTER_LEFT);
        String[] presets = {"#1A73E8", "#00897B", "#E65100", "#D93025", "#8E24AA", "#1E8E3E", "#F9AB00"};
        for (String hex : presets) {
            Button chip = new Button();
            chip.setPrefSize(22, 22);
            chip.setMinSize(22, 22);
            chip.setMaxSize(22, 22);
            chip.setStyle("-fx-background-color: " + hex + "; -fx-background-radius: 50%; -fx-cursor: hand; -fx-border-color: #dadce0; -fx-border-radius: 50%;");
            chip.setOnAction(e -> colorPicker.setValue(Color.web(hex)));
            presetChips.getChildren().add(chip);
        }

        VBox colorSection = new VBox(6, colorLabel, colorPicker, presetChips);

        // Visibility checkbox (boxes default to hidden for regular users)
        CheckBox visibleCheck = new CheckBox("Visible to regular users");
        visibleCheck.setSelected(model.isVisibleToUsers());
        visibleCheck.setStyle("-fx-font-size: 12px; -fx-text-fill: #202124; -fx-cursor: hand; -fx-font-weight: bold;");

        Label visibleHint = new Label("Unchecked: Clean invisible hitbox for users. Checked: Shows outline on map.");
        visibleHint.setWrapText(true);
        visibleHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #70757a;");

        VBox visibilityCard = new VBox(4, visibleCheck, visibleHint);
        visibilityCard.setPadding(new Insets(10));
        visibilityCard.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8px; -fx-border-color: #dadce0; -fx-border-radius: 8px;");

        // Buttons
        Button saveBtn = new Button("Save Changes");
        saveBtn.setStyle(
            "-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-background-radius: 8px; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px;"
        );
        saveBtn.setOnAction(e -> {
            String newName = nameField.getText().trim();
            if (newName.isEmpty()) {
                nameField.setStyle("-fx-border-color: #d93025; -fx-background-radius: 8px; -fx-border-radius: 8px; -fx-padding: 6 10;");
                return;
            }
            String newCode = codeField.getText() != null ? codeField.getText().trim().toUpperCase() : "";
            Color picked = colorPicker.getValue();
            String hex = String.format("#%02X%02X%02X",
                (int)(picked.getRed() * 255),
                (int)(picked.getGreen() * 255),
                (int)(picked.getBlue() * 255)
            );
            boolean isVisible = visibleCheck.isSelected();
            dialog.close();
            if (onSave != null) {
                onSave.onSave(newName, newCode, hex, isVisible);
            }
        });

        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle(
            "-fx-background-color: #fce8e6; -fx-text-fill: #c5221f; -fx-font-weight: bold; " +
            "-fx-background-radius: 8px; -fx-padding: 8 12; -fx-cursor: hand; -fx-font-size: 12px;"
        );
        deleteBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Delete");
            confirm.setHeaderText("Delete '" + model.getName() + "'?");
            confirm.setContentText("Are you sure you want to delete this building polygon? This cannot be undone.");
            var res = confirm.showAndWait();
            if (res.isPresent() && res.get() == ButtonType.OK) {
                dialog.close();
                if (onDelete != null) {
                    onDelete.run();
                }
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #5f6368; " +
            "-fx-background-radius: 8px; -fx-padding: 8 12; -fx-cursor: hand; -fx-font-size: 12px;"
        );
        cancelBtn.setOnAction(e -> dialog.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox buttonBar = new HBox(8, deleteBtn, spacer, cancelBtn, saveBtn);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        VBox layout = new VBox(12,
            titleLabel,
            new VBox(4, nameLabel, nameField),
            new VBox(4, codeLabel, codeField),
            colorSection,
            visibilityCard,
            buttonBar
        );
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: white;");

        dialog.setScene(new Scene(layout, 410, 440));
        dialog.setResizable(false);
        dialog.show();
    }
}
