package me.barnaby.trial.util;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

public class StringUtil {

    public static String format(String string) {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    public static String formatItem(ItemStack item) {
        String[] parts = item.getType().name().split("_");
        StringBuilder formatted = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                formatted.append(part.substring(0, 1).toUpperCase())
                        .append(part.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return formatted.toString().trim();
    }


}
