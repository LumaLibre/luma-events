package dev.jsinco.luma.lumaevents.explorer.events.hooks;

import com.palmergames.bukkit.towny.event.TownClaimEvent;
import com.palmergames.bukkit.towny.event.TownInvitePlayerEvent;
import com.palmergames.bukkit.towny.event.player.PlayerEntersIntoTownBorderEvent;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.explorer.events.AbstractExplorerListener;
import org.bukkit.event.EventHandler;

@AutoRegister(value = RegisterType.LISTENER, listenerRequires = "Towny")
public class TownyListeners extends AbstractExplorerListener {

    @EventHandler
    public void onPlayerEntersIntoTownBorder(PlayerEntersIntoTownBorderEvent event) {
        fire(event, event.getPlayer());
    }

    @EventHandler
    public void onTownInvitePlayer(TownInvitePlayerEvent event) {
        fire(event, event.getInvite().getSenderUUID());
    }

    @EventHandler
    public void onTownClaim(TownClaimEvent event) {
        fire(event, event.getResident().getUUID());
    }
}
