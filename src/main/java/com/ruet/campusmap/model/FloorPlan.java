package com.ruet.campusmap.model;

/**
 * Data model representing a single floor plan within a building.
 */
public class FloorPlan {
    private String floorId;
    private int floorNumber;
    private String floorName;
    private String shortLabel;
    private String mapFile;
    private String description;

    public FloorPlan() {}

    public FloorPlan(String floorId, int floorNumber, String floorName, String shortLabel, String mapFile, String description) {
        this.floorId = floorId;
        this.floorNumber = floorNumber;
        this.floorName = floorName;
        this.shortLabel = shortLabel;
        this.mapFile = mapFile;
        this.description = description;
    }

    public String getFloorId() {
        return floorId;
    }

    public void setFloorId(String floorId) {
        this.floorId = floorId;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public void setFloorNumber(int floorNumber) {
        this.floorNumber = floorNumber;
    }

    public String getFloorName() {
        return floorName;
    }

    public void setFloorName(String floorName) {
        this.floorName = floorName;
    }

    public String getShortLabel() {
        return shortLabel != null && !shortLabel.isBlank() ? shortLabel : String.valueOf(floorNumber);
    }

    public void setShortLabel(String shortLabel) {
        this.shortLabel = shortLabel;
    }

    public String getMapFile() {
        return mapFile;
    }

    public void setMapFile(String mapFile) {
        this.mapFile = mapFile;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return floorName != null ? floorName : ("Floor " + floorNumber);
    }
}
