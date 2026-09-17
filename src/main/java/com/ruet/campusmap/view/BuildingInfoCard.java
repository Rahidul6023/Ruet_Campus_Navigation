package com.ruet.campusmap.view;

import com.ruet.campusmap.model.BuildingPolygon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Modern floating bottom card that displays building or POI details when clicked on the map.
 */
public class BuildingInfoCard {

    private final VBox cardContainer;
    private final Label titleLabel;
    private final Label subtitleLabel;
    private final Button actionButton;
    private final Button closeBtn;
    private boolean isDark = false;

    public BuildingInfoCard() {
        cardContainer = new VBox(10);
        cardContainer.setPrefWidth(360);
        cardContainer.setMaxWidth(360);
        cardContainer.setPadding(new Insets(16));
        cardContainer.setEffect(new DropShadow(15, 0, 4, Color.rgb(60, 64, 67, 0.3)));
        cardContainer.setVisible(false);
        cardContainer.setManaged(false);

        // Prevent dragging on the card from panning the map underneath
        cardContainer.setOnMousePressed(javafx.event.Event::consume);
        cardContainer.setOnMouseDragged(javafx.event.Event::consume);

        // Header (Title + Close button)
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        titleLabel = new Label("Building Name");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 14px; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> hide());

        header.getChildren().addAll(titleLabel, closeBtn);

        // Subtitle / category description
        subtitleLabel = new Label("Academic Building • RUET Campus");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setStyle("-fx-font-size: 12px;");

        // Action Buttons Row
        HBox buttonRow = new HBox(10);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        actionButton = new Button("Explore Location");
        actionButton.setStyle(
            "-fx-background-color: #1a73e8; " +
            "-fx-text-fill: #ffffff; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 6px; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 7 14;"
        );

        buttonRow.getChildren().add(actionButton);

        cardContainer.getChildren().addAll(header, subtitleLabel, buttonRow);

        StackPane.setAlignment(cardContainer, Pos.BOTTOM_LEFT);
        StackPane.setMargin(cardContainer, new Insets(0, 0, 80, 24));

        applyTheme(false);
    }

    public void showBuilding(BuildingPolygon building) {
        if (building == null) return;
        titleLabel.setText(building.getName());
        subtitleLabel.setText("Academic Building • RUET Campus");
        actionButton.setText("Explore Interior Map");
        cardContainer.setVisible(true);
        cardContainer.setManaged(true);
        cardContainer.toFront();
    }

    public void showPoi(String name, String category, String details) {
        titleLabel.setText(name);
        subtitleLabel.setText(category + (details != null && !details.isBlank() ? " • " + details : ""));
        actionButton.setText("Get Directions");
        cardContainer.setVisible(true);
        cardContainer.setManaged(true);
        cardContainer.toFront();
    }

    public void applyTheme(boolean isDark) {
        this.isDark = isDark;
        if (isDark) {
            cardContainer.setStyle(
                "-fx-background-color: #202124; " +
                "-fx-background-radius: 12px; " +
                "-fx-border-radius: 12px; " +
                "-fx-border-color: #3c4043; " +
                "-fx-border-width: 1px;"
            );
            titleLabel.setTextFill(Color.web("#e8eaed"));
            subtitleLabel.setTextFill(Color.web("#9aa0a6"));
            closeBtn.setTextFill(Color.web("#9aa0a6"));
        } else {
            cardContainer.setStyle(
                "-fx-background-color: #ffffff; " +
                "-fx-background-radius: 12px; " +
                "-fx-border-radius: 12px; " +
                "-fx-border-color: #dadce0; " +
                "-fx-border-width: 1px;"
            );
            titleLabel.setTextFill(Color.web("#202124"));
            subtitleLabel.setTextFill(Color.web("#70757a"));
            closeBtn.setTextFill(Color.web("#5f6368"));
        }
    }

    public void hide() {
        cardContainer.setVisible(false);
        cardContainer.setManaged(false);
    }

    public VBox getContainer() {
        return cardContainer;
    }
}
