package me.barnaby.trial.commands;

import me.barnaby.trial.MarketPlace;
import me.barnaby.trial.config.ConfigType;
import me.barnaby.trial.gui.guis.MarketPlaceGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * Handles the /blackmarket command to open the Black Market GUI or refresh the Black Market.
 */
public class BlackMarketCommand implements CommandExecutor {

    private final MarketPlace marketPlace;

    public BlackMarketCommand(MarketPlace marketPlace) {
        this.marketPlace = marketPlace;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        // If no arguments, default to opening the Black Market GUI
        if (args.length == 0 || args[0].equalsIgnoreCase("open")) {
            if (!player.hasPermission(Objects.requireNonNull(marketPlace.getConfigManager().getConfig(ConfigType.MAIN)
                    .getString("permissions.blackmarket.view")))) {
                player.sendMessage(ChatColor.RED + "You do not have permission to refresh the Black Market.");
                return true;
            }
            new MarketPlaceGUI(marketPlace, player, 1, true).open(player);
            return true;
        }

        // If player types "/blackmarket refresh"
        if (args[0].equalsIgnoreCase("refresh")) {
            if (!player.hasPermission(Objects.requireNonNull(marketPlace.getConfigManager().getConfig(ConfigType.MAIN)
                    .getString("permissions.blackmarket.refresh")))) {
                player.sendMessage(ChatColor.RED + "You do not have permission to refresh the Black Market.");
                return true;
            }

            // Refresh the Black Market (move 5 items)
            marketPlace.getMongoDBManager().moveItemsToBlackMarket();
            player.sendMessage(ChatColor.GOLD + "The Black Market has been refreshed!");

            return true;
        }

        // Invalid argument handling
        player.sendMessage(ChatColor.RED + "Usage: /blackmarket [open|refresh]");
        return true;
    }
}
