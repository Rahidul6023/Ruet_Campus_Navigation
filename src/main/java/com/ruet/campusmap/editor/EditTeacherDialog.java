package com.ruet.campusmap.editor;

import com.ruet.campusmap.model.BuildingPolygon;
import com.ruet.campusmap.model.Teacher;
import com.ruet.campusmap.service.PolygonDataLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.*;

/**
 * Modal dialog to add or edit a RUET Teacher / Faculty record.
 */
public class EditTeacherDialog {

    public interface SaveCallback {
        void onSave(Teacher savedTeacher);
    }

    public static void show(
        Teacher teacherToEdit,
        SaveCallback onSave,
        Runnable onDelete
    ) {
        boolean isNew = (teacherToEdit == null);
        Teacher model = isNew ? new Teacher() : teacherToEdit;

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(isNew ? "Add New Teacher" : "Edit Teacher Information");

        Label titleLabel = new Label(isNew ? "Add Faculty Member" : "Edit Faculty Member");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #202124; -fx-font-family: 'Segoe UI', Roboto, sans-serif;");

        Label subLabel = new Label("Fill in teacher details and map office location");
        subLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #70757a; -fx-font-family: 'Segoe UI', Roboto, sans-serif;");

        // 1. Teacher Name
        Label nameLabel = new Label("Teacher Name *");
        nameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #3c4043;");
        TextField nameField = new TextField(model.getName() != null ? model.getName() : "");
        nameField.setPromptText("e.g. Dr. Md. Rahman");
        nameField.setStyle("-fx-font-size: 13px; -fx-background-radius: 8px; -fx-border-color: #dadce0; -fx-border-radius: 8px; -fx-padding: 6 10;");

        // 2. Designation
        Label desigLabel = new Label("Designation");
        desigLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #3c4043;");
        ComboBox<String> desigBox = new ComboBox<>();
        desigBox.setEditable(true);
        desigBox.setMaxWidth(Double.MAX_VALUE);
        desigBox.getItems().addAll(
            "Professor & Head",
            "Professor",
            "Associate Professor",
            "Assistant Professor",
            "Senior Lecturer",
            "Lecturer",
            "Adjunct Faculty"
        );
        desigBox.setValue(model.getDesignation() != null ? model.getDesignation() : "Assistant Professor");
        desigBox.setStyle("-fx-font-size: 13px; -fx-background-radius: 8px;");

        // 3. Department
        Label deptLabel = new Label("Department");
        deptLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #3c4043;");
        ComboBox<String> deptBox = new ComboBox<>();
        deptBox.setEditable(true);
        deptBox.setMaxWidth(Double.MAX_VALUE);
        deptBox.getItems().addAll(
            "Computer Science & Engineering",
            "Electrical & Electronic Engineering",
            "Civil Engineering",
            "Mechanical Engineering",
            "Mechatronics Engineering",
            "Electronics & Telecommunication Engineering",
            "Industrial & Production Engineering",
            "Chemical Engineering",
            "Architecture",
            "Mathematics",
            "Physics",
            "Chemistry",
            "Humanities"
        );
        deptBox.setValue(model.getDepartment() != null ? model.getDepartment() : "Computer Science & Engineering");
        deptBox.setStyle("-fx-font-size: 13px; -fx-background-radius: 8px;");

        // 4. Campus Building (Linked to mapped campus buildings)
        Label bldLabel = new Label("Campus Building *");
        bldLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #3c4043;");
        ComboBox<String> bldBox = new ComboBox<>();
        bldBox.setEditable(true);
        bldBox.setMaxWidth(Double.MAX_VALUE);

        // Gather buildings from campus.json + core landmarks
        Set<String> buildingNames = new LinkedHashSet<>();
        List<BuildingPolygon> campusPolygons = PolygonDataLoader.loadBuildingPolygons();
        for (BuildingPolygon bp : campusPolygons) {
            if (bp.getName() != null && !bp.getName().isBlank()) {
                buildingNames.add(bp.getName().trim());
            }
        }
        buildingNames.addAll(Arrays.asList(
            "Academic Building 02 (CSE)",
            "Academic Building 1",
            "Academic Building 2 (EEE)",
            "Academic Building 3 (CME)",
            "Academic Building 4 (Civil)",
            "Auditorium",
            "Central Library",
            "Admin Building"
        ));

        bldBox.getItems().addAll(buildingNames);
        if (model.getBuildingName() != null && !model.getBuildingName().isBlank()) {
            bldBox.setValue(model.getBuildingName());
        } else if (!bldBox.getItems().isEmpty()) {
            bldBox.setValue(bldBox.getItems().get(0));
        }
        bldBox.setStyle("-fx-font-size: 13px; -fx-background-radius: 8px;");

        // 5. Room / Office Number
        Label roomLabel = new Label("Room / Office Number");
        roomLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #3c4043;");
        TextField roomField = new TextField(model.getRoomNumber() != null ? model.getRoomNumber() : "");
        roomField.setPromptText("e.g. Room 402");
        roomField.setStyle("-fx-font-size: 13px; -fx-background-radius: 8px; -fx-border-color: #dadce0; -fx-border-radius: 8px; -fx-padding: 6 10;");

        // Action Buttons
        Button saveBtn = new Button(isNew ? "Add Teacher" : "Save Changes");
        saveBtn.setStyle(
            "-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-background-radius: 8px; -fx-padding: 8 18; -fx-cursor: hand; -fx-font-size: 12px;"
        );
        saveBtn.setOnAction(e -> {
            String name = nameField.getText() != null ? nameField.getText().trim() : "";
            if (name.isEmpty()) {
                nameField.setStyle("-fx-font-size: 13px; -fx-border-color: #d93025; -fx-background-radius: 8px; -fx-border-radius: 8px; -fx-padding: 6 10;");
                return;
            }

            String building = bldBox.getValue() != null ? bldBox.getValue().trim() : "";
            if (building.isEmpty()) {
                bldBox.setStyle("-fx-font-size: 13px; -fx-border-color: #d93025; -fx-background-radius: 8px; -fx-border-radius: 8px;");
                return;
            }

            model.setName(name);
            model.setDesignation(desigBox.getValue() != null ? desigBox.getValue().trim() : "");
            model.setDepartment(deptBox.getValue() != null ? deptBox.getValue().trim() : "");
            model.setBuildingName(building);
            model.setRoomNumber(roomField.getText() != null ? roomField.getText().trim() : "");

            dialog.close();
            if (onSave != null) {
                onSave.onSave(model);
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #5f6368; " +
            "-fx-background-radius: 8px; -fx-padding: 8 14; -fx-cursor: hand; -fx-font-size: 12px;"
        );
        cancelBtn.setOnAction(e -> dialog.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox buttonBar = new HBox(8);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        if (!isNew && onDelete != null) {
            Button deleteBtn = new Button("Delete");
            deleteBtn.setStyle(
                "-fx-background-color: #fce8e6; -fx-text-fill: #c5221f; -fx-font-weight: bold; " +
                "-fx-background-radius: 8px; -fx-padding: 8 14; -fx-cursor: hand; -fx-font-size: 12px;"
            );
            deleteBtn.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirm Delete");
                confirm.setHeaderText("Delete '" + model.getName() + "'?");
                confirm.setContentText("Are you sure you want to delete this teacher? This cannot be undone.");
                var res = confirm.showAndWait();
                if (res.isPresent() && res.get() == ButtonType.OK) {
                    dialog.close();
                    onDelete.run();
                }
            });
            buttonBar.getChildren().add(deleteBtn);
        }

        buttonBar.getChildren().addAll(spacer, cancelBtn, saveBtn);

        VBox form = new VBox(10,
            new VBox(4, nameLabel, nameField),
            new VBox(4, desigLabel, desigBox),
            new VBox(4, deptLabel, deptBox),
            new VBox(4, bldLabel, bldBox),
            new VBox(4, roomLabel, roomField)
        );

        VBox layout = new VBox(14, titleLabel, subLabel, form, buttonBar);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: white;");

        dialog.setScene(new Scene(layout, 420, 480));
        dialog.setResizable(false);
        dialog.show();
    }
}
