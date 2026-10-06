package com.ruet.campusmap.view;

import com.ruet.campusmap.model.BuildingPolygon;
import com.ruet.campusmap.model.RoomLocation;
import com.ruet.campusmap.model.Teacher;
import com.ruet.campusmap.service.BuildingImageStorage;
import com.ruet.campusmap.service.RoomRegistry;
import com.ruet.campusmap.service.TeacherDataLoader;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Modern floating Google Maps-style place sheet that displays building or POI details
 * when clicked on the map, with building photo preview, smooth slide-up entrance animation,
 * and vertically stacked action buttons.
 */
public class BuildingInfoCard {

    private final VBox cardContainer;
    private final Label categoryBadge;
    private final Label titleLabel;
    private final Label subtitleLabel;
    private final VBox dynamicSection;
    private final Button copyBtn;
    private final Button innerMapBtn;
    private final StackPane closeBtn;
    private final SVGPath closeIcon;
    private final SVGPath copyIcon;
    private final SVGPath innerMapIcon;
    private final Separator divider;
    private final Label feedbackLabel;
    private final VBox actionsBox;
    private java.util.function.Consumer<BuildingPolygon> onOpenInnerMap;
    private Consumer<RoomLocation> onOpenRoom;
    private Runnable onCloseCallback;
    private BuildingPolygon currentBuilding;
    private boolean isDark = false;
    private ParallelTransition currentAnim;

    public BuildingInfoCard() {
        cardContainer = new VBox(12);
        cardContainer.setPrefWidth(370);
        cardContainer.setMaxWidth(370);
        cardContainer.setPadding(new Insets(18, 20, 18, 20));
        cardContainer.setEffect(new DropShadow(18, 0, 6, Color.rgb(32, 33, 36, 0.22)));
        cardContainer.setVisible(false);
        cardContainer.setManaged(false);

        // Prevent dragging or clicking on card from panning map underneath
        cardContainer.setOnMousePressed(javafx.event.Event::consume);
        cardContainer.setOnMouseDragged(javafx.event.Event::consume);

        // Top Row: Category Badge Pill + Close Button
        categoryBadge = new Label("CAMPUS BUILDING");
        categoryBadge.setStyle(
            "-fx-font-size: 11px; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 4 10; " +
            "-fx-background-radius: 12px;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Circular close button with SVG cross
        closeIcon = new SVGPath();
        closeIcon.setContent("M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z");
        closeIcon.setScaleX(0.8);
        closeIcon.setScaleY(0.8);

        closeBtn = new StackPane(closeIcon);
        closeBtn.setPrefSize(28, 28);
        closeBtn.setMinSize(28, 28);
        closeBtn.setMaxSize(28, 28);
        closeBtn.setStyle("-fx-cursor: hand; -fx-background-radius: 14px;");
        Tooltip.install(closeBtn, new Tooltip("Close"));
        closeBtn.setOnMousePressed(e -> hide());

        HBox topRow = new HBox(8, categoryBadge, spacer, closeBtn);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Title
        titleLabel = new Label("Building Name");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-font-family: 'Segoe UI', Roboto, sans-serif;");
        titleLabel.setWrapText(true);

        // Subtitle / description
        subtitleLabel = new Label("RUET Campus • Rajshahi-6204");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-font-family: 'Segoe UI', Roboto, sans-serif;");

        // Dynamic Section for building photo or contextual details
        dynamicSection = new VBox(8);
        dynamicSection.setFillWidth(true);

        // Subtle divider
        divider = new Separator();
        divider.setStyle("-fx-opacity: 0.4;");

        // Vertical Action Buttons: Floor Plan (if available) + Copy Name
        innerMapIcon = new SVGPath();
        innerMapIcon.setContent("M11.99 18.54l-7.37-5.73L3 14.07l9 7 9-7-1.63-1.27-7.38 5.74zM12 16l7.36-5.73L21 9l-9-7-9 7 1.63 1.27L12 16z");
        innerMapIcon.setScaleX(0.75);
        innerMapIcon.setScaleY(0.75);

        innerMapBtn = new Button("Floor Plan", innerMapIcon);
        innerMapBtn.setMaxWidth(Double.MAX_VALUE);
        innerMapBtn.setAlignment(Pos.CENTER);
        innerMapBtn.setVisible(false);
        innerMapBtn.setManaged(false);
        innerMapBtn.setOnAction(e -> {
            if (onOpenInnerMap != null && currentBuilding != null) {
                onOpenInnerMap.accept(currentBuilding);
            }
        });

        copyIcon = new SVGPath();
        copyIcon.setContent("M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z");
        copyIcon.setScaleX(0.75);
        copyIcon.setScaleY(0.75);

        copyBtn = new Button("Copy Name", copyIcon);
        copyBtn.setMaxWidth(Double.MAX_VALUE);
        copyBtn.setAlignment(Pos.CENTER);
        copyBtn.setStyle(
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 500; " +
            "-fx-background-radius: 10px; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 8 14;"
        );

        feedbackLabel = new Label("✓ Copied!");
        feedbackLabel.setAlignment(Pos.CENTER);
        feedbackLabel.setMaxWidth(Double.MAX_VALUE);
        feedbackLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #34a853; -fx-font-weight: bold;");
        feedbackLabel.setVisible(false);
        feedbackLabel.setManaged(false);

        copyBtn.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(titleLabel.getText());
            clipboard.setContent(content);

            feedbackLabel.setVisible(true);
            feedbackLabel.setManaged(true);
            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(ev -> {
                feedbackLabel.setVisible(false);
                feedbackLabel.setManaged(false);
            });
            pause.play();
        });

        actionsBox = new VBox(8, innerMapBtn, copyBtn, feedbackLabel);
        actionsBox.setAlignment(Pos.CENTER);
        actionsBox.setFillWidth(true);

        cardContainer.getChildren().addAll(topRow, titleLabel, subtitleLabel, dynamicSection, divider, actionsBox);

        StackPane.setAlignment(cardContainer, Pos.BOTTOM_LEFT);
        StackPane.setMargin(cardContainer, new Insets(0, 0, 80, 24));

        applyTheme(false);
    }

    public void showBuilding(BuildingPolygon building) {
        if (building == null) return;
        this.currentBuilding = building;
        String name = building.getName();
        titleLabel.setText(name);

        String category;
        String lower = name.toLowerCase();
        if (lower.contains("hall")) {
            category = "🏠 RESIDENTIAL HALL";
        } else if (lower.contains("library")) {
            category = "📚 CENTRAL LIBRARY";
        } else if (lower.contains("cafeteria") || lower.contains("canteen")) {
            category = "🍽️ DINING & FOOD";
        } else if (lower.contains("admin")) {
            category = "🏢 ADMINISTRATION";
        } else if (lower.contains("auditorium")) {
            category = "🎭 AUDITORIUM";
        } else if (lower.contains("medical") || lower.contains("hospital")) {
            category = "🏥 HEALTH CENTER";
        } else if (lower.contains("mosque")) {
            category = "🕌 CAMPUS MOSQUE";
        } else {
            category = "🏛️ ACADEMIC BUILDING";
        }

        categoryBadge.setText(category);
        subtitleLabel.setText("RUET Campus • Rajshahi-6204");

        boolean hasInner = com.ruet.campusmap.service.InnerMapRegistry.hasInnerMap(name);
        innerMapBtn.setVisible(hasInner);
        innerMapBtn.setManaged(hasInner);
        copyBtn.setVisible(true);
        copyBtn.setManaged(true);

        // Building picture box or identical placeholder
        dynamicSection.getChildren().clear();
        dynamicSection.getChildren().add(createBuildingImageBox(building));

        animateShow();
    }

    private Node createBuildingImageBox(BuildingPolygon building) {
        String imagePath = building != null ? building.getImagePath() : null;
        if ((imagePath == null || imagePath.isBlank()) && building != null) {
            imagePath = BuildingImageStorage.findExistingImageForBuilding(building.getName());
            if (imagePath != null) {
                building.setImagePath(imagePath);
            }
        }
        Image img = null;
        if (imagePath != null && !imagePath.isBlank()) {
            img = BuildingImageStorage.loadBuildingImage(imagePath);
        }

        if (img != null && !img.isError()) {
            StackPane imgContainer = new StackPane();
            imgContainer.setPrefSize(330, 180);
            imgContainer.setMinSize(330, 180);
            imgContainer.setMaxSize(330, 180);
            imgContainer.setAlignment(Pos.CENTER);
            imgContainer.setStyle(
                "-fx-background-color: " + (isDark ? "#1e1e24;" : "#f1f3f4;") +
                "-fx-background-radius: 12px; " +
                "-fx-border-color: " + (isDark ? "#3c4043;" : "#dadce0;") +
                "-fx-border-radius: 12px; -fx-border-width: 1px;"
            );

            ImageView iv = new ImageView(img);
            iv.setFitWidth(330);
            iv.setFitHeight(180);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);

            Rectangle clip = new Rectangle(330, 180);
            clip.setArcWidth(24);
            clip.setArcHeight(24);
            imgContainer.setClip(clip);

            imgContainer.getChildren().add(iv);
            return imgContainer;
        } else {
            VBox placeholderBox = new VBox(8);
            placeholderBox.setAlignment(Pos.CENTER);
            placeholderBox.setPrefSize(330, 180);
            placeholderBox.setMinSize(330, 180);
            placeholderBox.setMaxSize(330, 180);
            placeholderBox.setStyle(
                "-fx-background-color: " + (isDark ? "#282a2d;" : "#f8f9fa;") +
                "-fx-background-radius: 12px; " +
                "-fx-border-color: " + (isDark ? "#444746;" : "#dadce0;") +
                "-fx-border-radius: 12px; " +
                "-fx-border-width: 1.5px; " +
                "-fx-border-style: dashed;"
            );

            Label iconLabel = new Label("🏛️");
            iconLabel.setStyle("-fx-font-size: 32px; -fx-opacity: 0.6;");

            Label textLabel = new Label("No picture uploaded");
            textLabel.setStyle(
                "-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: " +
                (isDark ? "#9aa0a6;" : "#70757a;")
            );

            placeholderBox.getChildren().addAll(iconLabel, textLabel);
            return placeholderBox;
        }
    }

    public void showTeacher(Teacher teacher, Runnable onNavigateToBuilding) {
        if (teacher == null) return;
        this.currentBuilding = null;
        innerMapBtn.setVisible(false);
        innerMapBtn.setManaged(false);

        titleLabel.setText(teacher.getName());
        categoryBadge.setText("👨‍🏫 FACULTY / TEACHER");

        String desig = (teacher.getDesignation() != null && !teacher.getDesignation().isBlank()) ? teacher.getDesignation() : "Faculty Member";
        String dept = (teacher.getDepartment() != null && !teacher.getDepartment().isBlank()) ? teacher.getDepartment() : "RUET";
        subtitleLabel.setText(desig + " • Department of " + dept);

        dynamicSection.getChildren().clear();

        // Modern office details card
        VBox officeCard = new VBox(6);
        officeCard.setPadding(new Insets(10, 12, 10, 12));
        officeCard.setStyle(
            "-fx-background-color: " + (isDark ? "#2d2f31;" : "#f8f9fa;") +
            "-fx-background-radius: 10px; -fx-border-color: " + (isDark ? "#3c4043;" : "#dadce0;") +
            "-fx-border-radius: 10px; -fx-border-width: 1px;"
        );

        // Building Info Row
        HBox bldRow = new HBox(8);
        bldRow.setAlignment(Pos.CENTER_LEFT);
        Label bldIcon = new Label("🏛️");
        bldIcon.setStyle("-fx-font-size: 13px;");
        VBox bldTextCol = new VBox(1);
        Label bldTitle = new Label("Building Location");
        bldTitle.setStyle("-fx-font-size: 10px; -fx-text-fill: " + (isDark ? "#9aa0a6;" : "#70757a;") + " -fx-font-weight: bold;");
        Label bldValue = new Label(teacher.getBuildingName() != null ? teacher.getBuildingName() : "Not specified");
        bldValue.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + (isDark ? "#e8eaed;" : "#202124;"));
        bldTextCol.getChildren().addAll(bldTitle, bldValue);
        bldRow.getChildren().addAll(bldIcon, bldTextCol);

        // Room Info Row
        HBox roomRow = new HBox(8);
        roomRow.setAlignment(Pos.CENTER_LEFT);
        Label roomIcon = new Label("🚪");
        roomIcon.setStyle("-fx-font-size: 13px;");
        VBox roomTextCol = new VBox(1);
        Label roomTitle = new Label("Office / Room Number");
        roomTitle.setStyle("-fx-font-size: 10px; -fx-text-fill: " + (isDark ? "#9aa0a6;" : "#70757a;") + " -fx-font-weight: bold;");

        Optional<RoomLocation> roomLoc = RoomRegistry.findRoom(teacher.getBuildingName(), teacher.getRoomNumber());
        if (roomLoc.isEmpty()) {
            roomLoc = RoomRegistry.findRoom(teacher.getBuildingName(), teacher.getName());
        }

        String roomDisplay = teacher.getRoomNumber() != null ? teacher.getRoomNumber() : "Main Office";
        if (roomLoc.isPresent()) {
            roomDisplay += " (" + roomLoc.get().floor().getFloorName() + ")";
        }
        Label roomValue = new Label(roomDisplay);
        roomValue.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + (isDark ? "#8ab4f8;" : "#1a73e8;"));
        roomTextCol.getChildren().addAll(roomTitle, roomValue);
        roomRow.getChildren().addAll(roomIcon, roomTextCol);

        officeCard.getChildren().addAll(bldRow, roomRow);

        // Action button to fly to office building on map
        Button locateBtn = new Button("View Office Building on Map");
        locateBtn.setMaxWidth(Double.MAX_VALUE);
        locateBtn.setStyle(
            "-fx-background-color: " + (isDark ? "#174ea6;" : "#e8f0fe;") +
            "-fx-text-fill: " + (isDark ? "#d2e3fc;" : "#1a73e8;") +
            "-fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 8px; " +
            "-fx-cursor: hand; -fx-padding: 6 12;"
        );
        locateBtn.setOnAction(e -> {
            if (onNavigateToBuilding != null) {
                onNavigateToBuilding.run();
            }
        });

        dynamicSection.getChildren().addAll(officeCard, locateBtn);

        if (roomLoc.isPresent()) {
            RoomLocation targetLoc = roomLoc.get();
            Button viewRoomBtn = new Button("🚪 View Room on Floor Plan (" + targetLoc.room().getDisplayTitle() + ")");
            viewRoomBtn.setMaxWidth(Double.MAX_VALUE);
            viewRoomBtn.setStyle(
                "-fx-background-color: #1a73e8; -fx-text-fill: white; " +
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 8px; " +
                "-fx-cursor: hand; -fx-padding: 7 12;"
            );
            viewRoomBtn.setOnAction(e -> {
                if (onOpenRoom != null) {
                    onOpenRoom.accept(targetLoc);
                }
            });
            dynamicSection.getChildren().add(viewRoomBtn);
        }

        animateShow();
    }

    public void showRoom(RoomLocation roomLoc, Runnable onEnterBuilding) {
        if (roomLoc == null || roomLoc.room() == null) return;
        this.currentBuilding = null;
        innerMapBtn.setVisible(false);
        innerMapBtn.setManaged(false);

        com.ruet.campusmap.model.RoomLabel r = roomLoc.room();
        String roomNum = r.getRoomNumber() != null ? r.getRoomNumber().trim() : "";
        String roomName = (r.getName() != null && !r.getName().isBlank()) ? r.getName().trim() : ("Room " + roomNum);

        String title = !roomNum.isBlank() && !roomName.equalsIgnoreCase(roomNum)
            ? ("Room " + roomNum + " • " + roomName)
            : roomName;
        titleLabel.setText(title);

        String typeStr = r.getType() != null && !r.getType().isBlank() ? r.getType() : "Room";
        String catIcon = switch (typeStr.toLowerCase()) {
            case "lab" -> "🔬 ";
            case "classroom" -> "📚 ";
            case "faculty office", "head office" -> "👨‍🏫 ";
            case "prayer room" -> "🕌 ";
            case "library" -> "📖 ";
            case "washroom" -> "🚻 ";
            default -> "🚪 ";
        };
        categoryBadge.setText((catIcon + typeStr).toUpperCase());

        String bName = roomLoc.building() != null ? roomLoc.building().getBuildingName() : "Campus Building";
        String fName = roomLoc.floor() != null ? roomLoc.floor().getFloorName() : "Floor Plan";
        subtitleLabel.setText(bName + " • " + fName);

        dynamicSection.getChildren().clear();

        VBox detailsBox = new VBox(6);
        detailsBox.setPadding(new Insets(10, 12, 10, 12));
        detailsBox.setStyle(
            "-fx-background-color: " + (isDark ? "#2d2f31;" : "#f8f9fa;") +
            "-fx-background-radius: 10px; -fx-border-color: " + (isDark ? "#3c4043;" : "#dadce0;") +
            "-fx-border-radius: 10px; -fx-border-width: 1px;"
        );

        detailsBox.getChildren().addAll(
            createMetaRow("🏛️", "Building", bName),
            createMetaRow("🏢", "Floor Level", fName),
            createMetaRow("🚪", "Room Type", typeStr)
        );

        if (!r.getOccupants().isEmpty()) {
            detailsBox.getChildren().add(createMetaRow("👤", "Occupants / Faculty", String.join(", ", r.getOccupants())));
        }

        if (r.getDescription() != null && !r.getDescription().isBlank()) {
            detailsBox.getChildren().add(createMetaRow("ℹ️", "Information", r.getDescription()));
        }

        Button enterBtn = new Button("🚪 Enter Building (" + fName + ")");
        enterBtn.setMaxWidth(Double.MAX_VALUE);
        enterBtn.setStyle(
            "-fx-background-color: #1a73e8; -fx-text-fill: white; " +
            "-fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 8px; " +
            "-fx-cursor: hand; -fx-padding: 8 14;"
        );
        enterBtn.setOnAction(e -> {
            if (onEnterBuilding != null) {
                onEnterBuilding.run();
            }
        });

        dynamicSection.getChildren().addAll(detailsBox, enterBtn);

        animateShow();
    }

    private HBox createMetaRow(String icon, String title, String value) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label ic = new Label(icon);
        ic.setStyle("-fx-font-size: 13px;");
        VBox col = new VBox(1);
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 10px; -fx-text-fill: " + (isDark ? "#9aa0a6;" : "#70757a;") + " -fx-font-weight: bold;");
        Label v = new Label(value);
        v.setWrapText(true);
        v.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + (isDark ? "#e8eaed;" : "#202124;"));
        col.getChildren().addAll(t, v);
        row.getChildren().addAll(ic, col);
        return row;
    }

    public void showPoi(String name, String category, String details) {
        this.currentBuilding = null;
        innerMapBtn.setVisible(false);
        innerMapBtn.setManaged(false);
        dynamicSection.getChildren().clear();
        titleLabel.setText(name);
        categoryBadge.setText(category != null ? category.toUpperCase() : "POINT OF INTEREST");
        subtitleLabel.setText(details != null && !details.isBlank() ? details : "RUET Campus Facilities");
        animateShow();
    }

    private void animateShow() {
        if (currentAnim != null) {
            currentAnim.stop();
        }
        cardContainer.setVisible(true);
        cardContainer.setManaged(true);
        cardContainer.toFront();

        cardContainer.setOpacity(0.0);
        cardContainer.setTranslateY(20);

        FadeTransition fade = new FadeTransition(Duration.millis(250), cardContainer);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition translate = new TranslateTransition(Duration.millis(250), cardContainer);
        translate.setFromY(20);
        translate.setToY(0);
        translate.setInterpolator(Interpolator.EASE_OUT);

        currentAnim = new ParallelTransition(fade, translate);
        currentAnim.play();
    }

    public void hide() {
        if (!cardContainer.isVisible()) return;
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
        if (currentAnim != null) {
            currentAnim.stop();
        }
        FadeTransition fade = new FadeTransition(Duration.millis(180), cardContainer);
        fade.setFromValue(cardContainer.getOpacity());
        fade.setToValue(0.0);
        fade.setInterpolator(Interpolator.EASE_IN);

        TranslateTransition translate = new TranslateTransition(Duration.millis(180), cardContainer);
        translate.setFromY(cardContainer.getTranslateY());
        translate.setToY(15);
        translate.setInterpolator(Interpolator.EASE_IN);

        currentAnim = new ParallelTransition(fade, translate);
        currentAnim.setOnFinished(e -> {
            cardContainer.setVisible(false);
            cardContainer.setManaged(false);
            cardContainer.setTranslateY(0);
        });
        currentAnim.play();
    }

    public void applyTheme(boolean isDark) {
        this.isDark = isDark;
        if (isDark) {
            cardContainer.setStyle(
                "-fx-background-color: #202124; " +
                "-fx-background-radius: 16px; " +
                "-fx-border-radius: 16px; " +
                "-fx-border-color: #3c4043; " +
                "-fx-border-width: 1px;"
            );
            categoryBadge.setStyle(
                "-fx-background-color: #173154; " +
                "-fx-text-fill: #8ab4f8; " +
                "-fx-font-size: 11px; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 4 10; " +
                "-fx-background-radius: 12px;"
            );
            titleLabel.setTextFill(Color.web("#e8eaed"));
            subtitleLabel.setTextFill(Color.web("#9aa0a6"));
            closeIcon.setFill(Color.web("#9aa0a6"));
            closeBtn.setStyle("-fx-cursor: hand; -fx-background-color: #303134; -fx-background-radius: 14px;");
            copyBtn.setStyle(
                "-fx-background-color: #303134; " +
                "-fx-text-fill: #e8eaed; " +
                "-fx-border-color: #5f6368; " +
                "-fx-border-radius: 10px; " +
                "-fx-border-width: 1px; " +
                "-fx-font-size: 12px; " +
                "-fx-font-weight: 500; " +
                "-fx-background-radius: 10px; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 8 14;"
            );
            copyIcon.setFill(Color.web("#bdc1c6"));
            innerMapBtn.setStyle(
                "-fx-background-color: #14b8a6; " +
                "-fx-text-fill: #0f172a; " +
                "-fx-font-size: 13px; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 10px; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 8 14;"
            );
            innerMapIcon.setFill(Color.web("#0f172a"));
        } else {
            cardContainer.setStyle(
                "-fx-background-color: #ffffff; " +
                "-fx-background-radius: 16px; " +
                "-fx-border-radius: 16px; " +
                "-fx-border-color: #dadce0; " +
                "-fx-border-width: 1px;"
            );
            categoryBadge.setStyle(
                "-fx-background-color: #e8f0fe; " +
                "-fx-text-fill: #1a73e8; " +
                "-fx-font-size: 11px; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 4 10; " +
                "-fx-background-radius: 12px;"
            );
            titleLabel.setTextFill(Color.web("#202124"));
            subtitleLabel.setTextFill(Color.web("#5f6368"));
            closeIcon.setFill(Color.web("#5f6368"));
            closeBtn.setStyle("-fx-cursor: hand; -fx-background-color: #f1f3f4; -fx-background-radius: 14px;");
            copyBtn.setStyle(
                "-fx-background-color: #f1f3f4; " +
                "-fx-text-fill: #3c4043; " +
                "-fx-border-color: #dadce0; " +
                "-fx-border-radius: 10px; " +
                "-fx-border-width: 1px; " +
                "-fx-font-size: 12px; " +
                "-fx-font-weight: 500; " +
                "-fx-background-radius: 10px; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 8 14;"
            );
            copyIcon.setFill(Color.web("#5f6368"));
            innerMapBtn.setStyle(
                "-fx-background-color: #0d9488; " +
                "-fx-text-fill: #ffffff; " +
                "-fx-font-size: 13px; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 10px; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 8 14;"
            );
            innerMapIcon.setFill(Color.WHITE);
        }

        if (currentBuilding != null && cardContainer.isVisible()) {
            dynamicSection.getChildren().clear();
            dynamicSection.getChildren().add(createBuildingImageBox(currentBuilding));
        }
    }

    public void setOnOpenInnerMap(java.util.function.Consumer<BuildingPolygon> callback) {
        this.onOpenInnerMap = callback;
    }

    public void setOnOpenRoom(Consumer<RoomLocation> callback) {
        this.onOpenRoom = callback;
    }

    public void setOnClose(Runnable callback) {
        this.onCloseCallback = callback;
    }

    public VBox getContainer() {
        return cardContainer;
    }
}
