package dev.lumas.events.utility.latency;

import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

public record LatencyHandler(LatencyTracker latency, boolean enabled, long maxRewindNano, long interpolationMilli) {

    public long rewindOf(Player shooter) {
        if (!enabled) return 0L;

        long asked = TimeUnit.MILLISECONDS.toNanos(
                latency.pingMillis(shooter) + Math.max(0, interpolationMilli));

        return Math.clamp(asked, 0L, maxRewindNano);
    }

    // The moment a shot fired by this player right now should be judged at
    public long timeOf(Player actor) {
        return System.nanoTime() - rewindOf(actor);
    }
}
