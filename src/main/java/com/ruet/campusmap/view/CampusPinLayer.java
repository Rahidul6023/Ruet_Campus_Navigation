package com.ruet.campusmap.view;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

/**
 * Renders an authentic Google Maps-style drop pin marker on campus buildings,
 * complete with drop-in bounce animation, glowing ground beacon, and floating label chip.
 */
public class CampusPinLayer {

    private final Pane container;
    private final StackPane pinWrapper;
    private final VBox pinBox;
    private final Label titleChip;
    private final Circle groundRipple;
    private final SVGPath pinIcon;

    private Timeline dropAnimation;
    private Timeline rippleAnimation;
    private Runnable clickCallback;
    private boolean isDark = false;
    private double currentX = 0;
    private double currentY = 0;

    public CampusPinLayer() {
        container = new Pane();
        container.setPickOnBounds(false);

        // Ground beacon ripple circle
        groundRipple = new Circle(14);
        groundRipple.setFill(Color.rgb(234, 67, 53, 0.22));
        groundRipple.setStroke(Color.rgb(234, 67, 53, 0.7));
        groundRipple.setStrokeWidth(1.5);
        groundRipple.setMouseTransparent(true);

        // Teardrop Pin SVG (24x24 standard path scaled up)
        pinIcon = new SVGPath();
        pinIcon.setContent("M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z");
        pinIcon.setFill(Color.web("#ea4335"));
        pinIcon.setScaleX(1.85);
        pinIcon.setScaleY(1.85);
        pinIcon.setEffect(new DropShadow(10, 0, 4, Color.rgb(0, 0, 0, 0.4)));

        // Floating Title Chip above pin
        titleChip = new Label();
        titleChip.setStyle(
            "-fx-font-size: 11px; -fx-font-weight: bold; -fx-font-family: 'Segoe UI', Roboto, sans-serif; " +
            "-fx-padding: 3 9; -fx-background-radius: 12px; -fx-background-color: white; " +
            "-fx-text-fill: #202124; -fx-border-color: #dadce0; -fx-border-radius: 12px; -fx-border-width: 1px;"
        );
        titleChip.setEffect(new DropShadow(8, 0, 3, Color.rgb(0, 0, 0, 0.25)));

        pinBox = new VBox(2, titleChip, pinIcon);
        pinBox.setAlignment(Pos.BOTTOM_CENTER);
        pinBox.setCursor(Cursor.HAND);
        pinBox.setOnMouseClicked(e -> {
            if (clickCallback != null) {
                clickCallback.run();
            }
            e.consume();
        });

        pinWrapper = new StackPane(groundRipple, pinBox);
        pinWrapper.setAlignment(Pos.BOTTOM_CENTER);
        pinWrapper.setPickOnBounds(false);
        pinWrapper.setVisible(false);
        pinWrapper.setManaged(false);

        container.getChildren().add(pinWrapper);
        setupRippleAnimation();
    }

    public Pane getContainer() {
        return container;
    }

    /**
     * Binds counter-scaling to the map's zoom level so the pin remains clear and legible.
     */
    public void bindScale(javafx.beans.value.ObservableValue<Number> mapScale) {
        mapScale.addListener((obs, oldVal, newVal) -> {
            double s = newVal.doubleValue();
            if (s > 0) {
                double counter = Math.max(0.65, Math.min(1.4, 1.0 / s));
                pinBox.setScaleX(counter);
                pinBox.setScaleY(counter);
                groundRipple.setScaleX(counter);
                groundRipple.setScaleY(counter);
            }
        });
    }

    public void dropPin(double x, double y, String label, Runnable onClick) {
        this.currentX = x;
        this.currentY = y;
        this.clickCallback = onClick;

        if (label != null && !label.isBlank()) {
            titleChip.setText(label.trim());
            titleChip.setVisible(true);
            titleChip.setManaged(true);
        } else {
            titleChip.setVisible(false);
            titleChip.setManaged(false);
        }

        pinWrapper.setVisible(true);

        // Position pin tip at (x, y)
        pinWrapper.applyCss();
        pinWrapper.autosize();
        double w = pinWrapper.getWidth() > 0 ? pinWrapper.getWidth() : pinWrapper.prefWidth(-1);
        double h = pinWrapper.getHeight() > 0 ? pinWrapper.getHeight() : pinWrapper.prefHeight(-1);

        pinWrapper.relocate(x - w / 2.0, y - h);

        // Drop-in bounce animation
        if (dropAnimation != null) dropAnimation.stop();
        pinBox.setTranslateY(-45);
        pinBox.setOpacity(0.0);

        dropAnimation = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(pinBox.translateYProperty(), -45),
                new KeyValue(pinBox.opacityProperty(), 0.0)),
            new KeyFrame(Duration.millis(260),
                new KeyValue(pinBox.translateYProperty(), 0, Interpolator.EASE_IN),
                new KeyValue(pinBox.opacityProperty(), 1.0, Interpolator.EASE_IN)),
            new KeyFrame(Duration.millis(360),
                new KeyValue(pinBox.translateYProperty(), -10, Interpolator.EASE_OUT)),
            new KeyFrame(Duration.millis(480),
                new KeyValue(pinBox.translateYProperty(), 0, Interpolator.EASE_BOTH))
        );
        dropAnimation.play();
        rippleAnimation.playFromStart();
    }

    public void clear() {
        if (dropAnimation != null) dropAnimation.stop();
        if (rippleAnimation != null) rippleAnimation.stop();
        pinWrapper.setVisible(false);
    }

    public boolean isShowing() {
        return pinWrapper.isVisible();
    }

    public void applyTheme(boolean isDark) {
        this.isDark = isDark;
        if (isDark) {
            titleChip.setStyle(
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-font-family: 'Segoe UI', Roboto, sans-serif; " +
                "-fx-padding: 3 9; -fx-background-radius: 12px; -fx-background-color: #202124; " +
                "-fx-text-fill: #e8eaed; -fx-border-color: #3c4043; -fx-border-radius: 12px; -fx-border-width: 1px;"
            );
        } else {
            titleChip.setStyle(
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-font-family: 'Segoe UI', Roboto, sans-serif; " +
                "-fx-padding: 3 9; -fx-background-radius: 12px; -fx-background-color: white; " +
                "-fx-text-fill: #202124; -fx-border-color: #dadce0; -fx-border-radius: 12px; -fx-border-width: 1px;"
            );
        }
    }

    private void setupRippleAnimation() {
        rippleAnimation = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(groundRipple.radiusProperty(), 6),
                new KeyValue(groundRipple.opacityProperty(), 0.9)),
            new KeyFrame(Duration.millis(1100),
                new KeyValue(groundRipple.radiusProperty(), 28, Interpolator.EASE_OUT),
                new KeyValue(groundRipple.opacityProperty(), 0.0, Interpolator.EASE_OUT))
        );
        rippleAnimation.setCycleCount(Timeline.INDEFINITE);
    }
}
