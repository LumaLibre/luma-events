package dev.lumas.events;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.Register;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

@Register(Autowire.LISTENER)
public class RunnerBootsRemoval implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta() || clickedItem.getType() != Material.LEATHER_BOOTS) {
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        var modifiers = meta.getAttributeModifiers(Attribute.SCALE);
        if (modifiers != null && !modifiers.isEmpty()) {
            clickedItem.setAmount(0);
        }
    }
}
