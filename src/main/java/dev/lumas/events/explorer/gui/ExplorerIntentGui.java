package dev.lumas.events.explorer.gui;

import dev.lumas.core.model.gui.items.IndexedGuiItem;
import dev.lumas.events.explorer.intention.ExplorerIntent;
import dev.lumas.events.explorer.intention.ExplorerIntentRegistry;
import dev.lumas.events.utility.Util;
import dev.lumas.events.utility.gui.GuiUtil;
import dev.lumas.events.utility.gui.PaginatedGui;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;

@NullMarked
public class ExplorerIntentGui extends ExplorerGui {

    private final Inventory baseInv = GuiUtil.getPaleSideInv(this, "Explorer Intents");

    private final IndexedGuiItem previousPage = IndexedGuiItem.of(
            48,
            GuiUtil.guiArrow(GuiUtil.GuiArrow.LEFT),
            (event, guiItem) -> {
                Inventory prev = this.paginatedGui.getPrevious(event.getInventory());
                if (prev != null) {
                    event.getWhoClicked().openInventory(prev);
                }
            }
    );

    private final IndexedGuiItem nextPage = IndexedGuiItem.of(
            50,
            GuiUtil.guiArrow(GuiUtil.GuiArrow.RIGHT),
            (event, guiItem) -> {
                Inventory next = this.paginatedGui.getNext(event.getInventory());
                if (next != null) {
                    event.getWhoClicked().openInventory(next);
                }
            }
    );

    public ExplorerIntentGui() {
        this.autoRegister();
        List<ItemStack> items = new ArrayList<>();

        for (ExplorerIntent<?> explorerIntent : ExplorerIntentRegistry.jvmUnifiedValues()) {
            ItemStack item = Util.createItem(explorerIntent.getIcon(), meta -> {
                String displayName = Util.paleSideColor(explorerIntent.getTitle());
                List<String> lore = GuiUtil.formatLore(explorerIntent.getDesc().split("\n"));
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.values());
                meta.displayName(Util.color(displayName));
                meta.lore(Util.color(lore, TextColor.fromHexString(Util.TEXT_COLOR)));
            });

            items.add(item);
        }

        this.paginatedGui = new PaginatedGui.Builder()
                .name("<b><gradient:#7B859D:#996779:#A7957B:#6B496B>Pale Side: Intents</gradient></b>")
                .base(baseInv)
                .items(items)
                .startEndSlots(20, 34)
                .ignoredSlots(25, 26, 27, 28)
                .build();
    }

    @Override
    public void onInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onInventoryClose(InventoryCloseEvent event) {

    }


    @Override
    public Inventory getInventory() {
        return baseInv;
    }
}
