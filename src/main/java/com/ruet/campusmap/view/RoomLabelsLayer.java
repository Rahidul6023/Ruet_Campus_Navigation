package com.ruet.campusmap.view;

import com.ruet.campusmap.model.RoomLabel;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Overlay drawn on top of an inner floor plan, in the same coordinate space as the SVG.
 * Shows room badges for users and lets admins drag / click labels while in edit mode.
 * Badges are counter-scaled so they keep a readable on-screen size at every zoom level.
 */
public class RoomLabelsLayer {

    private final Pane pane;
    private final double mapWidth;
    private final double mapHeight;
    private final Map<RoomLabel, HBox> badges = new LinkedHashMap<>();
    private final List<RoomLabel> rooms = new ArrayList<>();

    private boolean editMode = false;
    private boolean isDark = false;
    private double mapScale = 1.0;
    private String highlightedId = null;

    private Consumer<RoomLabel> onRoomEdit;
    private Consumer<RoomLabel> onRoomMoved;
    private RoomHoverTooltip hoverTooltip;

    public RoomLabelsLayer(double mapWidth, double mapHeight) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;

        pane = new Pane();
        pane.setPrefSize(mapWidth, mapHeight);
        pane.setMinSize(mapWidth, mapHeight);
        pane.setMaxSize(mapWidth, mapHeight);
        // Let clicks on empty space fall through to the viewport (used for placing new rooms)
        pane.setPickOnBounds(false);
        // Clip so badges near the edge never enlarge the map bounds (keeps fit/centering exact)
        pane.setClip(new Rectangle(0, 0, mapWidth, mapHeight));
    }

    public Pane getPane() {
        return pane;
    }

    public void setCallbacks(Consumer<RoomLabel> onRoomEdit, Consumer<RoomLabel> onRoomMoved) {
        this.onRoomEdit = onRoomEdit;
        this.onRoomMoved = onRoomMoved;
    }

    public void setHoverTooltip(RoomHoverTooltip hoverTooltip) {
        this.hoverTooltip = hoverTooltip;
    }

    public void setRooms(List<RoomLabel> newRooms) {
        if (hoverTooltip != null) hoverTooltip.hide();
        rooms.clear();
        if (newRooms != null) rooms.addAll(newRooms);
        rebuild();
    }

    public List<RoomLabel> getRooms() {
        return rooms;
    }

    public void setEditMode(boolean editMode) {
        if (hoverTooltip != null) hoverTooltip.hide();
        this.editMode = editMode;
        rebuild();
    }

    public void applyTheme(boolean isDark) {
        this.isDark = isDark;
        rebuild();
    }

    /** Called whenever the floor-plan zoom changes, so badges stay a constant screen size. */
    public void setMapScale(double scale) {
        this.mapScale = scale > 0 ? scale : 1.0;
        for (HBox badge : badges.values()) {
            applyBadgeScale(badge);
        }
    }

    /** Converts scene coordinates to floor-plan coordinates; null if outside the plan. */
    public Point2D sceneToMap(double sceneX, double sceneY) {
        Point2D p = pane.sceneToLocal(sceneX, sceneY);
        if (p == null || p.getX() < 0 || p.getY() < 0 || p.getX() > mapWidth || p.getY() > mapHeight) {
            return null;
        }
        return p;
    }

    /** Briefly pulses a room badge (used when jumping to a room from search). */
    public void highlight(RoomLabel room) {
        highlightedId = room != null ? room.getId() : null;
        rebuild();
        HBox badge = room != null ? badges.get(room) : null;
        if (badge == null) return;

        double base = badgeScale();
        Timeline pulse = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(badge.scaleXProperty(), base),
                new KeyValue(badge.scaleYProperty(), base)),
            new KeyFrame(Duration.millis(280),
                new KeyValue(badge.scaleXProperty(), base * 1.45, Interpolator.EASE_OUT),
                new KeyValue(badge.scaleYProperty(), base * 1.45, Interpolator.EASE_OUT)),
            new KeyFrame(Duration.millis(560),
                new KeyValue(badge.scaleXProperty(), base, Interpolator.EASE_IN),
                new KeyValue(badge.scaleYProperty(), base, Interpolator.EASE_IN))
        );
        pulse.setCycleCount(3);
        pulse.play();
    }

    private void rebuild() {
        pane.getChildren().clear();
        badges.clear();
        for (RoomLabel room : rooms) {
            if (!editMode && !room.isVisibleToUsers()) continue;
            HBox badge = createBadge(room);
            badges.put(room, badge);
            pane.getChildren().add(badge);
        }
    }

    private HBox createBadge(RoomLabel room) {
        boolean hidden = !room.isVisibleToUsers();
        boolean highlighted = room.getId() != null && room.getId().equals(highlightedId);

        Circle dot = new Circle(4, Color.web(colorForType(room.getType())));
        dot.setStroke(Color.WHITE);
        dot.setStrokeWidth(1.2);

        Label text = new Label(room.getDisplayTitle());
        text.setStyle(
            "-fx-font-size: 12px; -fx-font-weight: bold; -fx-font-family: 'Segoe UI', Roboto, sans-serif; " +
            "-fx-text-fill: " + (isDark ? "#e8eaed;" : "#202124;")
        );

        HBox badge = new HBox(5, dot, text);
        badge.setAlignment(Pos.CENTER);
        badge.setMouseTransparent(false);

        String bg = highlighted ? (isDark ? "#174ea6" : "#d2e3fc") : (isDark ? "rgba(32,33,36,0.92)" : "rgba(255,255,255,0.94)");
        String border = highlighted ? "#1a73e8" : (editMode ? "#1a73e8" : (isDark ? "#5f6368" : "#dadce0"));
        badge.setStyle(
            "-fx-background-color: " + bg + "; -fx-background-radius: 12px; " +
            "-fx-border-color: " + border + "; -fx-border-radius: 12px; -fx-border-width: " + (highlighted ? 2 : 1) + "px; " +
            (hidden ? "-fx-border-style: segments(4, 3); " : "") +
            "-fx-padding: 3 9 3 7;"
        );
        badge.setOpacity(hidden ? 0.6 : 1.0);
        badge.setEffect(new DropShadow(6, 0, 2, Color.rgb(0, 0, 0, 0.18)));

        // Keep the badge centered on the room point: reposition whenever its size is known/changes
        badge.widthProperty().addListener((o, ov, nv) -> positionBadge(badge, room));
        badge.heightProperty().addListener((o, ov, nv) -> positionBadge(badge, room));
        positionBadge(badge, room);
        applyBadgeScale(badge);

        // Smooth mouse-transparent hover tooltip
        badge.setOnMouseEntered(e -> {
            if (!editMode) {
                badge.setEffect(new DropShadow(10, 0, 2, Color.web("#1a73e8", 0.55)));
            }
            if (hoverTooltip != null) {
                hoverTooltip.show(room, e.getSceneX(), e.getSceneY());
            }
        });
        badge.setOnMouseMoved(e -> {
            if (hoverTooltip != null) {
                hoverTooltip.updatePosition(e.getSceneX(), e.getSceneY());
            }
        });
        badge.setOnMouseExited(e -> {
            if (!editMode) {
                badge.setEffect(new DropShadow(6, 0, 2, Color.rgb(0, 0, 0, 0.18)));
            }
            if (hoverTooltip != null) {
                hoverTooltip.hide();
            }
        });

        if (editMode) {
            installEditHandlers(badge, room);
        } else {
            badge.setCursor(Cursor.HAND);
            // Swallow press so clicking a label never starts a map pan
            badge.setOnMousePressed(javafx.event.Event::consume);
        }
        return badge;
    }

    private void installEditHandlers(HBox badge, RoomLabel room) {
        badge.setCursor(Cursor.MOVE);
        final double[] start = new double[2];
        final boolean[] moved = new boolean[]{false};

        badge.setOnMousePressed(e -> {
            if (hoverTooltip != null) hoverTooltip.hide();
            start[0] = e.getSceneX();
            start[1] = e.getSceneY();
            moved[0] = false;
            e.consume();
        });

        badge.setOnMouseDragged(e -> {
            if (Math.hypot(e.getSceneX() - start[0], e.getSceneY() - start[1]) > 3) {
                moved[0] = true;
            }
            if (moved[0]) {
                Point2D p = pane.sceneToLocal(e.getSceneX(), e.getSceneY());
                room.setX(clamp(p.getX(), 0, mapWidth));
                room.setY(clamp(p.getY(), 0, mapHeight));
                positionBadge(badge, room);
            }
            e.consume();
        });

        badge.setOnMouseReleased(e -> {
            if (moved[0]) {
                if (onRoomMoved != null) onRoomMoved.accept(room);
            } else if (e.getButton() == MouseButton.PRIMARY && onRoomEdit != null) {
                onRoomEdit.accept(room);
            }
            e.consume();
        });

        badge.setOnMouseClicked(javafx.event.Event::consume);
    }

    private void positionBadge(HBox badge, RoomLabel room) {
        double w = badge.getWidth() > 0 ? badge.getWidth() : badge.prefWidth(-1);
        double h = badge.getHeight() > 0 ? badge.getHeight() : badge.prefHeight(-1);
        badge.relocate(room.getX() - w / 2.0, room.getY() - h / 2.0);
    }

    private double badgeScale() {
        // Inverse of map zoom -> constant on-screen size; capped so labels don't explode when zoomed out
        return Math.max(0.35, Math.min(2.4, 1.0 / mapScale));
    }

    private void applyBadgeScale(HBox badge) {
        double s = badgeScale();
        badge.setScaleX(s);
        badge.setScaleY(s);
    }

    private String buildTooltip(RoomLabel room) {
        StringBuilder sb = new StringBuilder();
        if (room.getRoomNumber() != null && !room.getRoomNumber().isBlank()) sb.append("Room ").append(room.getRoomNumber());
        if (room.getName() != null && !room.getName().isBlank()) {
            if (sb.length() > 0) sb.append(" — ");
            sb.append(room.getName());
        }
        if (room.getType() != null && !room.getType().isBlank()) sb.append("\n").append(room.getType());
        if (!room.getOccupants().isEmpty()) sb.append("\n👤 ").append(String.join(", ", room.getOccupants()));
        if (room.getDescription() != null && !room.getDescription().isBlank()) sb.append("\n").append(room.getDescription());
        if (editMode) sb.append("\n\n(Drag to move • Click to edit)");
        return sb.toString();
    }

    private static String colorForType(String type) {
        if (type == null) return "#5f6368";
        return switch (type.toLowerCase()) {
            case "classroom" -> "#1a73e8";
            case "lab" -> "#e37400";
            case "faculty office", "head office" -> "#1e8e3e";
            case "administrative" -> "#00897b";
            case "seminar room", "library" -> "#8e24aa";
            case "washroom", "prayer room" -> "#12b5cb";
            case "stairs", "store" -> "#80868b";
            default -> "#5f6368";
        };
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
