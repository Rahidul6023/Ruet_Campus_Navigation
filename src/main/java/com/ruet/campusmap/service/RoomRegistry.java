package com.ruet.campusmap.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.ruet.campusmap.model.BuildingInnerMap;
import com.ruet.campusmap.model.FloorPlan;
import com.ruet.campusmap.model.RoomLabel;
import com.ruet.campusmap.model.RoomLocation;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Loads and saves room labels for building floors.
 *
 * Each floor has its OWN file:
 *   src/main/resources/data/rooms/&lt;buildingId&gt;/&lt;floorId&gt;.json
 * e.g. data/rooms/cse_building/ground.json
 *
 * File names come from the ids in inner_maps.json, so SVG file naming doesn't matter.
 */
public class RoomRegistry {

    private static final String DISK_ROOT = "src/main/resources/data/rooms";
    private static final String TARGET_ROOT = "target/classes/data/rooms";
    private static final String RESOURCE_ROOT = "/data/rooms";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<RoomLabel>>() {}.getType();

    private RoomRegistry() {}

    /** Relative path (inside data/rooms) of the file holding a floor's rooms. */
    public static String getRoomFilePath(String buildingId, String floorId) {
        return sanitize(buildingId) + "/" + sanitize(floorId) + ".json";
    }

    /** Loads rooms for one floor. Disk copy wins so edits show immediately; classpath is the fallback. */
    public static synchronized List<RoomLabel> loadRooms(String buildingId, String floorId) {
        if (buildingId == null || floorId == null) return new ArrayList<>();
        String rel = getRoomFilePath(buildingId, floorId);

        File diskFile = new File(DISK_ROOT, rel);
        if (diskFile.exists() && diskFile.length() > 0) {
            try (FileReader reader = new FileReader(diskFile, StandardCharsets.UTF_8)) {
                List<RoomLabel> rooms = GSON.fromJson(reader, LIST_TYPE);
                if (rooms != null) return new ArrayList<>(rooms);
            } catch (Exception e) {
                System.err.println("Could not read rooms from " + diskFile + ": " + e.getMessage());
            }
        }

        try (InputStream is = RoomRegistry.class.getResourceAsStream(RESOURCE_ROOT + "/" + rel)) {
            if (is != null) {
                try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    List<RoomLabel> rooms = GSON.fromJson(reader, LIST_TYPE);
                    if (rooms != null) return new ArrayList<>(rooms);
                }
            }
        } catch (Exception e) {
            System.err.println("Could not load rooms resource " + rel + ": " + e.getMessage());
        }
        return new ArrayList<>();
    }

    /** Saves all rooms of one floor to its own file (source tree + running build output). */
    public static synchronized boolean saveRooms(String buildingId, String floorId, List<RoomLabel> rooms) {
        if (buildingId == null || floorId == null) return false;
        List<RoomLabel> data = rooms != null ? rooms : new ArrayList<>();
        String rel = getRoomFilePath(buildingId, floorId);

        boolean saved = writeJson(new File(DISK_ROOT, rel), data);

        // Mirror into target/classes so a running app reading the classpath sees it too
        if (new File("target/classes").exists()) {
            writeJson(new File(TARGET_ROOT, rel), data);
        }
        return saved;
    }

    private static boolean writeJson(File file, List<RoomLabel> data) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to save rooms to " + file + ": " + e.getMessage());
            return false;
        }
    }

    /** All rooms across every floor of a building. */
    public static List<RoomLocation> getAllRooms(BuildingInnerMap building) {
        List<RoomLocation> result = new ArrayList<>();
        if (building == null || building.getFloors() == null) return result;
        for (FloorPlan floor : building.getFloors()) {
            for (RoomLabel room : loadRooms(building.getBuildingId(), floor.getFloorId())) {
                result.add(new RoomLocation(building, floor, room));
            }
        }
        return result;
    }

    /**
     * Finds a room inside a building by room number (e.g. "Room 401", "401"), room name,
     * or occupant name. Handy for jumping from a teacher search result to their office.
     */
    public static Optional<RoomLocation> findRoom(String buildingName, String query) {
        BuildingInnerMap building = InnerMapRegistry.findInnerMap(buildingName);
        if (building == null || query == null || query.isBlank()) return Optional.empty();

        String q = normalizeRoom(query);
        String raw = query.trim().toLowerCase(Locale.ROOT);
        RoomLocation partial = null;

        for (RoomLocation loc : getAllRooms(building)) {
            RoomLabel r = loc.room();
            if (r.getRoomNumber() != null && normalizeRoom(r.getRoomNumber()).equals(q)) {
                return Optional.of(loc);
            }
            if (partial == null) {
                boolean nameHit = r.getName() != null && r.getName().toLowerCase(Locale.ROOT).contains(raw);
                boolean occupantHit = r.getOccupants().stream()
                    .anyMatch(o -> o != null && o.toLowerCase(Locale.ROOT).contains(raw));
                if (nameHit || occupantHit) partial = loc;
            }
        }
        return Optional.ofNullable(partial);
    }

    /** Searches every building's rooms for a text match (number, name, type, occupants). */
    public static List<RoomLocation> searchRooms(String query) {
        List<RoomLocation> matches = new ArrayList<>();
        if (query == null || query.isBlank()) return matches;
        String q = query.trim().toLowerCase(Locale.ROOT);
        String qRoom = normalizeRoom(query);

        for (BuildingInnerMap building : InnerMapRegistry.getInnerMaps()) {
            for (RoomLocation loc : getAllRooms(building)) {
                RoomLabel r = loc.room();
                if ((r.getRoomNumber() != null && !qRoom.isEmpty() && normalizeRoom(r.getRoomNumber()).contains(qRoom))
                    || (r.getName() != null && r.getName().toLowerCase(Locale.ROOT).contains(q))
                    || (r.getType() != null && r.getType().toLowerCase(Locale.ROOT).contains(q))
                    || r.getOccupants().stream().anyMatch(o -> o != null && o.toLowerCase(Locale.ROOT).contains(q))) {
                    matches.add(loc);
                }
            }
        }
        return matches;
    }

    /** "Room 401" / "room-401" / " 401 " all become "401". */
    public static String normalizeRoom(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT)
            .replace("room", "")
            .replace("rm.", "")
            .replaceAll("[\\s#._-]", "")
            .trim();
    }

    private static String sanitize(String s) {
        return s.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
    }
}
