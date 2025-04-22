package dev.jsinco.luma.lumaevents.games.events;

import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.utility.Util;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.inventory.ItemStack;

@AutoRegister(RegisterType.LISTENER)
public class TheNabbitsPreventCarrotPickupListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerAttemptPickup(PlayerAttemptPickupItemEvent event) {
        ItemStack itemStack = event.getItem().getItemStack();
        if (itemStack.getType() == Material.CARROT && Util.hasPersistentKey(itemStack, "nabbit_carrot")) {
            event.setCancelled(true);
        }
    }
}
