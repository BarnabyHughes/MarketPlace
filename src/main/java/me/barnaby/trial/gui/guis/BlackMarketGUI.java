package me.barnaby.trial.gui.guis;

import me.barnaby.trial.MarketPlace;
import me.barnaby.trial.gui.GUI;
import me.barnaby.trial.gui.GUIItem;
import me.barnaby.trial.util.StringUtil;
import org.bson.Document;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * GUI for displaying black market items.
 */
public class BlackMarketGUI extends GUI {

    private final MarketPlace marketPlace;
    private final Player player;
    private final List<Document> blackMarketListings;

    public BlackMarketGUI(MarketPlace marketPlace, Player player) {
        super("&8Black Market", 3, player);
        this.marketPlace = marketPlace;
        this.player = player;
        this.blackMarketListings = marketPlace.getMongoDBManager().getBlackMarketListings();
        setupGUI();
    }

    private void setupGUI() {
        int slot = 0;
        for (Document doc : blackMarketListings) {
            if (slot >= 27) break;

            ItemStack item = new ItemStack(Material.DIAMOND);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§c" + doc.getString("playerId") + "'s Item");
            meta.setLore(List.of("§7Price: $" + doc.getDouble("price")));
            item.setItemMeta(meta);

            setItem(slot++, new GUIItem(item, e -> {
                e.setCancelled(true);
                player.sendMessage("§6You cannot buy from the Black Market yet!");
            }));
        }
    }
}
