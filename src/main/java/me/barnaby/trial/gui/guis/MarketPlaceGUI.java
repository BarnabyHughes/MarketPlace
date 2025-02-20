package me.barnaby.trial.gui.guis;

import dev.s7a.base64.Base64ItemStack;
import me.barnaby.trial.MarketPlace;
import me.barnaby.trial.config.ConfigType;
import me.barnaby.trial.gui.GUI;
import me.barnaby.trial.gui.GUIItem;
import me.barnaby.trial.util.ListingUtil;
import me.barnaby.trial.util.StringUtil;
import org.bson.Document;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static me.barnaby.trial.util.ListingUtil.formatTimestamp;
import static me.barnaby.trial.util.ListingUtil.getSellerName;

/**
 * GUI for displaying and interacting with the marketplace listings.
 * Supports pagination and purchasing items.
 */
public class MarketPlaceGUI extends GUI {

    private final MarketPlace marketPlace;
    private final Player player;
    private final int page;
    private final FileConfiguration guiConfig;
    // List of all marketplace listings (each with its item and associated document).
    private final List<ListingUtil.Listing> marketplaceListings;

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
     * Each document's "itemData" is deserialized into an ItemStack
     * and stored together with the Document.
     *
     * @return A list of Listing objects.
     */
    private List<ListingUtil.Listing> loadMarketplaceListings() {
        List<Document> docs = marketPlace.getMongoDBManager().getAllItemListings();
        List<ListingUtil.Listing> listings = new ArrayList<>();
        for (Document doc : docs) {
            String itemData = doc.get("itemData", String.class);
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
        int totalRows = guiConfig.getInt("marketplace-gui.rows", 6);
        int totalSlots = totalRows * 9;
        int itemsStart = guiConfig.getInt("marketplace-gui.items-area.start-slot", 0);
        int itemsEnd = guiConfig.getInt("marketplace-gui.items-area.end-slot", totalSlots - 9 - 1);
        int itemsPerPage = itemsEnd - itemsStart + 1;
        int startIndex = (page - 1) * itemsPerPage;

        // Display the marketplace listings in the available slots.
        for (int slot = itemsStart; slot <= itemsEnd; slot++) {
            int listingIndex = startIndex + (slot - itemsStart);
            if (listingIndex < marketplaceListings.size()) {
                ListingUtil.Listing listing = marketplaceListings.get(listingIndex);
                ItemStack displayItem = listing.item.clone();
                ItemMeta meta = displayItem.getItemMeta();
                double listingPrice = listing.doc.getDouble("price");
                boolean canAfford = marketPlace.getEconomy().getBalance(player) >= listingPrice;

                if (meta != null) {
                    // Set item name formatting based on affordability.
                    String defaultItemName = meta.hasDisplayName() ? meta.getDisplayName() : StringUtil.formatItem(displayItem);
                    String displayName = canAfford
                            ? guiConfig.getString("marketplace-gui.can-afford-item-name", "&a&l%item-name%").replace("%item-name%", defaultItemName)
                            : guiConfig.getString("marketplace-gui.cannot-afford-item-name", "&c&l%item-name%").replace("%item-name%", defaultItemName);
                    meta.setDisplayName(StringUtil.format(displayName));

                    // Set item lore with price, seller, and listing time placeholders.
                    List<String> extraLore = canAfford
                            ? guiConfig.getStringList("marketplace-gui.can-afford-item-lore")
                            : guiConfig.getStringList("marketplace-gui.cannot-afford-item-lore");

                    extraLore = extraLore.stream()
                            .map(StringUtil::format)
                            .map(line -> line.replace("%price%", String.valueOf(listingPrice))
                                    .replace("%seller%", getSellerName(listing.doc.getString("playerId")))
                                    .replace("%listedTime%", formatTimestamp(listing.doc.getLong("timestamp"))))
                            .collect(Collectors.toList());

                    // Append extra lore to the item's existing lore.
                    List<String> currentLore = meta.getLore() != null ? meta.getLore() : new ArrayList<>();
                    currentLore.addAll(extraLore);
                    meta.setLore(currentLore);
                    displayItem.setItemMeta(meta);
                }

                // Set the item in the GUI.
                setItem(slot, new GUIItem(displayItem, e -> {
                    e.setCancelled(true);
                    if (!canAfford) {
                        String cannotAffordMsg = guiConfig.getString("marketplace-gui.cannot-afford-message", "&cYou cannot afford this item!");
                        player.sendMessage(StringUtil.format(cannotAffordMsg));
                        String soundName = guiConfig.getString("marketplace-gui.cannot-afford-sound", "ENTITY_VILLAGER_NO");
                        try {
                            player.playSound(player.getLocation(), org.bukkit.Sound.valueOf(soundName.toUpperCase()), 1.0f, 1.0f);
                        } catch (IllegalArgumentException ex) {
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        }
                    } else {
                        new ConfirmBuyGUI(marketPlace, player, listing, listingPrice).open(player);
                    }
                }));
            }
        }

        // --- Next Page Button ---
        int nextPageSlot = guiConfig.getInt("marketplace-gui.next-page.slot", totalSlots - 1);
        Material nextMat = Material.matchMaterial(guiConfig.getString("marketplace-gui.next-page.material", "ARROW"));
        if (startIndex + itemsPerPage < marketplaceListings.size()) {
            setNavigationButton(nextPageSlot, nextMat, "marketplace-gui.next-page.name", "marketplace-gui.next-page.lore", page + 1);
        }

        // --- Previous Page Button ---
        int prevPageSlot = guiConfig.getInt("marketplace-gui.previous-page.slot", totalSlots - 9);
        Material prevMat = Material.matchMaterial(guiConfig.getString("marketplace-gui.previous-page.material", "ARROW"));
        if (page > 1) {
            setNavigationButton(prevPageSlot, prevMat, "marketplace-gui.previous-page.name", "marketplace-gui.previous-page.lore", page - 1);
        }
    }

    /**
     * Creates a navigation button (next/previous page).
     *
     * @param slot         The GUI slot where the button is placed.
     * @param material     The material of the button item.
     * @param nameKey      The config key for the button display name.
     * @param loreKey      The config key for the button lore.
     * @param newPage      The page number to navigate to.
     */
    private void setNavigationButton(int slot, Material material, String nameKey, String loreKey, int newPage) {
        ItemStack navItem = new ItemStack(material);
        ItemMeta meta = navItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(StringUtil.format(guiConfig.getString(nameKey, "&aPage")));
            meta.setLore(guiConfig.getStringList(loreKey).stream().map(StringUtil::format).collect(Collectors.toList()));
            navItem.setItemMeta(meta);
        }
        setItem(slot, new GUIItem(navItem, e -> {
            e.setCancelled(true);
            new MarketPlaceGUI(marketPlace, player, newPage).open(player);
        }));
    }
}

