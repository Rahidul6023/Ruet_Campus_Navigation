package com.ruet.campusmap.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Model representing a RUET faculty/teacher record.
 */
public class Teacher {

    private String id;
    private String name;
    private String designation;
    private String department;
    private String buildingName;
    private String roomNumber;

    public Teacher() {
        this.id = UUID.randomUUID().toString();
    }

    public Teacher(String name, String designation, String department, String buildingName, String roomNumber) {
        this(UUID.randomUUID().toString(), name, designation, department, buildingName, roomNumber);
    }

    public Teacher(String id, String name, String designation, String department, String buildingName, String roomNumber) {
        this.id = (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id;
        this.name = name;
        this.designation = designation;
        this.department = department;
        this.buildingName = buildingName;
        this.roomNumber = roomNumber;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Teacher teacher = (Teacher) o;
        return Objects.equals(id, teacher.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name + " (" + designation + ", " + department + ") - " + buildingName + " [" + roomNumber + "]";
    }
}
