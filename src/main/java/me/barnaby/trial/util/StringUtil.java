package me.barnaby.trial.util;

import org.bukkit.ChatColor;

public class StringUtil {

    public static String format(String string) {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

}
