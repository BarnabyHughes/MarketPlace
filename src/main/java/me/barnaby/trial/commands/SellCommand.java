package me.barnaby.trial.commands;

import me.barnaby.trial.MarketPlace;
import me.barnaby.trial.config.ConfigType;
import me.barnaby.trial.gui.guis.SellGUI;
import me.barnaby.trial.util.StringUtil;
import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

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

        // Check perm
        if (!player.hasPermission(Objects.requireNonNull(marketPlace.getConfigManager().getConfig(ConfigType.MAIN).getString("permissions.sell")))) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        // Parse the price argument.
        double price;
        try {
            price = Math.round(Double.parseDouble(args[0]) * 100.0) / 100.0;
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid price. Please enter a valid number.");
            return true;
        }

        if (price <= 0) {
            String invalidMsg = marketPlace.getConfigManager().getConfig(ConfigType.MESSAGES)
                    .getString("sell-messages.invalid-price", "&cPlease set a valid price first!");
            player.sendMessage(StringUtil.format(invalidMsg));
        }

        // Get the item in the player's main hand.
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You must hold an item in your hand to sell!");
            return true;
        }

        new SellGUI(marketPlace, player.getInventory().getItemInMainHand(), price, player,
                marketPlace.getConfigManager().getConfig(ConfigType.GUI)).open(player);
        return true;
    }
}
