package com.ruet.campusmap.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ruet.campusmap.model.BuildingInnerMap;
import com.ruet.campusmap.model.FloorPlan;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to manage and query registered inner building floor maps.
 * Reads configurations from inner_maps.json, allowing flexible file naming
 * and multi-floor metadata without hardcoded conventions.
 */
public class InnerMapRegistry {

    private static final String JSON_RESOURCE_PATH = "/data/inner_maps.json";
    private static final String JSON_FILE_PATH = "src/main/resources/data/inner_maps.json";

    private static List<BuildingInnerMap> registryCache = null;

    /**
     * Loads all registered building inner maps from JSON (disk first, fallback to classpath).
     */
    public static synchronized List<BuildingInnerMap> getInnerMaps() {
        if (registryCache != null) {
            return registryCache;
        }

        Gson gson = new Gson();
        Type listType = new TypeToken<List<BuildingInnerMap>>() {}.getType();

        // 1. Try reading directly from src/main/resources on disk (immediate dev saves)
        File file = new File(JSON_FILE_PATH);
        if (file.exists() && file.length() > 0) {
            try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                List<BuildingInnerMap> loaded = gson.fromJson(reader, listType);
                if (loaded != null && !loaded.isEmpty()) {
                    registryCache = loaded;
                    return registryCache;
                }
            } catch (Exception ignored) {}
        }

        // 2. Fallback to classpath resource
        try (InputStream is = InnerMapRegistry.class.getResourceAsStream(JSON_RESOURCE_PATH)) {
            if (is != null) {
                try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    List<BuildingInnerMap> loaded = gson.fromJson(reader, listType);
                    if (loaded != null) {
                        registryCache = loaded;
                        return registryCache;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Could not load inner maps config: " + e.getMessage());
        }

        registryCache = new ArrayList<>();
        return registryCache;
    }

    /**
     * Checks if a building with the given name or alias has inner maps configured.
     */
    public static boolean hasInnerMap(String buildingName) {
        if (buildingName == null || buildingName.isBlank()) return false;
        for (BuildingInnerMap bim : getInnerMaps()) {
            if (bim.matches(buildingName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the BuildingInnerMap matching the given building name or alias.
     */
    public static BuildingInnerMap findInnerMap(String buildingName) {
        if (buildingName == null || buildingName.isBlank()) return null;
        for (BuildingInnerMap bim : getInnerMaps()) {
            if (bim.matches(buildingName)) {
                return bim;
            }
        }
        return null;
    }

    /**
     * Reads the SVG markup content for a given floor plan resource path.
     * Prefers local src/main/resources file to reflect file edits immediately.
     */
    public static String loadSvgContent(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) return "";

        // Normalize disk path
        String relativeDiskPath = resourcePath.startsWith("/")
            ? "src/main/resources" + resourcePath
            : "src/main/resources/" + resourcePath;

        File diskFile = new File(relativeDiskPath);
        if (diskFile.exists() && diskFile.isFile()) {
            try {
                return java.nio.file.Files.readString(diskFile.toPath(), StandardCharsets.UTF_8);
            } catch (Exception ignored) {}
        }

        // Classpath fallback
        try (InputStream is = InnerMapRegistry.class.getResourceAsStream(resourcePath)) {
            if (is != null) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            System.err.println("Error loading SVG from " + resourcePath + ": " + e.getMessage());
        }

        return "";
    }

    /**
     * Invalidate cache to force reload from disk.
     */
    public static synchronized void reload() {
        registryCache = null;
        getInnerMaps();
    }
}
