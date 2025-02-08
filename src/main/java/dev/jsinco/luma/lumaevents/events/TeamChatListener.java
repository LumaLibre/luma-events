package dev.jsinco.luma.lumaevents.events;

import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.obj.EventTeam;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@AutoRegister(RegisterType.LISTENER)
public class TeamChatListener implements Listener {

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        if (!event.getPlayer().hasMetadata("teamchat")) {
            return;
        }
        EventPlayer eplayer = EventPlayerManager.getByUUID(event.getPlayer().getUniqueId());
        EventTeam team = EventTeam.ofOnlinePlayers(eplayer.getTeamType());
        team.teamMsg(event.getPlayer(), event.message());
        event.setCancelled(true);
    }
}
