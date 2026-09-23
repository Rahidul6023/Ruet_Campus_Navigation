package com.ruet.campusmap.editor;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class AdminLoginDialog {

    public interface LoginCallback {
        void onSuccess();
    }

    public static void show(Stage ownerStage, LoginCallback callback) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(ownerStage);
        dialog.setTitle("Admin Authentication");

        // Icon (Shield / Key icon)
        SVGPath lockIcon = new SVGPath();
        lockIcon.setContent("M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z");
        lockIcon.setFill(Color.web("#1a73e8"));
        lockIcon.setScaleX(1.1);
        lockIcon.setScaleY(1.1);

        Label titleLabel = new Label("Admin Authentication");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-font-family: 'Segoe UI', Roboto, sans-serif; -fx-text-fill: #202124;");

        Label subtitleLabel = new Label("Enter password to unlock map editor & drawing tools");
        subtitleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #70757a;");

        VBox header = new VBox(6, lockIcon, titleLabel, subtitleLabel);
        header.setAlignment(Pos.CENTER);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter admin password");
        passwordField.setStyle(
            "-fx-background-color: #f1f3f4; -fx-background-radius: 8px; " +
            "-fx-border-color: #dadce0; -fx-border-radius: 8px; -fx-padding: 8 12; -fx-font-size: 13px;"
        );

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #d93025; -fx-font-size: 12px; -fx-font-weight: bold;");
        errorLabel.setVisible(false);

        Button loginBtn = new Button("Unlock Editor");
        loginBtn.setStyle(
            "-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-background-radius: 8px; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 13px;"
        );

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #5f6368; " +
            "-fx-background-radius: 8px; -fx-padding: 8 14; -fx-cursor: hand; -fx-font-size: 13px;"
        );
        cancelBtn.setOnAction(e -> dialog.close());

        HBox btnRow = new HBox(10, cancelBtn, loginBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        Runnable verify = () -> {
            if ("admin123".equals(passwordField.getText())) {
                dialog.close();
                callback.onSuccess();
            } else {
                errorLabel.setText("Incorrect password. Please try again.");
                errorLabel.setVisible(true);
                passwordField.setStyle(
                    "-fx-background-color: #fce8e6; -fx-background-radius: 8px; " +
                    "-fx-border-color: #d93025; -fx-border-radius: 8px; -fx-padding: 8 12; -fx-font-size: 13px;"
                );
            }
        };

        loginBtn.setOnAction(e -> verify.run());
        passwordField.setOnAction(e -> verify.run());

        VBox layout = new VBox(16, header, passwordField, errorLabel, btnRow);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(24));
        layout.setStyle("-fx-background-color: white; -fx-background-radius: 12px;");

        dialog.setScene(new Scene(layout, 360, 240));
        dialog.setResizable(false);
        dialog.show();
    }
}

