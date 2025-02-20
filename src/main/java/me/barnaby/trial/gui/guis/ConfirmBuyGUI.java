package me.barnaby.trial.gui.guis;

import me.barnaby.trial.MarketPlace;
import me.barnaby.trial.config.ConfigType;
import me.barnaby.trial.gui.GUI;
import me.barnaby.trial.gui.GUIItem;
import me.barnaby.trial.util.ListingUtil;
import me.barnaby.trial.util.StringUtil;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static me.barnaby.trial.util.ListingUtil.formatTimestamp;
import static me.barnaby.trial.util.ListingUtil.getSellerName;

/**
 * GUI for confirming a purchase.
 */
public class ConfirmBuyGUI extends GUI {

    private final MarketPlace marketPlace;
    private final Player player;
    // The listing being purchased (contains the item and its original Document)
    private final ListingUtil.Listing listing;
    private final double price;
    private final FileConfiguration guiConfig;
    private final FileConfiguration messagesConfig;
    private final boolean isBlackMarket;

    /**
     * Constructs the ConfirmBuyGUI.
     *
     * @param marketPlace  The main plugin instance.
     * @param player       The player buying the item.
     * @param listing      The listing (item + document) being purchased.
     * @param price        The price of the item.
     * @param isBlackMarket Whether this is a black market purchase.
     */
    public ConfirmBuyGUI(MarketPlace marketPlace, Player player, ListingUtil.Listing listing, double price, boolean isBlackMarket) {
        super(StringUtil.format(marketPlace.getConfigManager().getConfig(ConfigType.GUI)
                        .getString("confirmbuy-gui.name", "&aConfirm Purchase")),
                marketPlace.getConfigManager().getConfig(ConfigType.GUI).getInt("confirmbuy-gui.rows", 3),
                player);
        this.marketPlace = marketPlace;
        this.player = player;
        this.listing = listing;
        this.price = price;
        this.isBlackMarket = isBlackMarket;
        this.guiConfig = marketPlace.getConfigManager().getConfig(ConfigType.GUI);
        this.messagesConfig = marketPlace.getConfigManager().getConfig(ConfigType.MESSAGES);
        setupGUI();
    }

    /**
     * Sets up the Confirm Buy GUI by adding confirm and cancel buttons.
     */
    private void setupGUI() {
        // Setup Confirm Button.
        setupConfirmButton();
        // Setup Cancel Button.
        setupCancelButton();
    }

    /**
     * Sets up the confirm button in the GUI.
     */
    private void setupConfirmButton() {
        int confirmSlot = guiConfig.getInt("confirmbuy-gui.confirm.slot", 11);
        Material confirmMat = Material.matchMaterial(guiConfig.getString("confirmbuy-gui.confirm.material", "GREEN_STAINED_GLASS_PANE"));
        ItemStack confirmItem = new ItemStack(confirmMat);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName(StringUtil.format(guiConfig.getString("confirmbuy-gui.confirm.name", "&aConfirm Purchase")));
            List<String> confirmLore = guiConfig.getStringList("confirmbuy-gui.confirm.lore")
                    .stream().map(s -> StringUtil.format(
                            s.replace("%item%", StringUtil.formatItem(listing.item))
                                    .replace("%amount%", String.valueOf(listing.item.getAmount()))
                                    .replace("%price%", String.valueOf(price))
                    )).collect(Collectors.toList());
            confirmMeta.setLore(confirmLore);
            confirmItem.setItemMeta(confirmMeta);
        }
        setItem(confirmSlot, new GUIItem(confirmItem, e -> {
            e.setCancelled(true);
            processPurchase();
        }));
    }

    /**
     * Sets up the cancel button in the GUI.
     */
    private void setupCancelButton() {
        int cancelSlot = guiConfig.getInt("confirmbuy-gui.cancel.slot", 15);
        Material cancelMat = Material.matchMaterial(guiConfig.getString("confirmbuy-gui.cancel.material", "RED_STAINED_GLASS_PANE"));
        ItemStack cancelItem = new ItemStack(cancelMat);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName(StringUtil.format(guiConfig.getString("confirmbuy-gui.cancel.name", "&cCancel")));
            List<String> cancelLore = guiConfig.getStringList("confirmbuy-gui.cancel.lore")
                    .stream().map(StringUtil::format).collect(Collectors.toList());
            cancelMeta.setLore(cancelLore);
            cancelItem.setItemMeta(cancelMeta);
        }
        setItem(cancelSlot, new GUIItem(cancelItem, e -> {
            e.setCancelled(true);
            String cancelMsg = messagesConfig.getString("confirmbuy-gui.cancel-message", "&cPurchase cancelled.");
            player.sendMessage(StringUtil.format(cancelMsg));
            // Return to marketplace GUI.
            new MarketPlaceGUI(marketPlace, player, 1, isBlackMarket).open(player);
        }));
    }

    /**
     * Processes the purchase when the confirm button is clicked.
     * Checks for balance, withdraws funds, gives the item, records the transaction,
     * logs to Discord, and removes the listing.
     */
    private void processPurchase() {
        // Check if the player can afford the purchase.
        if (marketPlace.getEconomy().getBalance(player) < price) {
            sendFailureFeedback();
        } else {
            // Process purchase: update economy, deliver item, record transaction, etc.
            player.closeInventory();
            // Calculate modified prices if black market.
            double newBuyingPrice = price;
            double newSellingPrice = price;
            if (isBlackMarket) {
                // Round new buying price to two decimals.
                newBuyingPrice = Math.round(price * marketPlace.getConfigManager().getConfig(ConfigType.MAIN)
                        .getDouble("blackmarket.price-modifier") * 100.0) / 100.0;
                newSellingPrice *= marketPlace.getConfigManager().getConfig(ConfigType.MAIN)
                        .getDouble("blackmarket.sell-bonus");
            }
            // Update economy: deduct from buyer, deposit to seller.
            marketPlace.getEconomy().withdrawPlayer(player, newBuyingPrice);
            OfflinePlayer seller = Bukkit.getOfflinePlayer(UUID.fromString(listing.doc.getString("playerId")));
            marketPlace.getEconomy().depositPlayer(seller, newSellingPrice);
            // Give the item to the buyer.
            player.getInventory().addItem(listing.item);
            // Send success message and sound.
            sendSuccessFeedback(newBuyingPrice);
            // Record the transaction.
            marketPlace.getMongoDBManager().recordTransaction(
                    player.getUniqueId().toString(),
                    listing.doc.getString("playerId"),
                    listing.item,
                    price
            );
            // Log purchase to Discord.
            marketPlace.getDiscordWebhookLogger().sendPurchaseLog(
                    player.getName(),
                    getSellerName(listing.doc.getString("playerId")),
                    StringUtil.formatItem(listing.item),
                    listing.item.getAmount(),
                    price,
                    ListingUtil.formatTimestamp(listing.doc.getLong("timestamp"))
            );
            // Remove the listing from the marketplace.
            marketPlace.getMongoDBManager().deleteValue("itemListings", new Document("_id", listing.doc.get("_id")));
        }
    }

    /**
     * Sends failure feedback when the player cannot afford the item.
     */
    private void sendFailureFeedback() {
        String failMsg = messagesConfig.getString("confirmbuy-gui.failure-message", "&cYou cannot afford this item!");
        player.sendMessage(StringUtil.format(failMsg));
        String failSound = messagesConfig.getString("confirmbuy-gui.failure-sound", "ENTITY_VILLAGER_NO");
        try {
            player.playSound(player.getLocation(), Sound.valueOf(failSound.toUpperCase()), 1.0f, 1.0f);
        } catch (IllegalArgumentException ex) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    /**
     * Sends success feedback upon a successful purchase.
     *
     * @param newBuyingPrice The modified buying price.
     */
    private void sendSuccessFeedback(double newBuyingPrice) {
        String successMsg;
        if (isBlackMarket) {
            successMsg = messagesConfig.getString("blackmarket.success-message",
                            "&aPurchase &8> &fYou bought %item% for &c&m%oldprice%&f %price%!")
                    .replace("%item%", StringUtil.formatItem(listing.item))
                    .replace("%amount%", String.valueOf(listing.item.getAmount()))
                    .replace("%oldprice%", String.valueOf(price))
                    .replace("%price%", String.valueOf(newBuyingPrice));
        } else {
            successMsg = messagesConfig.getString("confirmbuy-gui.success-message", "&aPurchase successful!");
        }
        player.sendMessage(StringUtil.format(successMsg));
        String successSound = messagesConfig.getString("confirmbuy-gui.success-sound", "ENTITY_PLAYER_LEVELUP");
        try {
            player.playSound(player.getLocation(), Sound.valueOf(successSound.toUpperCase()), 1.0f, 1.0f);
        } catch (IllegalArgumentException ex) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }
}
