package dev.lumas.events.suspend;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.EventPlayerManager;
import dev.lumas.events.obj.EventPlayer;
import dev.lumas.events.utility.Util;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.List;

@Register(Autowire.LISTENER)
public class SuspendListeners implements Listener {

    // Intentionally set to monitor. We want to interfere with any plugin trying to do this.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("lumaevents.bypass")) return;

        if (isSuspendedOnlyWorld(event.getFrom().getWorld()) && isPlayerSuspended(player)) {
            event.setCancelled(true);
            Util.sendMsg(player, "You cannot leave this world while suspended.");
        } else if (isSuspendedOnlyWorld(event.getTo().getWorld()) && !isPlayerSuspended(player)) {
            event.setCancelled(true);
            Util.sendMsg(player, "You cannot enter this world while not suspended.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPreProcessCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!isSuspendedOnlyWorld(player.getWorld()) || player.hasPermission("lumaevents.bypass")) return;
        List<String> allowedCommands = EventMain.getOkaeriConfig().getCommandWhitelist();
        if (allowedCommands.contains(event.getMessage().split(" ")[0].substring(1).toLowerCase())) return;

        event.setCancelled(true);
        Util.sendMsg(player, "You cannot use that command while in this world.");
    }


    static boolean isSuspendedOnlyWorld(World world) {
        List<String> worldNames = EventMain.getOkaeriConfig().getSuspendedWorlds();
        return worldNames.contains(world.getName());
    }

    static boolean isPlayerSuspended(Player player) {
        EventPlayer eventPlayer = EventPlayerManager.getByUUIDOrNull(player.getUniqueId());
        if (eventPlayer == null) return false;
        return eventPlayer.isSuspended();
    }
}
