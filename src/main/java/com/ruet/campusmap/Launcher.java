package com.ruet.campusmap;

/**
 * Standard non-Application entry point for JavaFX.
 * Bypasses the JDK module system check when running directly from IDEs (like VS Code)
 * or classpath configurations without explicit --module-path arguments.
 */
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
