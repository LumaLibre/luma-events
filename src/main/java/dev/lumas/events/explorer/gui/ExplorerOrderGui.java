package dev.lumas.events.explorer.gui;

import dev.lumas.core.model.gui.items.IndexedGuiItem;
import dev.lumas.events.explorer.order.ActiveExplorerOrder;
import dev.lumas.events.explorer.order.ExplorerOrder;
import dev.lumas.events.explorer.order.ExplorerOrderCompletion;
import dev.lumas.events.explorer.order.ExplorerOrderRegistry;
import dev.lumas.events.obj.EventPlayer;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import dev.lumas.events.utility.gui.GuiUtil;
import dev.lumas.events.utility.gui.PaginatedGui;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;

@NullMarked
public class ExplorerOrderGui extends ExplorerGui {

    private final Inventory baseInv = GuiUtil.getBaseInv(this, 54, "Explorer Miles");
    private final EventPlayer eventPlayer;
    private PaginatedGui paginatedGui;

    private final IndexedGuiItem previousPage = IndexedGuiItem.of(
            48,
            GuiUtil.guiArrow(GuiUtil.GuiArrow.LEFT),
            (event, guiItem) -> {
                Inventory prev = this.paginatedGui.getPrevious(event.getInventory());
                if (prev == null) {
                    return;
                }
                event.getWhoClicked().openInventory(prev);
            }
    );

    private final IndexedGuiItem nextPage = IndexedGuiItem.of(
            50,
            GuiUtil.guiArrow(GuiUtil.GuiArrow.RIGHT),
            (event, guiItem) -> {
                Inventory next = this.paginatedGui.getNext(event.getInventory());
                if (next == null) {
                    return;
                }
                event.getWhoClicked().openInventory(next);
            }
    );

    public ExplorerOrderGui(EventPlayer eventPlayer) {
        this.eventPlayer = eventPlayer;
        this.autoRegister();
        this.refreshGui();
    }

    private void refreshGui() {
        List<ItemStack> items = new ArrayList<>();

        for (ExplorerOrder<?> explorerOrder : ExplorerOrderRegistry.jvmUnifiedValues()) {

            Material type = Material.AMETHYST_SHARD;
            ItemStack explorerMilePostCard = ItemStack.of(type);

            explorerMilePostCard.editMeta(meta -> {
                ActiveExplorerOrder activeExplorerOrder = eventPlayer.getActiveExplorerOrder(explorerOrder);
                ExplorerOrderCompletion snapshot = activeExplorerOrder != null ? activeExplorerOrder.getImmutableCompletion() : ExplorerOrderCompletion.empty(explorerOrder);

                String displayName = "<light_purple><b>" + explorerOrder.getName();
                List<String> lore = this.createExplorerMileLore(explorerOrder.getObjective(), snapshot);

                if (snapshot.isCompleted()) {
                    meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                }
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                meta.displayName(Util.color(displayName));
                meta.lore(Util.color(lore, NamedTextColor.WHITE));
            });
            items.add(explorerMilePostCard);
        }

        this.paginatedGui = new PaginatedGui.Builder()
                .name("Explorer Miles")
                .base(baseInv)
                .items(items)
                .startEndSlots(20, 34)
                .ignoredSlots(25, 26, 27, 28)
                .build();
    }

    private List<String> createExplorerMileLore(String desc, ExplorerOrderCompletion snapshot) {
        List<String> lore = new ArrayList<>();

        lore.addAll(GuiUtil.formatLore(desc.split("\n")));
        lore.add("");
        lore.add("<#EEE1D5><st>       </st>⋆⁺₊⋆ ★ ⋆⁺₊⋆<st>       </st></#EEE1D5>");
        lore.add("Completed<gray>:</gray> " + snapshot.isCompleted());
        lore.add("Progress " + snapshot.getCurrentQuantity() + "/" + snapshot.getMaxQuantity());
        lore.add("<#EEE1D5><st>       </st>⋆⁺₊⋆ ★ ⋆⁺₊⋆<st>       </st></#EEE1D5>");
        return lore;
    }


    @Override
    public void onInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onInventoryClose(InventoryCloseEvent inventoryCloseEvent) {

    }

    @Override
    public Inventory getInventory() {
        return baseInv;
    }

    @Override
    public void open(HumanEntity humanEntity) {
        Inventory first = this.paginatedGui.getFirst();
        if (!Bukkit.isOwnedByCurrentRegion(humanEntity)) {
            Executors.runSync(humanEntity, () -> humanEntity.openInventory(first));
        } else {
            humanEntity.openInventory(first);
        }
    }

}
