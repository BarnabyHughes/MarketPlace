package me.barnaby.trial.util;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

/**
 * Utility class for string formatting operations.
 */
public class StringUtil {

    /**
     *
     * @param string the input string with alternate color codes
     * @return the formatted string with proper color codes
     */
    public static String format(String string) {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    /**
     * Formats the item type name into a more readable form.
     * For example, "DIAMOND_SWORD" becomes "Diamond Sword".
     *
     * @param item the ItemStack whose type name will be formatted
     * @return the formatted item name as a String
     */
    public static String formatItem(ItemStack item) {
        // Split the item type name by underscore characters.
        String[] parts = item.getType().name().split("_");
        StringBuilder formatted = new StringBuilder();

        // Process each part to capitalize the first letter and lowercase the rest.
        for (String part : parts) {
            if (!part.isEmpty()) {
                formatted.append(part.substring(0, 1).toUpperCase())
                        .append(part.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        // Return the trimmed, formatted string.
        return formatted.toString().trim();
    }
}
