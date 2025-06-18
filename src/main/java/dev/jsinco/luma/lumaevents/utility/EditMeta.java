package dev.jsinco.luma.lumaevents.utility;

import org.bukkit.inventory.meta.ItemMeta;

@FunctionalInterface
public interface EditMeta {
    void edit(ItemMeta meta);
}
