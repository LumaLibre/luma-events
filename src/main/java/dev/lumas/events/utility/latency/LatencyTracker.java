package dev.lumas.events.utility.latency;

import dev.lumas.events.EventMain;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Externals;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class LatencyTracker {

    private static final String PACKETEVENTS_CLASS = "com.github.retrooper.packetevents.PacketEvents";
    private static final long PROBE_PERIOD_MILLIS = 250L;

    private final Supplier<List<Player>> players;

    private volatile PingSource source = new VanillaPingSource();

    @Nullable
    private volatile ScheduledTask task;

    public LatencyTracker(Supplier<List<Player>> players) {
        this.players = players;
    }

    private static PingSource pickSource() {
        if (Externals.classExists(PACKETEVENTS_CLASS)) {
            try {
                if (PacketPingSource.isSupported()) return new PacketPingSource();
            } catch (Throwable throwable) {
                EventMain.getInstance().getLogger().warning(
                        "packetevents is installed but unusable, latency will fall back to keep-alive pings: "
                                + throwable);
            }
        }
        return new VanillaPingSource();
    }

    public void start() {
        if (this.task != null) return;

        PingSource picked = pickSource();
        this.source = picked;
        picked.start();

        this.task = Executors.runRepeatingAsync(TimeUnit.MILLISECONDS, 0, PROBE_PERIOD_MILLIS, _ -> {
            Set<UUID> live = new HashSet<>();
            for (Player player : players.get()) {
                if (player == null || !player.isOnline()) continue;
                live.add(player.getUniqueId());
                picked.probe(player);
            }
            picked.retainOnly(live);
        });
    }

    public void stop() {
        ScheduledTask running = this.task;
        this.task = null;
        if (running != null) running.cancel();
        source.stop();
    }

    public int pingMillis(Player player) {
        int measured = source.pingMillis(player);
        return measured >= 0 ? measured : Math.max(0, player.getPing());
    }

    public String sourceName() {
        return source.name();
    }
}
