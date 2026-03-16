package dev.lumas.events.suspend;

import dev.lumas.events.utility.Util;
import dev.lumas.lumacore.manager.modules.AutoRegister;
import dev.lumas.lumacore.manager.modules.RegisterType;
import io.canvasmc.canvas.event.EntityTeleportAsyncEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import static dev.lumas.events.suspend.SuspendListeners.isPlayerSuspended;
import static dev.lumas.events.suspend.SuspendListeners.isSuspendedOnlyWorld;

@AutoRegister(value = RegisterType.LISTENER, requires = "io.canvasmc.canvas.event.EntityTeleportAsyncEvent")
public class CanvasSuspendListeners implements Listener {


    // Intentionally set to monitor. We want to interfere with any plugin trying to do this.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityTeleportAsync(EntityTeleportAsyncEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.hasPermission("lumaevents.bypass")) return;

        if (isSuspendedOnlyWorld(event.getFrom().getWorld()) && isPlayerSuspended(player)) {
            event.setCancelled(true);
            Util.sendMsg(player, "You cannot leave this world while suspended.");
        } else if (isSuspendedOnlyWorld(event.getTo().getWorld()) && !isPlayerSuspended(player)) {
            event.setCancelled(true);
            Util.sendMsg(player, "You cannot enter this world while not suspended.");
        }
    }
}
