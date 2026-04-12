package dev.lumas.events.bunnyarena.events;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.bunnyarena.BunnyArenaRegionHandler;
import dev.lumas.events.bunnyarena.BunnyArenaSchedulerService;
import dev.lumas.events.utility.Util;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTargetEvent;

@Register(Autowire.LISTENER)
public class BunnyArenaListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        BunnyArenaRegionHandler regionHandler = BunnyArenaSchedulerService.getInstance().getBunnyArenaRegionHandler();
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
        BunnyArenaRegionHandler regionHandler = BunnyArenaSchedulerService.getInstance().getBunnyArenaRegionHandler();
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

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        BunnyArenaRegionHandler regionHandler = BunnyArenaSchedulerService.getInstance().getBunnyArenaRegionHandler();
        if (regionHandler == null) {
            return;
        }

        Entity entity = event.getEntity();

        if (!regionHandler.getPlayArea().contains(entity) || !Util.hasPersistentKey(entity, "bunny")) {
            return;
        }

        if (event.getReason() == EntityTargetEvent.TargetReason.TEMPT) {
            event.setCancelled(true);
        }
    }
}