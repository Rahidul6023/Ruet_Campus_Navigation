package com.ruet.campusmap.model;

/**
 * Model representing a search dropdown suggestion (Location or Teacher).
 */
public class SearchSuggestion {

    public enum Type {
        LOCATION,
        TEACHER
    }

    private final Type type;
    private final String title;
    private final String subtitle;
    private final Object payload;

    public SearchSuggestion(Type type, String title, String subtitle, Object payload) {
        this.type = type;
        this.title = title;
        this.subtitle = subtitle;
        this.payload = payload;
    }

    public Type getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public Object getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return title;
    }
}
