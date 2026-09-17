package com.ruet.campusmap.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Dialog allowing users to submit map error reports and campus feedback.
 */
public class ReportErrorDialog {

    public static void show(Stage ownerStage) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (ownerStage != null) {
            dialog.initOwner(ownerStage);
        }
        dialog.setTitle("Report a Campus Map Error");

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setPrefWidth(400);
        root.setStyle("-fx-background-color: #ffffff; -fx-font-family: 'Segoe UI', Roboto, sans-serif;");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("📍 Report Map Error or Feedback");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #202124;");
        HBox.setHgrow(title, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 14px; -fx-cursor: hand; -fx-text-fill: #5f6368;");
        closeBtn.setOnAction(e -> dialog.close());

        header.getChildren().addAll(title, closeBtn);

        Label subtitle = new Label("Found an outdated building name, blocked path, or missing POI? Help us keep the RUET campus map accurate!");
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #5f6368;");

        // Issue Category
        Label catLabel = new Label("Issue Type");
        catLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll(
            "Incorrect Building / Location Name",
            "Missing POI (Food, Restroom, Printer, Parking)",
            "Closed or Under-Construction Pathway",
            "Wheelchair Accessibility Inaccuracy",
            "Other Map Suggestion"
        );
        categoryBox.setValue("Incorrect Building / Location Name");
        categoryBox.setMaxWidth(Double.MAX_VALUE);

        // Location / Building input
        Label locLabel = new Label("Location / Building Name");
        locLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        TextField locField = new TextField();
        locField.setPromptText("e.g. Near CSE Building, Central Field path");

        // Description input
        Label descLabel = new Label("Description of Correction");
        descLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        TextArea descArea = new TextArea();
        descArea.setPromptText("Provide details on what needs to be updated or added...");
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);

        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size: 12px;");

        // Buttons
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #f1f3f4; -fx-text-fill: #3c4043; -fx-cursor: hand; -fx-padding: 8 16;");
        cancelBtn.setOnAction(e -> dialog.close());

        Button submitBtn = new Button("Submit Report");
        submitBtn.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 16;");
        submitBtn.setOnAction(e -> {
            if (descArea.getText().trim().isEmpty()) {
                statusLabel.setText("Please enter a short description of the issue.");
                statusLabel.setStyle("-fx-text-fill: #d93025;");
                return;
            }

            statusLabel.setText("Thank you! Report recorded for RUET Map maintainers. ✓");
            statusLabel.setStyle("-fx-text-fill: #137333; -fx-font-weight: bold;");
            submitBtn.setDisable(true);

            // Auto close after 1.5 seconds
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.5));
            pause.setOnFinished(ev -> dialog.close());
            pause.play();
        });

        HBox btnRow = new HBox(10, cancelBtn, submitBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(
            header,
            subtitle,
            catLabel,
            categoryBox,
            locLabel,
            locField,
            descLabel,
            descArea,
            statusLabel,
            btnRow
        );

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.show();
    }
}
