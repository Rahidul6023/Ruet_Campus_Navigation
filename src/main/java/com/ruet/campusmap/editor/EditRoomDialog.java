package com.ruet.campusmap.editor;

import com.ruet.campusmap.model.RoomLabel;
import com.ruet.campusmap.model.Teacher;
import com.ruet.campusmap.service.TeacherDataLoader;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Modal dialog to add or edit a room label on a building floor plan (admin only).
 */
public class EditRoomDialog {

    public static final List<String> ROOM_TYPES = List.of(
        "Classroom", "Lab", "Faculty Office", "Head Office", "Administrative",
        "Seminar Room", "Library", "Store", "Washroom", "Prayer Room", "Stairs", "Other"
    );

    private static final String FIELD_STYLE =
        "-fx-font-size: 13px; -fx-background-radius: 8px; -fx-border-color: #dadce0; -fx-border-radius: 8px; -fx-padding: 6 10;";
    private static final String FIELD_ERROR_STYLE =
        "-fx-font-size: 13px; -fx-background-radius: 8px; -fx-border-color: #d93025; -fx-border-radius: 8px; -fx-padding: 6 10;";
    private static final String LABEL_STYLE = "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #3c4043;";

    public interface SaveCallback {
        void onSave(RoomLabel room);
    }

    /**
     * @param roomToEdit existing room, or null to create a new one
     * @param x          SVG x-coordinate used for new rooms
     * @param y          SVG y-coordinate used for new rooms
     * @param floorTitle shown in the dialog subtitle (e.g. "CSE • Ground Floor")
     */
    public static void show(RoomLabel roomToEdit, double x, double y, String floorTitle,
                            SaveCallback onSave, Runnable onDelete) {
        boolean isNew = roomToEdit == null;
        RoomLabel model = isNew ? new RoomLabel(null, null, "Classroom", x, y) : roomToEdit;

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(isNew ? "Add Room Label" : "Edit Room Label");

        Label titleLabel = new Label(isNew ? "Label a Room" : "Edit Room");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #202124; -fx-font-family: 'Segoe UI', Roboto, sans-serif;");
        Label subLabel = new Label(floorTitle != null ? floorTitle : "Floor plan room");
        subLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #70757a;");

        // Room number
        Label numLabel = new Label("Room Number *");
        numLabel.setStyle(LABEL_STYLE);
        TextField numField = new TextField(model.getRoomNumber() != null ? model.getRoomNumber() : "");
        numField.setPromptText("e.g. 101, 401, HOD");
        numField.setStyle(FIELD_STYLE);

        // Room name
        Label nameLabel = new Label("Room Name");
        nameLabel.setStyle(LABEL_STYLE);
        TextField nameField = new TextField(model.getName() != null ? model.getName() : "");
        nameField.setPromptText("e.g. Software Lab, Seminar Room");
        nameField.setStyle(FIELD_STYLE);

        // Type
        Label typeLabel = new Label("Room Type");
        typeLabel.setStyle(LABEL_STYLE);
        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.setEditable(true);
        typeBox.setMaxWidth(Double.MAX_VALUE);
        typeBox.getItems().addAll(ROOM_TYPES);
        typeBox.setValue(model.getType() != null ? model.getType() : "Classroom");

        // Occupants (teachers)
        Label occLabel = new Label("Occupants / Teachers (one per line)");
        occLabel.setStyle(LABEL_STYLE);
        TextArea occArea = new TextArea(String.join("\n", model.getOccupants()));
        occArea.setPromptText("e.g. Dr. Md. Al Mamun");
        occArea.setPrefRowCount(3);
        occArea.setWrapText(true);

        ComboBox<String> teacherPicker = new ComboBox<>();
        teacherPicker.setPromptText("Add teacher from list…");
        teacherPicker.setMaxWidth(Double.MAX_VALUE);
        for (Teacher t : TeacherDataLoader.loadTeachers()) {
            if (t.getName() != null && !t.getName().isBlank()) teacherPicker.getItems().add(t.getName());
        }
        teacherPicker.setOnAction(e -> {
            String picked = teacherPicker.getValue();
            if (picked == null || picked.isBlank()) return;
            List<String> current = parseLines(occArea.getText());
            if (!current.contains(picked)) {
                current.add(picked);
                occArea.setText(String.join("\n", current));
            }
            javafx.application.Platform.runLater(() -> teacherPicker.getSelectionModel().clearSelection());
        });

        // Description
        Label descLabel = new Label("Notes");
        descLabel.setStyle(LABEL_STYLE);
        TextField descField = new TextField(model.getDescription() != null ? model.getDescription() : "");
        descField.setPromptText("Optional details (capacity, hours, …)");
        descField.setStyle(FIELD_STYLE);

        CheckBox visibleCheck = new CheckBox("Show label to regular users");
        visibleCheck.setSelected(model.isVisibleToUsers());
        visibleCheck.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #202124;");

        // Buttons
        Button saveBtn = new Button(isNew ? "Add Room" : "Save Changes");
        saveBtn.setDefaultButton(true);
        saveBtn.setStyle(
            "-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-background-radius: 8px; -fx-padding: 8 18; -fx-cursor: hand; -fx-font-size: 12px;"
        );
        saveBtn.setOnAction(e -> {
            String num = numField.getText() != null ? numField.getText().trim() : "";
            String name = nameField.getText() != null ? nameField.getText().trim() : "";
            if (num.isEmpty() && name.isEmpty()) {
                numField.setStyle(FIELD_ERROR_STYLE);
                return;
            }
            model.setRoomNumber(num);
            model.setName(name);
            model.setType(typeBox.getValue() != null ? typeBox.getValue().trim() : "");
            model.setDescription(descField.getText() != null ? descField.getText().trim() : "");
            model.setOccupants(parseLines(occArea.getText()));
            model.setVisibleToUsers(visibleCheck.isSelected());

            dialog.close();
            if (onSave != null) onSave.onSave(model);
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setCancelButton(true);
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
                confirm.setHeaderText("Delete room '" + model.getDisplayTitle() + "'?");
                confirm.setContentText("This removes the label from the floor plan.");
                var res = confirm.showAndWait();
                if (res.isPresent() && res.get() == ButtonType.OK) {
                    dialog.close();
                    onDelete.run();
                }
            });
            buttonBar.getChildren().add(deleteBtn);
        }
        buttonBar.getChildren().addAll(spacer, cancelBtn, saveBtn);

        HBox numNameRow = new HBox(10, new VBox(4, numLabel, numField), new VBox(4, nameLabel, nameField));
        HBox.setHgrow(numNameRow.getChildren().get(1), Priority.ALWAYS);

        VBox form = new VBox(10,
            numNameRow,
            new VBox(4, typeLabel, typeBox),
            new VBox(4, occLabel, occArea, teacherPicker),
            new VBox(4, descLabel, descField),
            visibleCheck
        );

        VBox layout = new VBox(14, titleLabel, subLabel, form, buttonBar);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: white; -fx-font-family: 'Segoe UI', Roboto, sans-serif;");

        dialog.setScene(new Scene(layout, 440, 540));
        dialog.setResizable(false);
        dialog.show();
        numField.requestFocus();
    }

    private static List<String> parseLines(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        Arrays.stream(text.split("[\\r\\n,]+"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .distinct()
            .forEach(out::add);
        return out;
    }
}
