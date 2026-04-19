package dev.lumas.events.listeners;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.manager.EventPlayerManager;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.model.team.EventTeam;
import dev.lumas.events.model.team.EventTeamPlayerHandle;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

@Register(Autowire.LISTENER)
public class EventTeamChatListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        EventPlayer eventPlayer = EventPlayerManager.getByUUIDOrNull(event.getPlayer().getUniqueId());
        if (eventPlayer == null) {
            return;
        }

        EventTeam eventTeam = eventPlayer.getLazyTeam();

        if (eventTeam != null) {
            EventTeamPlayerHandle handle = eventTeam.getMember(eventPlayer);
            if (handle.isPersistentTeamChat()) {
                eventTeam.sendTeamChat(eventPlayer, event.message());
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        EventPlayer eventPlayer = EventPlayerManager.getByUUIDOrNull(event.getPlayer().getUniqueId());
        if (eventPlayer == null) {
            return;
        }

        EventTeam eventTeam = eventPlayer.getLazyTeam();

        if (eventTeam != null) {
            EventTeamPlayerHandle handle = eventTeam.getMember(eventPlayer);
            if (handle.isPersistentTeamChat()) {
                handle.setPersistentTeamChat(false);
            }
        }
    }
}
