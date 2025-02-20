package me.barnaby.trial.commands;

import dev.s7a.base64.Base64ItemStack;
import me.barnaby.trial.MarketPlace;
import me.barnaby.trial.config.ConfigType;
import me.barnaby.trial.util.StringUtil;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class TransactionsCommand implements CommandExecutor {

    private final MarketPlace plugin;

    public TransactionsCommand(MarketPlace plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String targetId;
        String targetName;
        if (args.length == 0) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                targetId = player.getUniqueId().toString();
                targetName = player.getName();
            } else {
                sender.sendMessage(ChatColor.RED + "Please specify a player.");
                return true;
            }
        } else {
            // Get player by name (online or offline)
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                targetId = target.getUniqueId().toString();
                targetName = target.getName();
            } else {
                // Try to fetch offline player
                targetId = Bukkit.getOfflinePlayer(args[0]).getUniqueId().toString();
                targetName = args[0];
            }
        }

        List<Document> transactions = plugin.getMongoDBManager().getTransactionHistory(targetId);
        if (transactions.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No transactions found for " + targetName + ".");
            return true;
        }
        sender.sendMessage(ChatColor.GOLD + "Transaction History for " + targetName + ":");

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // Retrieve the messages configuration once
        FileConfiguration messagesConfig = plugin.getConfigManager().getConfig(ConfigType.MESSAGES);

        for (Document doc : transactions) {
            String buyerId = doc.getString("buyerId");
            String sellerId = doc.getString("sellerId");
            double price = doc.getDouble("price");

            // Deserialize itemData.
            @SuppressWarnings("unchecked")
            ItemStack item = Base64ItemStack.decode(doc.get("itemData", String.class));
            int amount = item.getAmount();
            String itemName = StringUtil.formatItem(item);
            String time = dateFormat.format(new Date(doc.getLong("timestamp")));
            String line = "";

            // Determine the proper template based on whether the target is the buyer or seller
            String template = "";
            if (targetId.equals(buyerId)) {
                template = messagesConfig.getString("transaction.buy",
                        "Bought %item% x%amount% for $%price% on %time%");
            } else if (targetId.equals(sellerId)) {
                template = messagesConfig.getString("transaction.sell",
                        "Sold %item% x%amount% for $%price% on %time%");
            }

            // Replace placeholders
            line = template.replace("%item%", itemName)
                    .replace("%amount%", String.valueOf(amount))
                    .replace("%price%", String.valueOf(price))
                    .replace("%time%", time);

            sender.sendMessage(ChatColor.GREEN + StringUtil.format(line));
        }
        return true;
    }
}

