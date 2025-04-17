package dev.jsinco.luma.lumaevents.explorer.events.hooks;

import dev.jsinco.luma.lumaevents.explorer.events.AbstractExplorerListener;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.ListenerPriority;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageReceivedEvent;

import java.util.UUID;

public class DiscordSRVListeners extends AbstractExplorerListener {

    @Subscribe(priority = ListenerPriority.MONITOR)
    public void onDiscordMessageReceived(DiscordGuildMessageReceivedEvent event) {
        UUID uuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(event.getAuthor().getId());
        if (uuid == null) {
            return;
        }
        fire(event, uuid);
    }
}
