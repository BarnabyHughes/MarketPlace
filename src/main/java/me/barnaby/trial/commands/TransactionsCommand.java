package me.barnaby.trial.commands;

import dev.s7a.base64.Base64ItemStack;
import me.barnaby.trial.MarketPlace;
import me.barnaby.trial.config.ConfigType;
import me.barnaby.trial.util.StringUtil;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class TransactionsCommand implements CommandExecutor {

    private final MarketPlace plugin;

    public TransactionsCommand(MarketPlace plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Retrieve permission keys from the MAIN config.
        FileConfiguration mainConfig = plugin.getConfigManager().getConfig(ConfigType.MAIN);
        String selfPerm = mainConfig.getString("permissions.transactions.self");
        String otherPerm = mainConfig.getString("permissions.transactions.other");

        String targetId;
        String targetName;
        boolean isSelf = false;

        // Determine target based on arguments.
        if (args.length == 0) {
            // No argument provided: default to self.
            if (sender instanceof Player player) {
                targetId = player.getUniqueId().toString();
                targetName = player.getName();
                isSelf = true;
            } else {
                sender.sendMessage(ChatColor.RED + "Please specify a player.");
                return true;
            }
        } else {
            // Attempt to find the specified player.
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                targetId = target.getUniqueId().toString();
                targetName = target.getName();
            } else {
                // Fallback: use OfflinePlayer.
                OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(args[0]);
                targetId = offlineTarget.getUniqueId().toString();
                targetName = args[0];
            }
            // Check if the sender is looking up their own transactions.
            if (sender instanceof Player player && player.getUniqueId().toString().equals(targetId)) {
                isSelf = true;
            }
        }

        // Check permissions based on whether the target is self or another player.
        if (sender instanceof Player player) {
            if (isSelf) {
                if (!player.hasPermission(selfPerm)) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to view your transactions.");
                    return true;
                }
            } else {
                if (!player.hasPermission(otherPerm)) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to view others' transactions.");
                    return true;
                }
            }
        }

        // Retrieve the transactions from MongoDB.
        List<Document> transactions = plugin.getMongoDBManager().getTransactionHistory(targetId);
        if (transactions.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No transactions found for " + targetName + ".");
            return true;
        }

        // Send a header message.
        sender.sendMessage(ChatColor.GOLD + "Transaction History for " + targetName + ":");

        // Format the timestamp.
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // Load messages configuration.
        FileConfiguration messagesConfig = plugin.getConfigManager().getConfig(ConfigType.MESSAGES);

        // Iterate through each transaction and display a formatted line.
        for (Document doc : transactions) {
            String buyerId = doc.getString("buyerId");
            String sellerId = doc.getString("sellerId");
            double price = doc.getDouble("price");

            // Decode the item from its Base64 string.
            @SuppressWarnings("unchecked")
            ItemStack item = Base64ItemStack.decode(doc.get("itemData", String.class));
            int amount = item.getAmount();
            String itemName = StringUtil.formatItem(item);
            String time = dateFormat.format(new Date(doc.getLong("timestamp")));
            String template;

            // Choose template based on whether the target was buyer or seller.
            if (targetId.equals(buyerId)) {
                template = messagesConfig.getString("transaction.buy", "Bought %item% x%amount% for $%price% on %time%");
            } else if (targetId.equals(sellerId)) {
                template = messagesConfig.getString("transaction.sell", "Sold %item% x%amount% for $%price% on %time%");
            } else {
                continue; // Should not occur.
            }

            // Replace placeholders.
            String line = template
                    .replace("%item%", itemName)
                    .replace("%amount%", String.valueOf(amount))
                    .replace("%price%", String.valueOf(price))
                    .replace("%time%", time);

            sender.sendMessage(ChatColor.GREEN + StringUtil.format(line));
        }
        return true;
    }
}


