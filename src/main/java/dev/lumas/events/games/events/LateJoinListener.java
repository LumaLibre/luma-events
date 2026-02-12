package dev.lumas.events.games.events;

import dev.lumas.events.games.MinigameManager;
import dev.lumas.events.games.interfaces.Minigame;
import dev.lumas.events.games.obj.CountdownBossBar;
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
