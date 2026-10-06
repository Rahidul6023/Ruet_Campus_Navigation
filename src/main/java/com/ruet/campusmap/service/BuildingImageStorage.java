package com.ruet.campusmap.service;

import javafx.scene.image.Image;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Service to manage building picture upload, persistence, and loading.
 * Stores building images in src/main/resources/images/buildings/ and target/classes/images/buildings/
 * with human-readable sanitized filenames based on the building name.
 */
public class BuildingImageStorage {

    private static final String RESOURCES_DIR = "src/main/resources/images/buildings";
    private static final String TARGET_DIR = "target/classes/images/buildings";

    /**
     * Copies an uploaded image file into the project resources directory with a sanitized building filename.
     * Also copies to target/classes if available for instant runtime feedback.
     *
     * @param sourceFile   the chosen file from user's device
     * @param buildingName the name or code of the building
     * @return the relative image path stored in BuildingPolygon (e.g. "images/buildings/cse_department.jpg")
     */
    public static String saveBuildingImage(File sourceFile, String buildingName) {
        if (sourceFile == null || !sourceFile.exists()) {
            return null;
        }

        String safeName = sanitizeBuildingName(buildingName);
        String ext = getFileExtension(sourceFile);
        String fileName = safeName + "." + ext;
        String relativePath = "images/buildings/" + fileName;

        try {
            // 1. Copy to src/main/resources/images/buildings/
            File srcDir = new File(RESOURCES_DIR);
            if (!srcDir.exists()) {
                srcDir.mkdirs();
            }
            File srcDest = new File(srcDir, fileName);
            Files.copy(sourceFile.toPath(), srcDest.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // 2. Also copy to target/classes/images/buildings/ for immediate runtime loading
            File targetDir = new File(TARGET_DIR);
            if (targetDir.getParentFile() != null && targetDir.getParentFile().exists()) {
                if (!targetDir.exists()) {
                    targetDir.mkdirs();
                }
                File targetDest = new File(targetDir, fileName);
                Files.copy(sourceFile.toPath(), targetDest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            return relativePath;
        } catch (Exception e) {
            System.err.println("Failed to save building image: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Loads a JavaFX Image from disk or classpath resource.
     *
     * @param imagePath relative path (e.g. "images/buildings/cse.jpg") or absolute path
     * @return Image if found, or null if not found/error
     */
    public static Image loadBuildingImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }

        try {
            // 1. Check local file in src/main/resources
            File srcFile = new File("src/main/resources/" + imagePath);
            if (srcFile.exists()) {
                return new Image(srcFile.toURI().toString());
            }

            // 2. Check local file in target/classes
            File targetFile = new File("target/classes/" + imagePath);
            if (targetFile.exists()) {
                return new Image(targetFile.toURI().toString());
            }

            // 3. Check direct path
            File directFile = new File(imagePath);
            if (directFile.exists()) {
                return new Image(directFile.toURI().toString());
            }

            // 4. Fallback to classpath resource
            String resourcePath = imagePath.startsWith("/") ? imagePath : ("/" + imagePath);
            InputStream is = BuildingImageStorage.class.getResourceAsStream(resourcePath);
            if (is != null) {
                return new Image(is);
            }
        } catch (Exception e) {
            System.err.println("Error loading building image from " + imagePath + ": " + e.getMessage());
        }

        return null;
    }

    private static String sanitizeBuildingName(String name) {
        if (name == null || name.isBlank()) {
            return "building_" + System.currentTimeMillis();
        }
        String clean = name.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("_+", "_");
        if (clean.startsWith("_")) {
            clean = clean.substring(1);
        }
        if (clean.endsWith("_")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean.isBlank() ? ("building_" + System.currentTimeMillis()) : clean;
    }

    private static String getFileExtension(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot >= 0 && dot < name.length() - 1) {
            return name.substring(dot + 1).toLowerCase();
        }
        return "jpg";
    }
}
