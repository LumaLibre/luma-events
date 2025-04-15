package dev.jsinco.luma.lumaevents.explorer.events.hooks;

import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.explorer.events.ExplorerListener;
import me.hexedhero.pp.api.PinataHitEvent;
import me.hexedhero.pp.api.VoteReceivedEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;

@AutoRegister(value = RegisterType.LISTENER, listenerRequires = "PinataParty")
public class PinataPartyListeners extends ExplorerListener {

    @EventHandler
    public void onVoteReceived(VoteReceivedEvent event) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayerIfCached(event.getVote().getUsername());
        if (offlinePlayer != null) {
            fire(event, offlinePlayer.getUniqueId());
        }
    }

    @EventHandler
    public void onPinataHit(PinataHitEvent event) {
        fire(event, event.getPlayer());
    }
}
