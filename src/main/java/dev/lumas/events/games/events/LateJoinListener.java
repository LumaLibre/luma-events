package dev.lumas.events.games.events;

import dev.lumas.events.games.MinigameManager;
import dev.lumas.events.games.interfaces.Minigame;
import dev.lumas.events.games.models.CountdownBossBar;
import dev.lumas.lumacore.manager.modules.AutoRegister;
import dev.lumas.lumacore.manager.modules.RegisterType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@AutoRegister(RegisterType.LISTENER)
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
