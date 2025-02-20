package me.barnaby.trial;

import me.barnaby.trial.commands.BlackMarketCommand;
import me.barnaby.trial.commands.MarketplaceCommand;
import me.barnaby.trial.commands.SellCommand;
import me.barnaby.trial.commands.TransactionsCommand;
import me.barnaby.trial.config.ConfigManager;
import me.barnaby.trial.config.ConfigType;
import me.barnaby.trial.discord.DiscordWebhookLogger;
import me.barnaby.trial.listener.PlayerListeners;
import me.barnaby.trial.mongo.MongoDBManager;
import me.barnaby.trial.runnables.BlackMarketRunnable;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main class for the MarketPlace plugin.
 */
public class MarketPlace extends JavaPlugin {

    private final ConfigManager configManager = new ConfigManager(this);
    private final MongoDBManager mongoDBManager = new MongoDBManager(this);
    private Economy economy;
    private DiscordWebhookLogger discordWebhookLogger;

    @Override
    public void onEnable() {
        // Connect to MongoDB.
        mongoDBManager.connect();

        // Register commands and event listeners.
        registerCommands();
        registerListeners();

        // Setup Vault economy and log plugin enable status.
        setupEconomy();
        sendEnableMessage();

        // Register Discord webhook for transaction logging.
        registerDiscordHook();

        new BlackMarketRunnable(this)
                .runTaskTimer(this,0,
                        configManager.getConfig(ConfigType.MAIN)
                                .getLong("blackmarket.add-items-every") * 20);
    }

    @Override
    public void onDisable() {
        sendDisableMessage();
    }

    /**
     * Registers all event listeners.
     */
    private void registerListeners() {
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new PlayerListeners(), this);
    }

    /**
     * Registers all commands and sets their executors.
     */
    private void registerCommands() {
        getCommand("sell").setExecutor(new SellCommand(this));
        getCommand("marketplace").setExecutor(new MarketplaceCommand(this));
        getCommand("transactions").setExecutor(new TransactionsCommand(this));
        getCommand("blackmarket").setExecutor(new BlackMarketCommand(this));
    }

    /**
     * Initializes the Discord webhook logger from the configuration.
     */
    public void registerDiscordHook() {
        ConfigurationSection discordConfig = getConfigManager()
                .getConfig(ConfigType.MAIN)
                .getConfigurationSection("discord");
        String webhookUrl = discordConfig.getString("webhook");
        String embedTitle = discordConfig.getString("embed.title", "Transaction Log");
        String embedColor = discordConfig.getString("embed.color", "#00FF00");
        String embedDescriptionTemplate = discordConfig.getString("embed.description",
                "A purchase was made: %item% x%amount% for $%price% at %time% by %buyer% from %seller%");
        discordWebhookLogger = new DiscordWebhookLogger(webhookUrl, embedTitle, embedDescriptionTemplate, embedColor);
    }

    /**
     * Returns the configuration manager.
     *
     * @return the ConfigManager instance.
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Returns the MongoDB manager.
     *
     * @return the MongoDBManager instance.
     */
    public MongoDBManager getMongoDBManager() {
        return mongoDBManager;
    }

    /**
     * Returns the Discord webhook logger.
     *
     * @return the DiscordWebhookLogger instance.
     */
    public DiscordWebhookLogger getDiscordWebhookLogger() {
        return discordWebhookLogger;
    }

    /**
     * Sends an enable message to the console.
     */
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

    /**
     * Sends a disable message to the console.
     */
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
     * @return the Economy instance.
     */
    public Economy getEconomy() {
        return economy;
    }
}

