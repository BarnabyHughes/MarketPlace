package me.barnaby.trial.gui.guis;

import me.barnaby.trial.MarketPlace;
import me.barnaby.trial.config.ConfigType;
import me.barnaby.trial.gui.GUI;
import me.barnaby.trial.gui.GUIItem;
import me.barnaby.trial.util.StringUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bson.Document;

import java.util.List;
import java.util.stream.Collectors;

public class ConfirmBuyGUI extends GUI {

    private final MarketPlace marketPlace;
    private final Player player;
    // The listing being purchased (contains the item and its document)
    private final MarketPlaceGUI.Listing listing;
    private final double price;
    private final FileConfiguration guiConfig;
    private final FileConfiguration messagesConfig;


    /**
     * Constructs the ConfirmBuyGUI.
     *
     * @param marketPlace The main plugin instance.
     * @param player      The player buying the item.
     * @param listing     The listing (item + document) being purchased.
     * @param price       The price of the item.
     */
    public ConfirmBuyGUI(MarketPlace marketPlace, Player player, MarketPlaceGUI.Listing listing, double price) {
        super(StringUtil.format(marketPlace.getConfigManager().getConfig(ConfigType.GUI)
                        .getString("confirmbuy-gui.name", "&aConfirm Purchase")),
                marketPlace.getConfigManager().getConfig(ConfigType.GUI).getInt("confirmbuy-gui.rows", 3)
                ,
                player);
        this.marketPlace = marketPlace;
        this.player = player;
        this.listing = listing;
        this.price = price;
        this.guiConfig = marketPlace.getConfigManager().getConfig(ConfigType.GUI);
        this.messagesConfig = marketPlace.getConfigManager().getConfig(ConfigType.MESSAGES);
        setupGUI();
    }

    private void setupGUI() {
        // --- Confirm Button ---
        int confirmSlot = guiConfig.getInt("confirmbuy-gui.confirm.slot", 11);
        Material confirmMat = Material.matchMaterial(guiConfig.getString("confirmbuy-gui.confirm.material", "GREEN_STAINED_GLASS_PANE"));
        ItemStack confirmItem = new ItemStack(confirmMat);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName(StringUtil.format(guiConfig.getString("confirmbuy-gui.confirm.name", "&aConfirm Purchase")));
            List<String> confirmLore = guiConfig.getStringList("confirmbuy-gui.confirm.lore")
                    .stream().map(s -> StringUtil.format(
                            s.replace("%item%", StringUtil.formatItem(listing.item))
                                    .replace("%amount%", listing.item.getAmount() + "")
                                    .replace("%price%", price + ""))).collect(Collectors.toList());
            confirmMeta.setLore(confirmLore);
            confirmItem.setItemMeta(confirmMeta);
        }
        setItem(confirmSlot, new GUIItem(confirmItem, e -> {
            e.setCancelled(true);
            // Double-check balance before purchase.
            if (marketPlace.getEconomy().getBalance(player) < price) {
                String failMsg = messagesConfig.getString("confirmbuy-gui.failure-message", "&cYou cannot afford this item!");
                player.sendMessage(StringUtil.format(failMsg));
                String failSound = messagesConfig.getString("confirmbuy-gui.failure-sound", "ENTITY_VILLAGER_NO");
                try {
                    player.playSound(player.getLocation(), Sound.valueOf(failSound.toUpperCase()), 1.0f, 1.0f);
                } catch (IllegalArgumentException ex) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
            } else {
                // Deduct the money and give the item.
                marketPlace.getEconomy().withdrawPlayer(player, price);
                player.getInventory().addItem(listing.item);
                String successMsg = messagesConfig.getString("confirmbuy-gui.success-message", "&aPurchase successful!");
                player.sendMessage(StringUtil.format(successMsg));
                String successSound = messagesConfig.getString("confirmbuy-gui.success-sound", "ENTITY_PLAYER_LEVELUP");
                try {
                    player.playSound(player.getLocation(), Sound.valueOf(successSound.toUpperCase()), 1.0f, 1.0f);
                } catch (IllegalArgumentException ex) {
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                }
                player.closeInventory();
                // Optionally: remove the listing from the marketplace.
            }
        }));

        // --- Cancel Button ---
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
            new MarketPlaceGUI(marketPlace, player, 1).open(player);
        }));
    }
}
