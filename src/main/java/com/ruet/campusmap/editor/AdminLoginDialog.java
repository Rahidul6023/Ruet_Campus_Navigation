package com.ruet.campusmap.editor;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
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

        Label titleLabel = new Label("Admin / Editor Access");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter admin password");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");

        Button loginBtn = new Button("Login & Open Editor");
        loginBtn.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-weight: bold;");

        Runnable verify = () -> {
            if ("admin123".equals(passwordField.getText())) {
                dialog.close();
                callback.onSuccess();
            } else {
                errorLabel.setText("Incorrect password!");
            }
        };

        loginBtn.setOnAction(e -> verify.run());
        passwordField.setOnAction(e -> verify.run());

        VBox layout = new VBox(12, titleLabel, passwordField, errorLabel, loginBtn);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(24));
        layout.setStyle("-fx-background-color: white;");

        dialog.setScene(new Scene(layout, 320, 200));
        dialog.show();
    }
}
