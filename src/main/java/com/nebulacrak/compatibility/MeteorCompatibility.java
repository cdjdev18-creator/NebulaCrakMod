package com.nebulacrak.compatibility;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.MeteorClient;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Compatibility layer for Meteor Client.
 * Handles version checking and feature availability.
 */
public class MeteorCompatibility {
    
    private static final String METEOR_CLIENT_ID = "meteor-client";
    private static boolean checked = false;
    private static boolean compatible = false;
    private static String meteorVersion = "unknown";
    
    /**
     * Checks if Meteor Client is installed and compatible.
     * 
     * @return true if Meteor Client is present and compatible
     */
    public static boolean isMeteorClientPresent() {
        if (!checked) {
            checkCompatibility();
        }
        return compatible;
    }
    
    /**
     * Gets the detected Meteor Client version.
     * 
     * @return Meteor Client version string
     */
    public static String getMeteorVersion() {
        if (!checked) {
            checkCompatibility();
        }
        return meteorVersion;
    }
    
    /**
     * Performs the compatibility check.
     */
    private static void checkCompatibility() {
        checked = true;
        
        try {
            // Check if Meteor Client mod is loaded
            if (FabricLoader.getInstance().isModLoaded(METEOR_CLIENT_ID)) {
                meteorVersion = FabricLoader.getInstance()
                    .getModContainer(METEOR_CLIENT_ID)
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("unknown");
                
                // Verify MeteorClient class is accessible
                Class.forName("meteordevelopment.meteorclient.MeteorClient");
                compatible = true;
            }
        } catch (Exception e) {
            compatible = false;
            meteorVersion = "error: " + e.getMessage();
        }
    }
    
    /**
     * Checks if a specific Meteor Client feature is available.
     * Uses reflection to avoid hard dependencies.
     * 
     * @param className The fully qualified class name to check
     * @return true if the class exists and is accessible
     */
    public static boolean isFeatureAvailable(String className) {
        if (!isMeteorClientPresent()) {
            return false;
        }
        
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Gets a Meteor Client setting value by category and name.
     * 
     * @param category The setting category
     * @param name The setting name
     * @return The setting value or null if not found
     */
    public static Object getMeteorSetting(String category, String name) {
        if (!isMeteorClientPresent()) {
            return null;
        }
        
        try {
            // Use reflection to access Meteor's setting system
            Class<?> settingsClass = Class.forName("meteordevelopment.meteorclient.systems.modules.Module");
            // This is a placeholder - actual implementation depends on Meteor's API
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Registers a keybind with Meteor Client's input system.
     * 
     * @param name The keybind name
     * @param description The keybind description
     * @param defaultKey The default key code
     * @param category The keybind category
     * @param onPress The action to perform on press
     * @return The created Input instance or null if failed
     */
    public static Input registerKeybind(String name, String description, int defaultKey, String category, Runnable onPress) {
        if (!isMeteorClientPresent()) {
            return null;
        }
        
        try {
            Class<?> inputClass = Class.forName("com.meteorclient.utils.misc.input.Input");
            // Use reflection to create and register the keybind
            // Actual implementation depends on Meteor's current API
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Checks if the current Meteor Client version meets minimum requirements.
     * 
     * @param minimumVersion The minimum required version (e.g., "1.2.3")
     * @return true if version meets or exceeds minimum
     */
    public static boolean meetsMinimumVersion(String minimumVersion) {
        if (!isMeteorClientPresent()) {
            return false;
        }
        
        try {
            String[] current = meteorVersion.split("\\.");
            String[] minimum = minimumVersion.split("\\.");
            
            for (int i = 0; i < Math.max(current.length, minimum.length); i++) {
                int currentVer = i < current.length ? Integer.parseInt(current[i]) : 0;
                int minimumVer = i < minimum.length ? Integer.parseInt(minimum[i]) : 0;
                
                if (currentVer > minimumVer) return true;
                if (currentVer < minimumVer) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
