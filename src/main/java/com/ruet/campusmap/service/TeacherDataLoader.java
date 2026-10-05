package com.ruet.campusmap.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.ruet.campusmap.model.Teacher;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Service to load, persist, and manage Teacher records from/to teachers.json.
 */
public class TeacherDataLoader {

    private static final String JSON_RESOURCE_PATH = "/data/teachers.json";
    private static final String JSON_FILE_PATH = "src/main/resources/data/teachers.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<Teacher>>() {}.getType();

    /**
     * Loads the list of Teacher records.
     * Prefers local disk file (so freshly added/edited records are immediately read),
     * and falls back to the classpath resource if disk file is not found.
     */
    public static synchronized List<Teacher> loadTeachers() {
        File file = new File(JSON_FILE_PATH);
        if (file.exists() && file.length() > 0) {
            try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                List<Teacher> result = GSON.fromJson(reader, LIST_TYPE);
                if (result != null) {
                    return new ArrayList<>(result);
                }
            } catch (Exception e) {
                System.err.println("Could not read teachers from disk file: " + e.getMessage());
            }
        }

        try (InputStream is = TeacherDataLoader.class.getResourceAsStream(JSON_RESOURCE_PATH)) {
            if (is != null) {
                try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    List<Teacher> result = GSON.fromJson(reader, LIST_TYPE);
                    if (result != null) {
                        return new ArrayList<>(result);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Could not load teachers from resource: " + e.getMessage());
        }

        return new ArrayList<>();
    }

    /**
     * Persists the full list of teachers to src/main/resources/data/teachers.json.
     */
    public static synchronized boolean saveTeachers(List<Teacher> teachers) {
        if (teachers == null) {
            teachers = Collections.emptyList();
        }

        boolean saved = false;
        File file = new File(JSON_FILE_PATH);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(teachers, writer);
            saved = true;
        } catch (Exception e) {
            System.err.println("Failed to save teachers to " + JSON_FILE_PATH + ": " + e.getMessage());
        }

        // Also write to target/classes if it exists so running app instantly sees updates
        File targetFile = new File("target/classes/data/teachers.json");
        if (targetFile.getParentFile() != null && targetFile.getParentFile().exists()) {
            try (FileWriter writer = new FileWriter(targetFile, StandardCharsets.UTF_8)) {
                GSON.toJson(teachers, writer);
                saved = true;
            } catch (Exception ignored) {}
        }

        return saved;
    }

    /**
     * Adds a new teacher and saves to disk.
     */
    public static synchronized boolean addTeacher(Teacher teacher) {
        if (teacher == null) return false;
        List<Teacher> teachers = loadTeachers();
        teachers.add(teacher);
        return saveTeachers(teachers);
    }

    /**
     * Updates an existing teacher by matching ID and saves to disk.
     */
    public static synchronized boolean updateTeacher(Teacher updatedTeacher) {
        if (updatedTeacher == null || updatedTeacher.getId() == null) return false;
        List<Teacher> teachers = loadTeachers();
        boolean found = false;
        for (int i = 0; i < teachers.size(); i++) {
            if (updatedTeacher.getId().equals(teachers.get(i).getId())) {
                teachers.set(i, updatedTeacher);
                found = true;
                break;
            }
        }
        if (found) {
            return saveTeachers(teachers);
        }
        return false;
    }

    /**
     * Deletes a teacher by ID and saves to disk.
     */
    public static synchronized boolean deleteTeacher(String teacherId) {
        if (teacherId == null || teacherId.isBlank()) return false;
        List<Teacher> teachers = loadTeachers();
        boolean removed = teachers.removeIf(t -> teacherId.equals(t.getId()));
        if (removed) {
            return saveTeachers(teachers);
        }
        return false;
    }

    /**
     * Searches teachers by a query string matching name, department, designation, building, or room.
     */
    public static List<Teacher> searchTeachers(String query) {
        List<Teacher> all = loadTeachers();
        if (query == null || query.isBlank()) {
            return all;
        }
        String q = query.trim().toLowerCase();
        List<Teacher> matches = new ArrayList<>();
        for (Teacher t : all) {
            if ((t.getName() != null && t.getName().toLowerCase().contains(q)) ||
                (t.getDepartment() != null && t.getDepartment().toLowerCase().contains(q)) ||
                (t.getDesignation() != null && t.getDesignation().toLowerCase().contains(q)) ||
                (t.getBuildingName() != null && t.getBuildingName().toLowerCase().contains(q)) ||
                (t.getRoomNumber() != null && t.getRoomNumber().toLowerCase().contains(q))) {
                matches.add(t);
            }
        }
        return matches;
    }

    /**
     * Finds a teacher by their unique ID.
     */
    public static Optional<Teacher> findById(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        return loadTeachers().stream()
                .filter(t -> id.equals(t.getId()))
                .findFirst();
    }
}
