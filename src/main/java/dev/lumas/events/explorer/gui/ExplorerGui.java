package dev.lumas.events.explorer.gui;

import dev.lumas.core.model.gui.AbstractGui;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.gui.PaginatedGui;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.jspecify.annotations.NonNull;


public abstract class ExplorerGui extends AbstractGui {

    protected PaginatedGui paginatedGui;

    @Override
    public void open(@NonNull HumanEntity humanEntity) {
        Inventory first = this.paginatedGui.getFirst();
        if (!Bukkit.isOwnedByCurrentRegion(humanEntity)) {
            Executors.runSync(humanEntity, () -> humanEntity.openInventory(first));
        } else {
            humanEntity.openInventory(first);
        }
    }
}
