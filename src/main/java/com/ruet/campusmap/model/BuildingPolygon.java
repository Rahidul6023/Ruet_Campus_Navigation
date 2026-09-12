package com.ruet.campusmap.model;
import java.util.List;

public class BuildingPolygon {
    private String name;
    private String color;
    private List<double[]> points;
    
    public BuildingPolygon() {}

    public BuildingPolygon(String name, String color, List<double[]> points){
        this.name = name;
        this.color = color;
        this.points = points;
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
}
