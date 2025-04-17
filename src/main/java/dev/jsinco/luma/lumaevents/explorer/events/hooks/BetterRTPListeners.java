package dev.jsinco.luma.lumaevents.explorer.events.hooks;

import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.explorer.events.AbstractExplorerListener;
import me.SuperRonanCraft.BetterRTP.references.customEvents.RTP_TeleportEvent;
import org.bukkit.event.EventHandler;

@AutoRegister(value = RegisterType.LISTENER, listenerRequires = "BetterRTP")
public class BetterRTPListeners extends AbstractExplorerListener {

    @EventHandler
    public void onRTPTeleport(RTP_TeleportEvent event) {
        fire(event, event.getPlayer());
    }
}
