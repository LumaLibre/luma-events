package dev.jsinco.luma.lumaevents.explorer.events.hooks;

import com.olziedev.playerwarps.api.events.warp.PlayerWarpCreateEvent;
import com.olziedev.playerwarps.api.events.warp.PlayerWarpSponsorEvent;
import com.olziedev.playerwarps.api.events.warp.PlayerWarpTeleportEvent;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.explorer.events.ExplorerListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

@AutoRegister(value = RegisterType.LISTENER, listenerRequires = "PlayerWarps")
public class PlayerWarpsListeners extends ExplorerListener {

    @EventHandler
    public void onPlayerWarpCreate(PlayerWarpCreateEvent event) {
        if (!(event.getCreator() instanceof Player player)) {
            return;
        }
        fire(event, player);
    }

    @EventHandler
    public void onPlayerWarpTeleport(PlayerWarpTeleportEvent event) {
        fire(event, event.getTeleporter());
    }

    @EventHandler
    public void onPlayerWarpSponsor(PlayerWarpSponsorEvent event) {
        fire(event, event.getPlayer());
    }
}
