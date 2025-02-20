package me.barnaby.trial.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private File configFile;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        initialize();
    }

    /**
     * Initializes the configuration by loading defaults and copying them if necessary.
     */
    private void initialize() {
        // Save the default config.yml if it doesn't exist
        plugin.saveDefaultConfig();

        // Load the configuration
        config = plugin.getConfig();

        // Set the configuration to copy defaults
        config.options().copyDefaults(true);

        // Save the configuration to apply defaults to the file
        saveConfig();
    }

    /**
     * Reloads the configuration from the file.
     */
    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    /**
     * Saves the current configuration to the file.
     */
    public void saveConfig() {
        plugin.saveConfig();
    }

    /**
     * Gets a value from the config with the given path.
     *
     * @param path The config path (e.g., "settings.enable-feature").
     * @return The object (cast as needed) or null if not found.
     */
    public Object get(String path) {
        return config.get(path);
    }

    /**
     * Gets a string value from the config with a default.
     *
     * @param path The config path.
     * @param defaultValue The default value if not found.
     * @return The string value.
     */
    public String getString(String path, String defaultValue) {
        return config.getString(path, defaultValue);
    }

    /**
     * Gets an integer value from the config with a default.
     *
     * @param path The config path.
     * @param defaultValue The default value if not found.
     * @return The integer value.
     */
    public int getInt(String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }

    /**
     * Gets a boolean value from the config with a default.
     *
     * @param path The config path.
     * @param defaultValue The default value if not found.
     * @return The boolean value.
     */
    public boolean getBoolean(String path, boolean defaultValue) {
        return config.getBoolean(path, defaultValue);
    }

    /**
     * Sets a value in the config and saves it.
     *
     * @param path  The config path.
     * @param value The value to set.
     */
    public void set(String path, Object value) {
        config.set(path, value);
        saveConfig();
    }

    /**
     * Returns the raw config object if needed.
     */
    public FileConfiguration getConfig() {
        return config;
    }
}
