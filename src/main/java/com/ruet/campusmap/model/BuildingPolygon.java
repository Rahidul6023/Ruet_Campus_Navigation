package com.ruet.campusmap.model;

import java.util.List;

/**
 * Model representing a campus building polygon hitbox and its metadata.
 */
public class BuildingPolygon {
    private String name;
    private String codeName;
    private String color;
    private List<double[]> points;
    private boolean visibleToUsers = false;
    private String imagePath;
    
    public BuildingPolygon() {}

    public BuildingPolygon(String name, String color, List<double[]> points){
        this(name, "", color, points, false);
    }

    public BuildingPolygon(String name, String color, List<double[]> points, boolean visibleToUsers){
        this(name, "", color, points, visibleToUsers);
    }

    public BuildingPolygon(String name, String codeName, String color, List<double[]> points, boolean visibleToUsers){
        this(name, codeName, color, points, visibleToUsers, null);
    }

    public BuildingPolygon(String name, String codeName, String color, List<double[]> points, boolean visibleToUsers, String imagePath){
        this.name = name;
        this.codeName = codeName;
        this.color = color;
        this.points = points;
        this.visibleToUsers = visibleToUsers;
        this.imagePath = imagePath;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getCodeName(){
        return codeName;
    }

    public void setCodeName(String codeName){
        this.codeName = codeName;
    }

    public String getColor(){
        return color;
    }

    public void setColor(String color){
        this.color = color;
    }

    public List<double[]> getPoints(){
        return points;
    }

    public void setPoints(List<double[]> points){
        this.points = points;
    }

    public boolean isVisibleToUsers() {
        return visibleToUsers;
    }

    public void setVisibleToUsers(boolean visibleToUsers) {
        this.visibleToUsers = visibleToUsers;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}
