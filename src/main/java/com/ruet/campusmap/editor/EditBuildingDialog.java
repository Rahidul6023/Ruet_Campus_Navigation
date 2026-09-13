package com.ruet.campusmap.editor;

import com.ruet.campusmap.model.BuildingPolygon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.function.BiConsumer;

/**
 * Modal dialog to edit or delete an existing building polygon.
 */
public class EditBuildingDialog {

    public static void show(
        BuildingPolygon model,
        BiConsumer<String, String> onSave,
        Runnable onDelete
    ) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Edit Building");

        Label titleLabel = new Label("Edit Building Information");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #202124;");

        // Name input
        Label nameLabel = new Label("Building Name:");
        nameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5f6368;");

        TextField nameField = new TextField(model.getName() != null ? model.getName() : "");
        nameField.setPromptText("Enter building name");
        nameField.setStyle("-fx-font-size: 13px;");

        // Color input
        Label colorLabel = new Label("Polygon Color:");
        colorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5f6368;");

        Color initialColor;
        try {
            initialColor = Color.web(model.getColor() != null ? model.getColor() : "#3498DB");
        } catch (Exception e) {
            initialColor = Color.web("#3498DB");
        }

        ColorPicker colorPicker = new ColorPicker(initialColor);
        colorPicker.setMaxWidth(Double.MAX_VALUE);

        // Buttons
        Button saveBtn = new Button("Save Changes");
        saveBtn.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        saveBtn.setOnAction(e -> {
            String newName = nameField.getText().trim();
            if (newName.isEmpty()) {
                nameField.setStyle("-fx-border-color: red;");
                return;
            }
            Color picked = colorPicker.getValue();
            String hex = String.format("#%02X%02X%02X",
                (int)(picked.getRed() * 255),
                (int)(picked.getGreen() * 255),
                (int)(picked.getBlue() * 255)
            );
            dialog.close();
            if (onSave != null) {
                onSave.accept(newName, hex);
            }
        });

        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle("-fx-background-color: #ea4335; -fx-text-fill: white; -fx-cursor: hand;");
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
        cancelBtn.setStyle("-fx-background-color: #f1f3f4; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());

        HBox buttonBar = new HBox(8, deleteBtn, cancelBtn, saveBtn);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        VBox layout = new VBox(14, titleLabel, nameLabel, nameField, colorLabel, colorPicker, buttonBar);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: white;");

        dialog.setScene(new Scene(layout, 360, 260));
        dialog.show();
    }
}
