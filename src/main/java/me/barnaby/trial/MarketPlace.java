package me.barnaby.trial;

import me.barnaby.trial.commands.MarketplaceCommand;
import me.barnaby.trial.commands.SellCommand;
import me.barnaby.trial.commands.TransactionsCommand;
import me.barnaby.trial.config.ConfigManager;
import me.barnaby.trial.config.ConfigType;
import me.barnaby.trial.discord.DiscordWebhookLogger;
import me.barnaby.trial.listener.PlayerListeners;
import me.barnaby.trial.mongo.MongoDBManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class MarketPlace extends JavaPlugin {

    private final ConfigManager configManager = new ConfigManager(this);
    private final MongoDBManager mongoDBManager = new MongoDBManager(this);
    private Economy economy;
    private DiscordWebhookLogger discordWebhookLogger;

    @Override
    public void onEnable() {
        mongoDBManager.connect();

        registerCommands();
        registerListeners();

        setupEconomy();
        sendEnableMessage();

        registerDiscordHook();
    }

    @Override
    public void onDisable() {
        sendDisableMessage();
    }

    private void registerListeners() {
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new PlayerListeners(), this);
    }

    private void registerCommands() {
        getCommand("sell").setExecutor(new SellCommand(this));
        getCommand("marketplace").setExecutor(new MarketplaceCommand(this));
        getCommand("transactions").setExecutor(new TransactionsCommand(this));
    }

    public void registerDiscordHook() {
        ConfigurationSection discordConfig = getConfigManager().getConfig(ConfigType.MAIN).getConfigurationSection("discord");
        String webhookUrl = discordConfig.getString("webhook");
        String embedTitle = discordConfig.getString("embed.title", "Transaction Log");
        String embedColor = discordConfig.getString("embed.color", "#00FF00");
        String embedDescriptionTemplate = discordConfig.getString("embed.description",
                "A purchase was made: %item% x%amount% for $%price% at %time% by %buyer% from %seller%");
        discordWebhookLogger = new DiscordWebhookLogger(webhookUrl, embedTitle, embedDescriptionTemplate, embedColor);
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MongoDBManager getMongoDBManager() {
        return mongoDBManager;
    }

    public DiscordWebhookLogger getDiscordWebhookLogger() {
        return discordWebhookLogger;
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

    /**
     * Sets up the economy instance using Vault.
     *
     * @return true if successful; false otherwise.
     */
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("Could not find Vault for Economy!");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().severe("Could not find Economy!");
            return false;
        }
        economy = rsp.getProvider();
        return true;
    }

    /**
     * Returns the Vault Economy instance.
     *
     * @return the economy instance.
     */
    public Economy getEconomy() {
        return economy;
    }

}

