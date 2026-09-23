package com.ruet.campusmap.model;
import java.util.List;

public class BuildingPolygon {
    private String name;
    private String color;
    private List<double[]> points;
    private boolean visibleToUsers = false;
    
    public BuildingPolygon() {}

    public BuildingPolygon(String name, String color, List<double[]> points){
        this(name, color, points, false);
    }

    public BuildingPolygon(String name, String color, List<double[]> points, boolean visibleToUsers){
        this.name = name;
        this.color = color;
        this.points = points;
        this.visibleToUsers = visibleToUsers;
    }

    public String getName(){
        return name;
    }
    public void setName(String name){
        this.name = name;
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
}
