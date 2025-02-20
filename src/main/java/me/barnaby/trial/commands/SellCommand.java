package me.barnaby.trial.commands;

import me.barnaby.trial.MarketPlace;
import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SellCommand implements CommandExecutor {

    private final MarketPlace marketPlace;
    public SellCommand(MarketPlace marketPlace) {
        this.marketPlace = marketPlace;
    }

    /**
     * Handles the /sell command.
     * Expected usage: /sell <price>
     * Retrieves the item in the player’s main hand, validates it,
     * removes it from their inventory, serializes it, and then stores it
     * in the MongoDB "itemListings" collection.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Only allow players to execute the command.
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can execute this command.");
            return true;
        }
        Player player = (Player) sender;

        // Validate the argument count.
        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /sell <price>");
            return true;
        }

        // Parse the price argument.
        double price;
        try {
            price = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid price. Please enter a valid number.");
            return true;
        }

        // Get the item in the player's main hand.
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You must hold an item in your hand to sell!");
            return true;
        }

        // Remove the item from the player's hand.
        player.getInventory().setItemInMainHand(null);

        // Serialize the item using Bukkit's built-in serialization.
        // This converts the item to a Map which we wrap in a Document.
        Document itemDoc = new Document(itemInHand.serialize());

        // Create a new document representing the listing.
        Document listing = new Document()
                .append("playerId", player.getUniqueId().toString())
                .append("price", price)
                .append("itemData", itemDoc)
                .append("timestamp", System.currentTimeMillis());

        // Insert the listing into MongoDB.
        marketPlace.getMongoDBManager().insertItemListing(listing);

        player.sendMessage(ChatColor.GREEN + "Your item has been listed for sale at $" + price);
        return true;
    }
}
