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
import javafx.scene.shape.SVGPath;

/**
 * Modern floating bottom card that displays building details when a polygon is clicked.
 */
public class BuildingInfoCard {

    private final VBox cardContainer;
    private final Label titleLabel;
    private final Label subtitleLabel;
    private final Button interiorButton;

    public BuildingInfoCard() {
        cardContainer = new VBox(10);
        cardContainer.setPrefWidth(360);
        cardContainer.setMaxWidth(360);
        cardContainer.setPadding(new Insets(16));
        cardContainer.setStyle(
            "-fx-background-color: #ffffff; " +
            "-fx-background-radius: 12px; " +
            "-fx-border-radius: 12px; " +
            "-fx-border-color: #dadce0; " +
            "-fx-border-width: 1px;"
        );
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
        titleLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #202124;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-font-size: 14px; " +
            "-fx-text-fill: #5f6368; " +
            "-fx-cursor: hand;"
        );
        closeBtn.setOnAction(e -> hide());

        header.getChildren().addAll(titleLabel, closeBtn);

        // Subtitle / category description
        subtitleLabel = new Label("Academic Building • RUET Campus");
        subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #70757a;");

        // Action Buttons Row
        HBox buttonRow = new HBox(10);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        interiorButton = new Button("Explore Interior Map");
        interiorButton.setStyle(
            "-fx-background-color: #1a73e8; " +
            "-fx-text-fill: #ffffff; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 6px; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 8 14 8 14;"
        );

        buttonRow.getChildren().add(interiorButton);

        cardContainer.getChildren().addAll(header, subtitleLabel, buttonRow);

        StackPane.setAlignment(cardContainer, Pos.BOTTOM_LEFT);
        StackPane.setMargin(cardContainer, new Insets(0, 0, 80, 24));
    }

    public void showBuilding(BuildingPolygon building) {
        titleLabel.setText(building.getName());
        cardContainer.setVisible(true);
        cardContainer.setManaged(true);
    }

    public void hide() {
        cardContainer.setVisible(false);
        cardContainer.setManaged(false);
    }

    public VBox getContainer() {
        return cardContainer;
    }
}
