package com.ruet.campusmap.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A labelled room placed on a building floor plan.
 * Coordinates (x, y) are in floor-plan SVG space (e.g. 0..2300 x 0..1700),
 * so labels stay anchored to the right room at every zoom level.
 */
public class RoomLabel {

    private String id;
    private String roomNumber;
    private String name;
    private String type;
    private String description;
    private double x;
    private double y;
    private boolean visibleToUsers = true;
    private List<String> occupants = new ArrayList<>();

    public RoomLabel() {
        this.id = UUID.randomUUID().toString();
    }

    public RoomLabel(String roomNumber, String name, String type, double x, double y) {
        this();
        this.roomNumber = roomNumber;
        this.name = name;
        this.type = type;
        this.x = x;
        this.y = y;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public boolean isVisibleToUsers() { return visibleToUsers; }
    public void setVisibleToUsers(boolean visibleToUsers) { this.visibleToUsers = visibleToUsers; }

    /** Names of people (e.g. teachers) who sit in this room. Used for search. */
    public List<String> getOccupants() {
        if (occupants == null) occupants = new ArrayList<>();
        return occupants;
    }
    public void setOccupants(List<String> occupants) {
        this.occupants = occupants != null ? occupants : new ArrayList<>();
    }

    /** Short text shown on the map badge: room number, falling back to name. */
    public String getDisplayTitle() {
        if (roomNumber != null && !roomNumber.isBlank()) return roomNumber.trim();
        return name != null ? name.trim() : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoomLabel other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getDisplayTitle() + (name != null && !name.isBlank() && roomNumber != null ? " - " + name : "");
    }
}
