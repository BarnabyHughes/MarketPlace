package me.barnaby.trial.commands;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import me.barnaby.trial.MarketPlace;
import me.barnaby.trial.gui.guis.MarketPlaceGUI;
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

        new MarketPlaceGUI(marketPlace, player, 1).open(player);
        return true;
    }
}
