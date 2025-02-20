package me.barnaby.trial.config;

/**
 * Enum to represent different configuration files.
 * Each enum constant has an associated filename.
 */
public enum ConfigType {
    MAIN("config.yml"),
    MESSAGES("messages.yml"),
    MONGO("mongo.yml");

    private final String fileName;

    ConfigType(String fileName) {
        this.fileName = fileName;
    }

    /**
     * @return The file name for this configuration type.
     */
    public String getFileName() {
        return fileName;
    }
}
