package me.barnaby.trial.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class GUIItem {
    private final ItemStack itemStack;
    private final Consumer<InventoryClickEvent> clickEventConsumer;
    public GUIItem(ItemStack item, Consumer<InventoryClickEvent> clickEventConsumer) {
        this.itemStack = item;
        this.clickEventConsumer = clickEventConsumer;
    }

    public GUIItem(ItemStack item) {
        this(item, null);
    }

    public ItemStack item() {
        return itemStack.clone();
    }

    public void onClick(InventoryClickEvent event) {
        if(clickEventConsumer == null || event.getClick().isKeyboardClick()) return;

        clickEventConsumer.accept(event);
    }

}

