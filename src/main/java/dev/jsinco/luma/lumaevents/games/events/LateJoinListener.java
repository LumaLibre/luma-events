package dev.jsinco.luma.lumaevents.games.events;

import dev.jsinco.luma.lumaevents.games.MinigameManager;
import dev.jsinco.luma.lumaevents.games.interfaces.Minigame;
import dev.jsinco.luma.lumaevents.games.obj.CountdownBossBar;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class LateJoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        Minigame current = MinigameManager.getInstance().getCurrent();
        if (!current.isOpen()) return;

        CountdownBossBar queueBossbar = current.getQueueBossbar();
        if (queueBossbar.isCancelled()) return;

        queueBossbar.addViewer(event.getPlayer());
    }

}
