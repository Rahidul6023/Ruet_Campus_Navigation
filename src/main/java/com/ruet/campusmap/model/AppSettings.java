package com.ruet.campusmap.model;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Model that holds user preferences and application settings for the RUET Campus Map.
 */
public class AppSettings {

    public enum ThemeMode {
        LIGHT,
        DARK,
        AUTO
    }

    public enum BuildingLabelMode {
        FULL_NAME,
        CODE,
        NONE
    }

    public enum TransportMode {
        WALK,
        BIKE,
        SHUTTLE
    }

    // --- State Properties ---
    private ThemeMode themeMode = ThemeMode.LIGHT;
    private BuildingLabelMode labelMode = BuildingLabelMode.CODE;
    private boolean avoidStairs = false;
    private TransportMode transportMode = TransportMode.WALK;

    // POI Filter Toggles
    private boolean showFood = false;
    private boolean showPrinters = false;
    private boolean showRestrooms = false;
    private boolean showParking = false;

    // Listeners for reactive updates
    private final List<Runnable> changeListeners = new ArrayList<>();

    public AppSettings() {}

    public void addListener(Runnable listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    private void notifyListeners() {
        for (Runnable listener : changeListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Resolves whether the UI should currently be in dark mode.
     * In AUTO mode, checks if local time is evening/night (18:30 to 06:00).
     */
    public boolean isEffectiveDarkMode() {
        if (themeMode == ThemeMode.DARK) {
            return true;
        } else if (themeMode == ThemeMode.LIGHT) {
            return false;
        } else {
            // AUTO Mode: 6:30 PM (18:30) to 6:00 AM
            LocalTime now = LocalTime.now();
            return now.isAfter(LocalTime.of(18, 30)) || now.isBefore(LocalTime.of(6, 0));
        }
    }

    // --- Getters & Setters ---

    public ThemeMode getThemeMode() {
        return themeMode;
    }

    public void setThemeMode(ThemeMode themeMode) {
        if (this.themeMode != themeMode) {
            this.themeMode = themeMode;
            notifyListeners();
        }
    }

    public BuildingLabelMode getLabelMode() {
        return labelMode;
    }

    public void setLabelMode(BuildingLabelMode labelMode) {
        if (this.labelMode != labelMode) {
            this.labelMode = labelMode;
            notifyListeners();
        }
    }

    public boolean isAvoidStairs() {
        return avoidStairs;
    }

    public void setAvoidStairs(boolean avoidStairs) {
        if (this.avoidStairs != avoidStairs) {
            this.avoidStairs = avoidStairs;
            notifyListeners();
        }
    }

    public TransportMode getTransportMode() {
        return transportMode;
    }

    public void setTransportMode(TransportMode transportMode) {
        if (this.transportMode != transportMode) {
            this.transportMode = transportMode;
            notifyListeners();
        }
    }

    public boolean isShowFood() {
        return showFood;
    }

    public void setShowFood(boolean showFood) {
        if (this.showFood != showFood) {
            this.showFood = showFood;
            notifyListeners();
        }
    }

    public boolean isShowPrinters() {
        return showPrinters;
    }

    public void setShowPrinters(boolean showPrinters) {
        if (this.showPrinters != showPrinters) {
            this.showPrinters = showPrinters;
            notifyListeners();
        }
    }

    public boolean isShowRestrooms() {
        return showRestrooms;
    }

    public void setShowRestrooms(boolean showRestrooms) {
        if (this.showRestrooms != showRestrooms) {
            this.showRestrooms = showRestrooms;
            notifyListeners();
        }
    }

    public boolean isShowParking() {
        return showParking;
    }

    public void setShowParking(boolean showParking) {
        if (this.showParking != showParking) {
            this.showParking = showParking;
            notifyListeners();
        }
    }
}
