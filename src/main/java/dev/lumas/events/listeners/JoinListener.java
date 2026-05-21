package dev.lumas.events.listeners;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.games.MinigameManager;
import dev.lumas.events.games.interfaces.Minigame;
import dev.lumas.events.games.models.CountdownBossBar;
import dev.lumas.events.manager.EventPlayerManager;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Executors;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.concurrent.TimeUnit;

@Register(Autowire.LISTENER)
public class JoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Minigame current = MinigameManager.getInstance().getCurrent();
        EventPlayer eventPlayer = EventPlayerManager.getByUUID(event.getPlayer().getUniqueId());
        boolean claimableCharm = !eventPlayer.isClaimedCharm();

        if (claimableCharm && !eventPlayer.getScores().isEmpty()) {
            Executors.runDelayedAsync(TimeUnit.SECONDS, 3, (t) -> {
                eventPlayer.sendMessage("You have an event charm available to claim!");
            });
        }

        if (current.isOpen())  {
            CountdownBossBar queueBossbar = current.getQueueBossbar();
            if (!queueBossbar.isCancelled()) {
                queueBossbar.addViewer(event.getPlayer());
            }
        }
    }

}
