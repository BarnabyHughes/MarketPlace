package me.barnaby.trial.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class GUI implements InventoryHolder {
    private final String name;
    private final int rows;
    private final Inventory inventory;
    private final Player owner;
    private final GUIItem[] items;

    public GUI(String name, int rows) {
        this.name = name;
        this.rows = rows;
        this.inventory = Bukkit.createInventory(this, rows * 9, name);
        this.items = new GUIItem[inventory.getContents().length];

        this.owner = null;
    }

    public GUI(String name, int rows, Player player) {
        this.name = name;
        this.rows = rows;
        this.inventory = Bukkit.createInventory(this, rows * 9, name);
        this.items = new GUIItem[inventory.getContents().length];
        this.owner = player;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    public String getName() {
        return this.name;
    }

    public GUIItem[] getItems() {
        return this.items;
    }

    public int getRows() {
        return this.rows;
    }

    public void setItem(int index, GUIItem guiItem) {
        items[index] = guiItem;

        inventory.setItem(index, guiItem.item());
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public void onClick(InventoryClickEvent event) {}
    public void onDrag(InventoryDragEvent event) {}
    public void onClose(InventoryCloseEvent event) {}
    public void onOpen(InventoryOpenEvent event) {}

    public void outline(GUIItem item) {
        if (item == null) {
            return;
        }

        for (int index = 0; index < inventory.getSize(); index++) {
            int row = index / 9;
            int column = (index % 9) + 1;

            if (row == 0 || row == rows - 1 || column == 1 || column == 9) {
                setItem(index, item);
            }
        }
    }

    public void fill(GUIItem item) {
        if (item == null) {
            return;
        }

        for (int index = 0; index < inventory.getSize(); index++) {
            if (items[index] != null) {
                continue;
            }
            setItem(index, item);
        }
    }

    public GUIItem getItem(int index) {
        return items[index];
    }

    public Player getOwner() {
        return this.owner;
    }
}