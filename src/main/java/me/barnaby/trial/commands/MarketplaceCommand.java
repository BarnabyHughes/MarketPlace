package me.barnaby.trial.commands;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import me.barnaby.trial.MarketPlace;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MarketplaceCommand implements CommandExecutor {

    private final MarketPlace marketPlace;
    public MarketplaceCommand(MarketPlace marketPlace) {
        this.marketPlace = marketPlace;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Only allow players to execute this command.
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can execute this command.");
            return true;
        }
        Player player = (Player) sender;

        // Retrieve item listings from MongoDB.
        MongoCollection<Document> collection = marketPlace.getMongoDBManager()
                .getCollection("itemListings");
        List<Document> listings = new ArrayList<>();

        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                listings.add(cursor.next());
            }
        }

        // If there are no listings, inform the player.
        if (listings.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "There are no items listed in the marketplace.");
            return true;
        }

        // Create a basic chest GUI (27 slots) to display the listings.
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.BLUE + "Marketplace");

        // Iterate over each listing and add the item to the GUI.
        for (Document listing : listings) {
            // Retrieve the serialized item data.
            Map<String, Object> itemMap = (Map<String, Object>) listing.get("itemData");
            if (itemMap == null) continue;

            // Deserialize the item.
            ItemStack itemStack = ItemStack.deserialize(itemMap);

            // Retrieve the price and update the display name.
            double price = listing.getDouble("price");
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + "$" + price);
                itemStack.setItemMeta(meta);
            }

            // Add the item to the GUI.
            gui.addItem(itemStack);
        }

        // Open the GUI for the player.
        player.openInventory(gui);
        return true;
    }
}
