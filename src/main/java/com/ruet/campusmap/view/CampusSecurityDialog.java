package com.ruet.campusmap.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Modern dialog displaying RUET campus security and emergency hotline directory.
 */
public class CampusSecurityDialog {

    public static void show(Stage ownerStage) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (ownerStage != null) {
            dialog.initOwner(ownerStage);
        }
        dialog.setTitle("RUET Campus Emergency & Security");

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));
        root.setPrefWidth(420);
        root.setStyle("-fx-background-color: #ffffff; -fx-font-family: 'Segoe UI', Roboto, sans-serif;");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("🚨 Campus Security & Emergency");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #d93025;");
        HBox.setHgrow(title, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 14px; -fx-cursor: hand; -fx-text-fill: #5f6368;");
        closeBtn.setOnAction(e -> dialog.close());

        header.getChildren().addAll(title, closeBtn);

        Label subtitle = new Label("Important direct contact numbers for campus help, safety, and health emergencies.");
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #5f6368;");

        VBox contactList = new VBox(10);
        contactList.setPadding(new Insets(6, 0, 6, 0));

        contactList.getChildren().addAll(
            createContactRow("Proctor Office", "+880 721 750742", "Campus discipline & incident reporting"),
            createContactRow("RUET Medical Center", "+880 721 750744", "On-campus clinic & first aid ambulance"),
            createContactRow("Main Gate Security Post", "+880 721 750743", "24/7 Gate security & lost-and-found"),
            createContactRow("Rajshahi Fire Station", "01730-002444", "Fire emergency brigade service"),
            createContactRow("National Emergency", "999", "Police, Ambulance & Fire Service")
        );

        Button doneBtn = new Button("Close");
        doneBtn.setPrefWidth(100);
        doneBtn.setStyle(
            "-fx-background-color: #1a73e8; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 6px; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 8 16;"
        );
        doneBtn.setOnAction(e -> dialog.close());

        HBox footer = new HBox(doneBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(header, subtitle, new Separator(), contactList, new Separator(), footer);

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.show();
    }

    private static HBox createContactRow(String label, String number, String description) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 8, 12));
        row.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8px; -fx-border-color: #dadce0; -fx-border-radius: 8px;");

        VBox details = new VBox(2);
        Label titleLabel = new Label(label);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #202124;");

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #70757a;");

        Label numLabel = new Label(number);
        numLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1a73e8;");

        details.getChildren().addAll(titleLabel, descLabel, numLabel);
        HBox.setHgrow(details, Priority.ALWAYS);

        Button copyBtn = new Button("Copy");
        copyBtn.setStyle("-fx-background-color: #ffffff; -fx-border-color: #dadce0; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-cursor: hand; -fx-font-size: 11px;");
        copyBtn.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(number);
            Clipboard.getSystemClipboard().setContent(content);
            copyBtn.setText("Copied! ✓");
            copyBtn.setStyle("-fx-background-color: #e6f4ea; -fx-border-color: #137333; -fx-text-fill: #137333; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-font-size: 11px;");
        });

        row.getChildren().addAll(details, copyBtn);
        return row;
    }
}
