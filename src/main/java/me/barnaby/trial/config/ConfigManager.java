package me.barnaby.trial.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/**
 * ConfigManager handles the initialization, loading, saving, and reloading
 * of different configuration files defined in the ConfigType enum.
 */
public class ConfigManager {

    // Map to hold FileConfiguration objects for each ConfigType
    private final Map<ConfigType, FileConfiguration> configs = new EnumMap<>(ConfigType.class);
    // Map to hold File objects for each ConfigType
    private final Map<ConfigType, File> configFiles = new EnumMap<>(ConfigType.class);

    // Reference to the main plugin instance
    private final JavaPlugin plugin;

    /**
     * Constructor which initializes the ConfigManager and loads all configuration files.
     *
     * @param plugin The main plugin instance.
     */
    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        initializeConfigs();
    }

    /**
     * Iterates over all ConfigType values and initializes their corresponding files.
     */
    private void initializeConfigs() {
        for (ConfigType type : ConfigType.values()) {
            // Get the file name from the enum
            String fileName = type.getFileName();
            // Create a File object in the plugin's data folder
            File configFile = new File(plugin.getDataFolder(), fileName);
            configFiles.put(type, configFile);

            // If the file doesn't exist, copy the default from the jar
            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
                plugin.saveResource(fileName, false);
            }

            // Load the configuration from the file
            FileConfiguration configuration = YamlConfiguration.loadConfiguration(configFile);
            configs.put(type, configuration);
        }
    }

    /**
     * Returns the FileConfiguration for a given ConfigType.
     *
     * @param type The type of configuration to retrieve.
     * @return The FileConfiguration associated with that type.
     */
    public FileConfiguration getConfig(ConfigType type) {
        return configs.get(type);
    }

    /**
     * Reloads a specific configuration file.
     *
     * @param type The type of configuration to reload.
     */
    public void reloadConfig(ConfigType type) {
        File file = configFiles.get(type);
        if (file != null) {
            FileConfiguration configuration = YamlConfiguration.loadConfiguration(file);
            configs.put(type, configuration);
        }
    }

    /**
     * Saves a specific configuration file.
     *
     * @param type The type of configuration to save.
     */
    public void saveConfig(ConfigType type) {
        File file = configFiles.get(type);
        FileConfiguration configuration = configs.get(type);
        if (file != null && configuration != null) {
            try {
                configuration.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save " + type.getFileName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Reloads all configuration files.
     */
    public void reloadAllConfigs() {
        for (ConfigType type : ConfigType.values()) {
            reloadConfig(type);
        }
    }

    /**
     * Saves all configuration files.
     */
    public void saveAllConfigs() {
        for (ConfigType type : ConfigType.values()) {
            saveConfig(type);
        }
    }
}

