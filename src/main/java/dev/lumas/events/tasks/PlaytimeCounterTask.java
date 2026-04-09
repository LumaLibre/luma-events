package dev.lumas.events.tasks;

import dev.lumas.events.manager.EventPlayerManager;
import dev.lumas.events.obj.EventPlayer;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public record PlaytimeCounterTask(long period, TimeUnit timeUnit) implements Consumer<ScheduledTask> {

    @Override
    public void accept(ScheduledTask scheduledTask) {
        for (Player player : Bukkit.getOnlinePlayers()) {

            long increment = timeUnit.toSeconds(period);
            EventPlayer eventPlayer = EventPlayerManager.getByUUIDOrNull(player.getUniqueId());
            if (eventPlayer != null) {
                eventPlayer.addSecondsPlayed(increment);
            }

        }
    }
}
