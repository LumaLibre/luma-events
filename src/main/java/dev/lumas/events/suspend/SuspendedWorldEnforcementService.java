package dev.lumas.events.suspend;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.EventPlayerManager;
import dev.lumas.events.obj.EventPlayer;
import dev.lumas.events.utility.Util;
import dev.lumas.events.utility.scheduler.AsynchronousRunnable;
import dev.lumas.lumacore.manager.models.Service;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

// TODO: Use global thread
@Register(Autowire.SERVICE)
public class SuspendedWorldEnforcementService extends AsynchronousRunnable implements Service {
    @Override
    public void accept(ScheduledTask task) {
        World unsuspendWorld = EventMain.getOkaeriConfig().getUnsuspendWorld();
        if (unsuspendWorld == null) return;
        List<String> worldNames = EventMain.getOkaeriConfig().getSuspendedWorlds();

        for (String worldName : worldNames) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                continue;
            }

            for (Player player : world.getPlayers()) {
                if (player.hasPermission("lumaevents.bypass")) continue;
                EventPlayer eventPlayer = EventPlayerManager.getByUUIDOrNull(player.getUniqueId());
                if (eventPlayer == null || !eventPlayer.isSuspended()) {
                    player.teleportAsync(unsuspendWorld.getSpawnLocation());
                    Util.sendMsg(player, "Removed from suspend-only world.");
                }
            }
        }
    }

    @Override
    public void register() {
        this.repeatingAsync(200, 200);
    }

    @Override
    public void unregister() {
        this.cancel();
    }
}
