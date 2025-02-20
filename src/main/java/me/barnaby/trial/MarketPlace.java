package me.barnaby.trial;

import me.barnaby.trial.commands.MarketplaceCommand;
import me.barnaby.trial.commands.SellCommand;
import me.barnaby.trial.config.ConfigManager;
import me.barnaby.trial.mongo.MongoDBManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class MarketPlace extends JavaPlugin {

    private final ConfigManager configManager = new ConfigManager(this);
    private final MongoDBManager mongoDBManager = new MongoDBManager(this);

    @Override
    public void onEnable() {
        sendEnableMessage();
        registerCommands();

        mongoDBManager.connect();
    }

    @Override
    public void onDisable() {
        sendDisableMessage();
    }


    public void registerCommands() {
        getCommand("sell").setExecutor(new SellCommand(this));
        getCommand("marketplace").setExecutor(new MarketplaceCommand(this));
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MongoDBManager getMongoDBManager() {
        return mongoDBManager;
    }

    private void sendEnableMessage() {
        String pluginName = getDescription().getName();
        String version = getDescription().getVersion();
        String author = String.join(", ", getDescription().getAuthors());

        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_AQUA + "============================================");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "  " + pluginName + " v" + version);
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "  Developed by: " + ChatColor.AQUA + author);
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "  Status: " + ChatColor.BOLD + "ENABLED!");
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_AQUA + "============================================");
    }

    private void sendDisableMessage() {
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_RED + "============================================");
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "  " + getDescription().getName() + " is now disabled.");
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_RED + "============================================");
    }
}

