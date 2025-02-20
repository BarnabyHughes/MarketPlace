package me.barnaby.trial.listener;

import me.barnaby.trial.gui.GUI;
import me.barnaby.trial.gui.GUIItem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

public class PlayerListeners implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GUI gui)) return;
        if (event.getClickedInventory() == null) return;

        if (event.getClickedInventory().getType() == InventoryType.PLAYER) {
            event.setCancelled(true);
        }

        gui.onClick(event);

        GUIItem item = gui.getItem(event.getSlot());
        if (item == null) return;

        event.setCancelled(true);
        item.onClick(event);
    }


}
