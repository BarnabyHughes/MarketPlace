package me.barnaby.trial.util;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class ListingUtil {
    /**
     * Formats a timestamp (in milliseconds) into a human-readable date/time string.
     *
     * @param timestamp The timestamp in milliseconds.
     * @return A formatted date/time string.
     */
    public static String formatTimestamp(long timestamp) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault());
        return formatter.format(Instant.ofEpochMilli(timestamp));
    }

    /**
     * Retrieves the seller's name given their UUID string.
     *
     * @param uuidStr The seller's UUID as a string.
     * @return The seller's name, or the UUID if no name is found.
     */
    public static String getSellerName(String uuidStr) {
        try {
            UUID uuid = UUID.fromString(uuidStr);
            return Bukkit.getOfflinePlayer(uuid).getName() != null ? Bukkit.getOfflinePlayer(uuid).getName() : uuidStr;
        } catch (Exception e) {
            return uuidStr;
        }
    }

    /**
     * A helper inner class to wrap an ItemStack with its associated Document.
     */
    public static class Listing {
        public final ItemStack item;
        public final Document doc;

        public Listing(ItemStack item, Document doc) {
            this.item = item;
            this.doc = doc;
        }
    }
}
