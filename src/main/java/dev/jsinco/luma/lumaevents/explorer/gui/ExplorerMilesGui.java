package dev.jsinco.luma.lumaevents.explorer.gui;

import dev.jsinco.luma.lumacore.LumaCore;
import dev.jsinco.luma.lumacore.manager.guis.AbstractGui;
import dev.jsinco.luma.lumacore.manager.guis.GuiItem;
import dev.jsinco.luma.lumaevents.explorer.ExplorerMile;
import dev.jsinco.luma.lumaevents.explorer.constants.ExplorerMiles;
import dev.jsinco.luma.lumaevents.explorer.events.IAItemStacksListener;
import dev.jsinco.luma.lumaevents.explorer.events.IAItemStacksListener.CustomStackNameSpace;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.utility.gui.GuiUtil;
import dev.jsinco.luma.lumaevents.utility.gui.GuiUtil.GuiArrow;
import dev.jsinco.luma.lumaevents.utility.Util;
import dev.jsinco.luma.lumaevents.utility.gui.PaginatedGui;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ExplorerMilesGui extends AbstractGui {

    private final Inventory baseInv = GuiUtil.getBaseInv(this, 54, "Explorer Miles");
    private PaginatedGui paginatedGui;
    private EventPlayer eventPlayer;

    private final GuiItem unlockMore = new GuiItem(
            53,
            Util.editMeta(IAItemStacksListener.getPostCard(CustomStackNameSpace.POSTCARD_PILE), (meta -> {
                meta.displayName(Util.color("<gold><b>Unlock more explorer miles"));
                int unlocked = 0;//eventPlayer.getUnlockedExplorerMiles().size();
                int total = ExplorerMiles.values().size();
                meta.lore(Util.colorList(GuiUtil.formatLore("You have unlocked " + unlocked + "/" + total + " explorer miles. Click to unlock more explorer miles for 100k each."), NamedTextColor.WHITE));
            })),
            (event, guiItem) -> {
                eventPlayer.sendMessage("<red>Not implemented yet.");
            }
    );

    private final GuiItem previousPage = new GuiItem(
            48,
            GuiUtil.guiArrow(GuiArrow.LEFT),
            (event, guiItem) -> {
                Inventory prev = this.paginatedGui.getPrevious(event.getInventory());
                if (prev == null) {
                    return;
                }
                event.getWhoClicked().openInventory(prev);
                eventPlayer.sendMessage("<red>Not implemented yet.");
            }
    );

    private final GuiItem nextPage = new GuiItem(
            50,
            GuiUtil.guiArrow(GuiArrow.RIGHT),
            (event, guiItem) -> {
                Inventory next = this.paginatedGui.getNext(event.getInventory());
                if (next == null) {
                    return;
                }
                event.getWhoClicked().openInventory(next);
                eventPlayer.sendMessage("<red>Not implemented yet.");
            }
    );

    public ExplorerMilesGui(EventPlayer eventPlayer) {
        this.eventPlayer = eventPlayer;

        List<ItemStack> items = new ArrayList<>();

        for (ExplorerMile<?> explorerMile : ExplorerMiles.values()) {
            boolean hasUnlocked = eventPlayer.hasUnlockedExplorerMile(explorerMile);

            CustomStackNameSpace[] type = hasUnlocked ? CustomStackNameSpace.POSTCARDS : CustomStackNameSpace.POSTCARDS_NO_ART;
            ItemStack explorerMilePostCard = IAItemStacksListener.getPostCard(Util.getRandom(type));

            if (explorerMilePostCard == null) {
                continue;
            }

            explorerMilePostCard.editMeta(meta -> {
                // TODO: set some persistent data
                if (hasUnlocked) {
                    meta.displayName(Util.color("<gold><b>" + explorerMile.getTitle()));
                    meta.lore(Util.color(GuiUtil.formatLore(explorerMile.getDesc())));
                } else {
                    meta.displayName(Util.color("<white><b>???"));
                    meta.lore(Util.colorList(GuiUtil.formatLore("You haven't unlocked this explorer mile yet."), NamedTextColor.WHITE));
                }
            });
            items.add(explorerMilePostCard);
        }

        this.autoRegister();

        this.paginatedGui = new PaginatedGui.Builder()
                .name("Explorer Miles")
                .base(baseInv)
                .items(items)
                .startEndSlots(20, 34)
                .ignoredSlots(25, 26, 27, 28)
                .build();
    }

    @Override
    public void onInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);

        // TODO: Checks on postcards/explorermile items
    }

    @Override
    public void onInventoryClose(InventoryCloseEvent inventoryCloseEvent) {

    }

    @Override
    public @NotNull Inventory getInventory() {
        return baseInv;
    }

    @Override
    public void open(HumanEntity humanEntity) {
        Inventory first = this.paginatedGui.getFirst();
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(LumaCore.getInstance(), () -> humanEntity.openInventory(first));
        } else {
            humanEntity.openInventory(first);
        }
    }
}
