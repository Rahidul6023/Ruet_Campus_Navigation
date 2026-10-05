package com.ruet.campusmap.model;

/**
 * Search result pointing to a room on a specific floor of a specific building.
 * Used to jump from a search result (e.g. a teacher) straight to their room.
 */
public record RoomLocation(BuildingInnerMap building, FloorPlan floor, RoomLabel room) {
}
