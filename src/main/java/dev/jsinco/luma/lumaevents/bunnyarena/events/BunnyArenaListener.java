package dev.jsinco.luma.lumaevents.bunnyarena.events;

import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.bunnyarena.BunnyArenaRegionHandler;
import dev.jsinco.luma.lumaevents.bunnyarena.BunnyArenaScheduler;
import dev.jsinco.luma.lumaevents.utility.Util;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

@AutoRegister(RegisterType.LISTENER)
public class BunnyArenaListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        BunnyArenaRegionHandler regionHandler = BunnyArenaScheduler.getInstance().getBunnyArenaRegionHandler();
        if (regionHandler == null) {
            return;
        }

        LivingEntity entity = event.getEntity();

        if (!regionHandler.getPlayArea().contains(entity)) {
            return;
        }

        event.setDroppedExp(0);
        event.getDrops().clear();

        Player killer;
        if (event.getDamageSource().getCausingEntity() instanceof Player inline) {
            killer = inline;
        } else {
            killer = null;
        }

        if (killer != null) {
            regionHandler.rewardBunnyKill(entity, killer);
        }


        (killer != null ? killer : entity).getLocation().getNearbyPlayers(3.0).forEach(player -> {
            if (killer != null && player == killer) {
                return;
            }
            regionHandler.rewardBunnyKill(entity, player);
        });
    }

    @EventHandler
    public void onEntitySpawnEvent(EntitySpawnEvent event) {
        BunnyArenaRegionHandler regionHandler = BunnyArenaScheduler.getInstance().getBunnyArenaRegionHandler();
        if (regionHandler == null) {
            return;
        }

        Entity entity = event.getEntity();

        if (!regionHandler.getPlayArea().contains(entity)) {
            return;
        }

        if (entity instanceof LivingEntity && !Util.hasPersistentKey(entity, "bunny")) {
            event.setCancelled(true);
        }
    }
}
