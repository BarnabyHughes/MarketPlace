package me.barnaby.trial.gui.guis;

import me.barnaby.trial.MarketPlace;
import me.barnaby.trial.config.ConfigType;
import me.barnaby.trial.gui.GUI;
import me.barnaby.trial.gui.GUIItem;
import me.barnaby.trial.util.StringUtil;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class MarketPlaceGUI extends GUI {

    private final MarketPlace marketPlace;
    private final Player player;
    private final int page;
    private final FileConfiguration guiConfig;
    // List of all marketplace listings (each with its item and associated document)
    private final List<Listing> marketplaceListings;

    /**
     * Constructs a paginated marketplace GUI.
     *
     * @param marketPlace The main plugin instance.
     * @param player      The player viewing the marketplace.
     * @param page        The current page number (1-indexed).
     */
    public MarketPlaceGUI(MarketPlace marketPlace, Player player, int page) {
        super(StringUtil.format(marketPlace.getConfigManager().getConfig(ConfigType.GUI)
                        .getString("marketplace-gui.name", "&bMarketplace")),
                marketPlace.getConfigManager().getConfig(ConfigType.GUI).getInt("marketplace-gui.rows", 6),
                player);
        this.marketPlace = marketPlace;
        this.player = player;
        this.page = page;
        this.guiConfig = marketPlace.getConfigManager().getConfig(ConfigType.GUI);
        this.marketplaceListings = loadMarketplaceListings();
        setupGUI();
    }

    /**
     * Loads all marketplace listings from the database.
     * Each document's "itemData" is deserialized into an ItemStack,
     * and stored together with the Document.
     *
     * @return A list of Listing objects.
     */
    private List<Listing> loadMarketplaceListings() {
        List<Document> docs = marketPlace.getMongoDBManager().getAllItemListings(); // Assumes this method exists.
        List<Listing> listings = new ArrayList<>();
        for (Document doc : docs) {
            Map<String, Object> itemData = doc.get("itemData", Map.class);
            if (itemData != null) {
                try {
                    ItemStack item = ItemStack.deserialize(itemData);
                    listings.add(new Listing(item, doc));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return listings;
    }

    private void setupGUI() {
        // Total number of slots in the GUI.
        int totalRows = guiConfig.getInt("marketplace-gui.rows", 6);
        int totalSlots = totalRows * 9;

        // Define the items area using config (so the bottom row is reserved for navigation).
        int itemsStart = guiConfig.getInt("marketplace-gui.items-area.start-slot", 0);
        int itemsEnd = guiConfig.getInt("marketplace-gui.items-area.end-slot", totalSlots - 9 - 1); // Default: bottom row reserved.
        int itemsPerPage = itemsEnd - itemsStart + 1;
        int startIndex = (page - 1) * itemsPerPage;

        // Fill the items area with marketplace listings for the current page.
        for (int slot = itemsStart; slot <= itemsEnd; slot++) {
            int listingIndex = startIndex + (slot - itemsStart);
            if (listingIndex < marketplaceListings.size()) {
                Listing listing = marketplaceListings.get(listingIndex);
                // Clone the item so as not to modify the original.
                ItemStack displayItem = listing.item.clone();
                ItemMeta meta = displayItem.getItemMeta();
                if (meta != null) {
                    // Retrieve price from document.
                    double listingPrice = listing.doc.getDouble("price");
                    // Check if player can afford the item.
                    boolean canAfford = marketPlace.getEconomy().getBalance(player) >= listingPrice;

                    // Determine the display name based on affordability.
                    String defaultItemName = (meta.hasDisplayName() ? meta.getDisplayName() :
                            StringUtil.formatItem(displayItem));
                    String displayName;
                    if (canAfford) {
                        displayName = guiConfig.getString("marketplace-gui.can-afford-item-name", "&a&l%item-name%")
                                .replace("%item-name%", defaultItemName);
                    } else {
                        displayName = guiConfig.getString("marketplace-gui.cannot-afford-item-name", "&c&l%item-name%")
                                .replace("%item-name%", defaultItemName);
                    }
                    meta.setDisplayName(StringUtil.format(displayName));

                    // Choose lore based on affordability.
                    List<String> extraLore;
                    if (canAfford) {
                        extraLore = guiConfig.getStringList("marketplace-gui.can-afford-item-lore");
                    } else {
                        extraLore = guiConfig.getStringList("marketplace-gui.cannot-afford-item-lore");
                    }
                    // Process lore: replace placeholders %price%, %seller%, %listedTime%.
                    extraLore = extraLore.stream()
                            .map(StringUtil::format)
                            .map(line -> line.replace("%price%", String.valueOf(listingPrice))
                                    .replace("%seller%", getSellerName(listing.doc.getString("playerId")))
                                    .replace("%listedTime%", formatTimestamp(listing.doc.getLong("timestamp"))))
                            .collect(Collectors.toList());

                    // Append extra lore to any existing lore.
                    List<String> currentLore = meta.getLore() != null ? meta.getLore() : new ArrayList<>();
                    currentLore.addAll(extraLore);
                    meta.setLore(currentLore);
                    displayItem.setItemMeta(meta);
                }
                setItem(slot, new GUIItem(displayItem, e -> {
                    e.setCancelled(true);
                    // Optionally, add code to handle purchasing when an item is clicked.
                }));
            }
        }

        // --- Next Page Button ---
        int nextPageSlot = guiConfig.getInt("marketplace-gui.next-page.slot", totalSlots - 1);
        Material nextMat = Material.matchMaterial(guiConfig.getString("marketplace-gui.next-page.material", "ARROW"));
        ItemStack nextItem = new ItemStack(nextMat);
        ItemMeta nextMeta = nextItem.getItemMeta();
        if (nextMeta != null) {
            String nextName = guiConfig.getString("marketplace-gui.next-page.name", "&aNext Page");
            nextMeta.setDisplayName(StringUtil.format(nextName));
            List<String> nextLore = guiConfig.getStringList("marketplace-gui.next-page.lore")
                    .stream().map(StringUtil::format).collect(Collectors.toList());
            nextMeta.setLore(nextLore);
            nextItem.setItemMeta(nextMeta);
        }
        // Only show next page button if there are more listings.
        if (startIndex + itemsPerPage < marketplaceListings.size()) {
            setItem(nextPageSlot, new GUIItem(nextItem, e -> {
                e.setCancelled(true);
                new MarketPlaceGUI(marketPlace, player, page + 1).open(player);
            }));
        }

        // --- Previous Page Button ---
        int prevPageSlot = guiConfig.getInt("marketplace-gui.previous-page.slot", totalSlots - 9);
        Material prevMat = Material.matchMaterial(guiConfig.getString("marketplace-gui.previous-page.material", "ARROW"));
        ItemStack prevItem = new ItemStack(prevMat);
        ItemMeta prevMeta = prevItem.getItemMeta();
        if (prevMeta != null) {
            String prevName = guiConfig.getString("marketplace-gui.previous-page.name", "&aPrevious Page");
            prevMeta.setDisplayName(StringUtil.format(prevName));
            List<String> prevLore = guiConfig.getStringList("marketplace-gui.previous-page.lore")
                    .stream().map(StringUtil::format).collect(Collectors.toList());
            prevMeta.setLore(prevLore);
            prevItem.setItemMeta(prevMeta);
        }
        // Only show previous page button if we're beyond the first page.
        if (page > 1) {
            setItem(prevPageSlot, new GUIItem(prevItem, e -> {
                e.setCancelled(true);
                new MarketPlaceGUI(marketPlace, player, page - 1).open(player);
            }));
        }
    }

    /**
     * Formats a timestamp (in milliseconds) into a human-readable date/time string.
     *
     * @param timestamp The timestamp in milliseconds.
     * @return A formatted date/time string.
     */
    private String formatTimestamp(long timestamp) {
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
    private String getSellerName(String uuidStr) {
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
    private static class Listing {
        public final ItemStack item;
        public final Document doc;

        public Listing(ItemStack item, Document doc) {
            this.item = item;
            this.doc = doc;
        }
    }
}
