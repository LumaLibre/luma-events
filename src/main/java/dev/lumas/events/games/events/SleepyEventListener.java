package dev.lumas.events.games.events;

import dev.lumas.events.games.interfaces.Minigame;
import dev.lumas.sleepy.api.event.SleepySleepEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public record SleepyEventListener(Minigame minigame) implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSleepySleep(SleepySleepEvent event) {
        minigame.ensureNotIllegal();

        if (event.isTeleport()) {
            minigame.getParticipants().stream()
                .filter(eventPlayer -> eventPlayer.getUuid().equals(event.getPlayer().getUniqueId()))
                .forEach(minigame::removeInactiveParticipant);
        }
    }
}
