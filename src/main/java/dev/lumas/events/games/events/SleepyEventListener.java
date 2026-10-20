package dev.lumas.events.games.events;

import dev.lumas.events.games.interfaces.Minigame;
import dev.lumas.sleepy.api.event.SleepySleepEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public record SleepyEventListener(Minigame minigame) implements Listener {

    // Sleepy fires this every second while a player is marked AFK. The minigame's own inactivity
    // check warns and removes participants, so we keep Sleepy from pulling them out mid-game.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSleepySleep(SleepySleepEvent event) {
        minigame.ensureNotIllegal();

        boolean participant = minigame.getParticipants().stream()
            .anyMatch(eventPlayer -> eventPlayer.getUuid().equals(event.getPlayer().getUniqueId()));
        if (participant) {
            event.setCancelled(true);
        }
    }
}
