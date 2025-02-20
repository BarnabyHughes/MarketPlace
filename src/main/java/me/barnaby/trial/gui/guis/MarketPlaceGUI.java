package me.barnaby.trial.gui.guis;

import dev.s7a.base64.Base64ItemStack;
import me.barnaby.trial.MarketPlace;
import me.barnaby.trial.config.ConfigType;
import me.barnaby.trial.gui.GUI;
import me.barnaby.trial.gui.GUIItem;
import me.barnaby.trial.util.ListingUtil;
import me.barnaby.trial.util.StringUtil;
import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static me.barnaby.trial.util.ListingUtil.formatTimestamp;

/**
 * GUI for displaying and interacting with the marketplace listings.
 * Supports both regular and black market modes and is paginated based on the guis.yml configuration.
 */
public class MarketPlaceGUI extends GUI {

    private final MarketPlace marketPlace;
    private final Player player;
    private final int page;
    private final boolean isBlackMarket;
    private final FileConfiguration guiConfig;
    private final List<ListingUtil.Listing> marketplaceListings;

    /**
     * Constructs a paginated marketplace GUI.
     *
     * @param marketPlace   The main plugin instance.
     * @param player        The player viewing the marketplace.
     * @param page          The current page number (1-indexed).
     * @param isBlackMarket If true, displays only black market items.
     */
    public MarketPlaceGUI(MarketPlace marketPlace, Player player, int page, boolean isBlackMarket) {
        super(StringUtil.format(marketPlace.getConfigManager().getConfig(ConfigType.GUI)
                        .getString(isBlackMarket ? "blackmarket-gui.name" : "marketplace-gui.name", "&bMarketplace")),
                marketPlace.getConfigManager().getConfig(ConfigType.GUI)
                        .getInt(isBlackMarket ? "blackmarket-gui.rows" : "marketplace-gui.rows", 6),
                player);
        this.marketPlace = marketPlace;
        this.player = player;
        this.page = page;
        this.isBlackMarket = isBlackMarket;
        this.guiConfig = marketPlace.getConfigManager().getConfig(ConfigType.GUI);
        this.marketplaceListings = loadMarketplaceListings();
        setupGUI();
    }

    /**
     * Loads only marketplace listings for the normal shop and only black market listings for the black market.
     *
     * @return A list of Listing objects filtered based on shop type.
     */
    private List<ListingUtil.Listing> loadMarketplaceListings() {
        List<Document> docs = marketPlace.getMongoDBManager().getAllItemListings(); // Get all listings
        List<ListingUtil.Listing> listings = new ArrayList<>();
        for (Document doc : docs) {
            boolean isListingBlackMarket = doc.getBoolean("isBlackMarket", false);
            // Ensure only black market items load in Black Market and only normal items in the normal shop.
            if (isBlackMarket != isListingBlackMarket) continue;
            String itemData = doc.getString("itemData");
            if (itemData != null) {
                try {
                    ItemStack item = Base64ItemStack.decode(itemData);
                    listings.add(new ListingUtil.Listing(item, doc));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return listings;
    }

    /**
     * Sets up the GUI layout, populating it with items and navigation buttons.
     */
    private void setupGUI() {
        // Total slots based on configured rows.
        int totalRows = guiConfig.getInt(isBlackMarket ? "blackmarket-gui.rows" : "marketplace-gui.rows", 6);
        int totalSlots = totalRows * 9;

        // Determine the item area from configuration.
        int itemsStart = guiConfig.getInt(isBlackMarket ? "blackmarket-gui.items-area.start-slot" : "marketplace-gui.items-area.start-slot", 0);
        int itemsEnd = guiConfig.getInt(isBlackMarket ? "blackmarket-gui.items-area.end-slot" : "marketplace-gui.items-area.end-slot", totalSlots - 9 - 1);
        int itemsPerPage = itemsEnd - itemsStart + 1;
        int startIndex = (page - 1) * itemsPerPage;

        // Populate item area.
        for (int slot = itemsStart; slot <= itemsEnd; slot++) {
            int listingIndex = startIndex + (slot - itemsStart);
            if (listingIndex < marketplaceListings.size()) {
                ListingUtil.Listing listing = marketplaceListings.get(listingIndex);
                ItemStack displayItem = listing.item.clone();
                ItemMeta meta = displayItem.getItemMeta();
                double listingPrice = listing.doc.getDouble("price");
                boolean canAfford = marketPlace.getEconomy().getBalance(player) >= listingPrice;

                if (meta != null) {
                    String defaultItemName = meta.hasDisplayName() ? meta.getDisplayName() : StringUtil.formatItem(displayItem);
                    String displayName = canAfford
                            ? guiConfig.getString(isBlackMarket ? "blackmarket-gui.can-afford-item-name" : "marketplace-gui.can-afford-item-name", "&a&l%item-name%")
                            : guiConfig.getString(isBlackMarket ? "blackmarket-gui.cannot-afford-item-name" : "marketplace-gui.cannot-afford-item-name", "&c&l%item-name%");
                    meta.setDisplayName(StringUtil.format(displayName.replace("%item-name%", defaultItemName)));

                    // Choose lore based on affordability.

                    List<String> extraLore = canAfford
                            ? guiConfig.getStringList(isBlackMarket ? "blackmarket-gui.can-afford-item-lore" : "marketplace-gui.can-afford-item-lore")
                            : guiConfig.getStringList(isBlackMarket ? "blackmarket-gui.cannot-afford-item-lore" : "marketplace-gui.cannot-afford-item-lore");
                    extraLore = extraLore.stream()
                            .map(StringUtil::format)
                            .map(line -> line.replace("%price%", String.valueOf(listingPrice))
                                    .replace("%seller%", ListingUtil.getSellerName(listing.doc.getString("playerId")))
                                    .replace("%listedTime%", formatTimestamp(listing.doc.getLong("timestamp"))))
                            .collect(Collectors.toList());
                    meta.setLore(extraLore);
                    displayItem.setItemMeta(meta);
                }
                setItem(slot, new GUIItem(displayItem, e -> {
                    e.setCancelled(true);
                    if (listing.doc.getString("playerId").equals(player.getUniqueId().toString())) {
                        player.sendMessage(ChatColor.RED + "This is your own market listing!");
                        return;
                    }
                    if (!canAfford) {
                        // Send cannot-afford message.
                        String msg = guiConfig.getString(isBlackMarket ? "blackmarket-gui.cannot-afford-message" : "marketplace-gui.cannot-afford-message", "&cYou cannot afford this item!");
                        player.sendMessage(StringUtil.format(msg));
                    } else {
                        // For normal items, open the ConfirmBuyGUI.
                        new ConfirmBuyGUI(marketPlace, player, listing, listingPrice, isBlackMarket).open(player);
                    }
                }));
            }
        }

        // Add navigation buttons in the bottom row.
        addNavigationButtons(totalSlots, itemsPerPage, startIndex);
    }

    /**
     * Adds next and previous page navigation buttons to the GUI.
     *
     * @param totalSlots   Total slots in the GUI.
     * @param itemsPerPage Number of item slots per page.
     * @param startIndex   Index of the first item on the current page.
     */
    private void addNavigationButtons(int totalSlots, int itemsPerPage, int startIndex) {
        // Next Page Button.
        int nextPageSlot = guiConfig.getInt(isBlackMarket ? "blackmarket-gui.next-page.slot" : "marketplace-gui.next-page.slot", totalSlots - 1);
        Material nextMat = Material.matchMaterial(guiConfig.getString(isBlackMarket ? "blackmarket-gui.next-page.material" : "marketplace-gui.next-page.material", "ARROW"));
        ItemStack nextItem = new ItemStack(nextMat);
        ItemMeta nextMeta = nextItem.getItemMeta();
        if (nextMeta != null) {
            String nextName = guiConfig.getString(isBlackMarket ? "blackmarket-gui.next-page.name" : "marketplace-gui.next-page.name", "&aNext Page");
            nextMeta.setDisplayName(StringUtil.format(nextName));
            List<String> nextLore = guiConfig.getStringList(isBlackMarket ? "blackmarket-gui.next-page.lore" : "marketplace-gui.next-page.lore")
                    .stream().map(StringUtil::format).collect(Collectors.toList());
            nextMeta.setLore(nextLore);
            nextItem.setItemMeta(nextMeta);
        }
        // Only show next page if there are more items.
        if (startIndex + itemsPerPage < marketplaceListings.size()) {
            setItem(nextPageSlot, new GUIItem(nextItem, e -> {
                e.setCancelled(true);
                new MarketPlaceGUI(marketPlace, player, page + 1, isBlackMarket).open(player);
            }));
        }

        // Previous Page Button.
        int prevPageSlot = guiConfig.getInt(isBlackMarket ? "blackmarket-gui.previous-page.slot" : "marketplace-gui.previous-page.slot", totalSlots - 9);
        Material prevMat = Material.matchMaterial(guiConfig.getString(isBlackMarket ? "blackmarket-gui.previous-page.material" : "marketplace-gui.previous-page.material", "ARROW"));
        ItemStack prevItem = new ItemStack(prevMat);
        ItemMeta prevMeta = prevItem.getItemMeta();
        if (prevMeta != null) {
            String prevName = guiConfig.getString(isBlackMarket ? "blackmarket-gui.previous-page.name" : "marketplace-gui.previous-page.name", "&aPrevious Page");
            prevMeta.setDisplayName(StringUtil.format(prevName));
            List<String> prevLore = guiConfig.getStringList(isBlackMarket ? "blackmarket-gui.previous-page.lore" : "marketplace-gui.previous-page.lore")
                    .stream().map(StringUtil::format).collect(Collectors.toList());
            prevMeta.setLore(prevLore);
            prevItem.setItemMeta(prevMeta);
        }
        // Only show previous page if page > 1.
        if (page > 1) {
            setItem(prevPageSlot, new GUIItem(prevItem, e -> {
                e.setCancelled(true);
                new MarketPlaceGUI(marketPlace, player, page - 1, isBlackMarket).open(player);
            }));
        }
    }
}
