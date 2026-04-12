package dev.lumas.events.shop;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.manager.EventPlayerManager;
import dev.lumas.events.obj.EventPlayer;
import dev.lumas.events.utility.Util;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;

@Register(Autowire.LISTENER)
public class ShopListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        Inventory topInv = event.getView().getTopInventory();
        if (!(topInv.getHolder() instanceof ShopManagerService.ShopHolder)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() != topInv) return;

        if (!event.getClick().isLeftClick()) return;

        int slot = event.getSlot();
        ShopEntry entry = ShopManagerService.getInstance().getEntry(slot);
        if (!(entry instanceof ShopEntry.Item shopItem)) return;

        EventPlayer eventPlayer = EventPlayerManager.getByUUID(player.getUniqueId());
        ShopManagerService.PurchaseResult result = ShopManagerService.getInstance().tryPurchase(player, eventPlayer, slot);
        ShopLang lang = ShopManagerService.getInstance().lang();

        switch (result) {
            case SUCCESS -> {
                String msg = lang.get("purchase_success")
                        .replace("{souls}", String.valueOf(eventPlayer.getSouls()));
                Util.sendMsg(player, msg);
                ShopManagerService.getInstance().refreshInventory(topInv, player.getUniqueId());
            }
            case LIMIT_REACHED -> {
                String msg = shopItem.maxPerPlayer() == 1
                        ? lang.get("limit_reached_single")
                        : lang.get("limit_reached_multi")
                                .replace("{limit}", String.valueOf(shopItem.maxPerPlayer()));
                Util.sendMsg(player, msg);
            }
            case OUT_OF_STOCK ->
                    Util.sendMsg(player, lang.get("out_of_stock"));
            case NOT_ENOUGH_SOULS -> {
                String msg = lang.get("not_enough_souls")
                        .replace("{price}", String.valueOf(shopItem.price()))
                        .replace("{souls}", String.valueOf(eventPlayer.getSouls()));
                Util.sendMsg(player, msg);
            }
            case INVALID_SLOT -> {}
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof ShopManagerService.ShopHolder)) return;
        int topSize = event.getView().getTopInventory().getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (ShopManagerService.isDisplayItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getItemDrop().remove();
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (ShopManagerService.isDisplayItem(event.getMainHandItem())) event.setCancelled(true);
        if (ShopManagerService.isDisplayItem(event.getOffHandItem())) event.setCancelled(true);
    }
}
