package com.ruet.campusmap.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Data model representing a building and its collection of inner floor plans.
 */
public class BuildingInnerMap {
    private String buildingId;
    private String buildingName;
    private List<String> aliases = new ArrayList<>();
    private String defaultFloorId;
    private List<FloorPlan> floors = new ArrayList<>();

    public BuildingInnerMap() {}

    public BuildingInnerMap(String buildingId, String buildingName, List<String> aliases, String defaultFloorId, List<FloorPlan> floors) {
        this.buildingId = buildingId;
        this.buildingName = buildingName;
        this.aliases = aliases != null ? aliases : new ArrayList<>();
        this.defaultFloorId = defaultFloorId;
        this.floors = floors != null ? floors : new ArrayList<>();
    }

    public String getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(String buildingId) {
        this.buildingId = buildingId;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases != null ? aliases : new ArrayList<>();
    }

    public String getDefaultFloorId() {
        return defaultFloorId;
    }

    public void setDefaultFloorId(String defaultFloorId) {
        this.defaultFloorId = defaultFloorId;
    }

    public List<FloorPlan> getFloors() {
        return floors;
    }

    public void setFloors(List<FloorPlan> floors) {
        this.floors = floors != null ? floors : new ArrayList<>();
    }

    public FloorPlan getDefaultFloor() {
        if (floors == null || floors.isEmpty()) return null;
        if (defaultFloorId != null && !defaultFloorId.isBlank()) {
            for (FloorPlan f : floors) {
                if (defaultFloorId.equalsIgnoreCase(f.getFloorId())) {
                    return f;
                }
            }
        }
        return floors.get(0);
    }

    public FloorPlan getFloorById(String floorId) {
        if (floors == null || floorId == null) return null;
        for (FloorPlan f : floors) {
            if (floorId.equalsIgnoreCase(f.getFloorId())) {
                return f;
            }
        }
        return null;
    }

    /**
     * Checks if this building matches a search query or polygon name (case-insensitive, checking buildingName,
     * buildingId, and all aliases).
     */
    public boolean matches(String query) {
        if (query == null || query.isBlank()) return false;
        String cleanQuery = query.trim().toLowerCase();

        if (buildingName != null && buildingName.toLowerCase().contains(cleanQuery)) {
            return true;
        }
        if (buildingId != null && buildingId.toLowerCase().contains(cleanQuery)) {
            return true;
        }
        if (aliases != null) {
            for (String alias : aliases) {
                if (alias != null) {
                    String cleanAlias = alias.toLowerCase().trim();
                    if (cleanAlias.equalsIgnoreCase(cleanQuery) || cleanAlias.contains(cleanQuery) || cleanQuery.contains(cleanAlias)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
