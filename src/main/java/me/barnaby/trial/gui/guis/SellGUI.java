package me.barnaby.trial.gui.guis;

import me.barnaby.trial.MarketPlace;
import me.barnaby.trial.config.ConfigType;
import me.barnaby.trial.gui.GUI;
import me.barnaby.trial.gui.GUIItem;
import me.barnaby.trial.util.StringUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Collectors;

public class SellGUI extends GUI {

    private final MarketPlace marketPlace;
    private final ItemStack itemStack;
    private final double price; // Price is provided via /sell <price>
    private final Player player;

    /**
     * Constructs the SellGUI.
     *
     * @param marketPlace The main plugin instance.
     * @param itemStack   The item to be sold.
     * @param price       The sale price provided as a command argument.
     * @param player      The player selling the item.
     * @param guiConfig   The configuration for the GUI.
     */
    public SellGUI(MarketPlace marketPlace, ItemStack itemStack, double price, Player player, FileConfiguration guiConfig) {
        super(StringUtil.format(
                guiConfig.getString("sellgui.name")
                        .replace("%item%", itemStack.getType().name())
                        .replace("%amount%", String.valueOf(itemStack.getAmount()))
        ), guiConfig.getInt("sellgui.rows"), player);
        this.marketPlace = marketPlace;
        this.itemStack = itemStack;
        this.price = price;
        this.player = player;
        setupGUI();
    }

    private void setupGUI() {
        FileConfiguration guiConfig = marketPlace.getConfigManager().getConfig(ConfigType.GUI);
        FileConfiguration messagesConfig = marketPlace.getConfigManager().getConfig(ConfigType.MESSAGES);

        // Display the item being sold at a fixed slot (slot 4).
        setItem(4, new GUIItem(itemStack, e -> e.setCancelled(true)));

        // --- Confirm Button ---
        Material confirmMat = Material.matchMaterial(guiConfig.getString("sellgui.confirm.material", "GREEN_STAINED_GLASS_PANE"));
        ItemStack confirmItem = new ItemStack(confirmMat);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName(StringUtil.format(guiConfig.getString("sellgui.confirm.name", "&aConfirm")));
            List<String> confirmLore = guiConfig.getStringList("sellgui.confirm.lore")
                    .stream()
                    .map(StringUtil::format)
                    .collect(Collectors.toList());
            confirmMeta.setLore(confirmLore);
            confirmItem.setItemMeta(confirmMeta);
        }
        // For a 4-row GUI (36 slots), use a slot number within 0-35.
        int confirmSlot = guiConfig.getInt("sellgui.confirm.slot", 27);
        setItem(confirmSlot, new GUIItem(confirmItem, e -> {
            e.setCancelled(true);
            marketPlace.getMongoDBManager().insertItemListing(
                    new org.bson.Document("playerId", player.getUniqueId().toString())
                            .append("price", price)
                            .append("itemData", itemStack.serialize())
                            .append("timestamp", System.currentTimeMillis())
            );
            String successMsg = messagesConfig.getString("sell-messages.sale-success", "&aItem listed for sale at $%price%");
            successMsg = successMsg.replace("%price%", String.valueOf(price));
            player.sendMessage(StringUtil.format(successMsg));
            // Remove the item from the player's main hand.
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            player.closeInventory();
        }));

        // --- Cancel Button ---
        Material cancelMat = Material.matchMaterial(guiConfig.getString("sellgui.cancel.material", "RED_STAINED_GLASS_PANE"));
        ItemStack cancelItem = new ItemStack(cancelMat);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName(StringUtil.format(guiConfig.getString("sellgui.cancel.name", "&cCancel")));
            List<String> cancelLore = guiConfig.getStringList("sellgui.cancel.lore")
                    .stream()
                    .map(StringUtil::format)
                    .collect(Collectors.toList());
            cancelMeta.setLore(cancelLore);
            cancelItem.setItemMeta(cancelMeta);
        }
        int cancelSlot = guiConfig.getInt("sellgui.cancel.slot", 35);
        setItem(cancelSlot, new GUIItem(cancelItem, e -> {
            e.setCancelled(true);
            String cancelMsg = messagesConfig.getString("sell-messages.sale-cancelled", "&cSale cancelled.");
            player.sendMessage(StringUtil.format(cancelMsg));
            player.closeInventory();
        }));

        // --- Price Display (Sign) ---
        Material priceMat = Material.matchMaterial(guiConfig.getString("sellgui.price.material", "OAK_SIGN"));
        ItemStack priceItem = new ItemStack(priceMat);
        ItemMeta priceMeta = priceItem.getItemMeta();
        if (priceMeta != null) {
            priceMeta.setDisplayName(StringUtil.format(guiConfig.getString("sellgui.price.name", "&ePrice: $") + price));
            List<String> priceLore = guiConfig.getStringList("sellgui.price.lore")
                    .stream()
                    .map(StringUtil::format)
                    .collect(Collectors.toList());
            priceMeta.setLore(priceLore);
            priceItem.setItemMeta(priceMeta);
        }
        int priceSlot = guiConfig.getInt("sellgui.price.slot", 31);
        // The price display is informational and non-interactive.
        setItem(priceSlot, new GUIItem(priceItem, e -> e.setCancelled(true)));
    }
}
