package dev.lumas.events.utility.latency;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPong;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Measures latency by sending a ping packet and timing the pong the client sends
 */
public final class PacketPingSource implements PingSource {

    private static final int SAMPLE_WINDOW = 5;
    private static final long TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(5);

    // These IDs seem to be shared with every other plugin on the server, so we start somewhere distinctive
    private final AtomicInteger nextId = new AtomicInteger(ThreadLocalRandom.current().nextInt());

    private final ConcurrentHashMap<UUID, Probe> probes = new ConcurrentHashMap<>();
    private final PacketEventsAPI<?> api;

    @Nullable
    private volatile PacketListenerCommon listener;

    public PacketPingSource() {
        PacketEventsAPI<?> api = PacketEvents.getAPI();
        if (api == null || !api.isInitialized()) {
            throw new IllegalStateException("packetevents is not initialised");
        }
        this.api = api;
    }

    // Whether packetevents is on the server and initialized
    public static boolean isSupported() {
        PacketEventsAPI<?> api = PacketEvents.getAPI();
        return api != null && api.isInitialized();
    }

    @Override
    public String name() {
        return "packetevents ping";
    }

    @Override
    public void start() {
        if (this.listener != null) return;
        this.listener = api.getEventManager().registerListener(new PongListener());
    }

    @Override
    public void stop() {
        PacketListenerCommon registered = this.listener;
        this.listener = null;
        if (registered != null) api.getEventManager().unregisterListener(registered);
        probes.clear();
    }

    @Override
    public void probe(Player player) {
        Probe probe = probes.computeIfAbsent(player.getUniqueId(), _ -> new Probe());

        int id;
        synchronized (probe) {
            long now = System.nanoTime();
            // Still waiting on the last one, so either the client is slow or the probe was lost
            if (probe.pending && now - probe.sentAt < TIMEOUT_NANOS) return;

            id = nextId.incrementAndGet();
            probe.pendingId = id;
            probe.pending = true;
            probe.sentAt = now;
        }

        api.getPlayerManager().sendPacket(player, new WrapperPlayServerPing(id));
    }

    @Override
    public void retainOnly(Set<UUID> keep) {
        probes.keySet().retainAll(keep);
    }

    @Override
    public int pingMillis(Player player) {
        Probe probe = probes.get(player.getUniqueId());
        return probe == null ? -1 : probe.median();
    }

    private void onPong(UUID uuid, int id) {
        Probe probe = probes.get(uuid);
        if (probe == null) return;

        synchronized (probe) {
            if (!probe.pending || probe.pendingId != id) return; // not one of ours
            probe.pending = false;
            probe.add((int) TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - probe.sentAt));
        }
    }

    private final class PongListener extends PacketListenerAbstract {

        private PongListener() {
            super(PacketListenerPriority.MONITOR);
        }

        @Override
        public void onPacketReceive(PacketReceiveEvent event) {
            if (event.getPacketType() != PacketType.Play.Client.PONG) return;

            User user = event.getUser();
            UUID uuid = user == null ? null : user.getUUID();
            if (uuid == null) return;

            onPong(uuid, new WrapperPlayClientPong(event).getId());
        }
    }

    // One player's outstanding probe and their last few round trips
    private static final class Probe {

        private final int[] samples = new int[SAMPLE_WINDOW];
        private int filled;
        private int next;

        private boolean pending;
        private int pendingId;
        private long sentAt;

        // Callers hold the monitor
        private void add(int millis) {
            samples[next] = Math.max(0, millis);
            this.next = (next + 1) % SAMPLE_WINDOW;
            if (filled < SAMPLE_WINDOW) this.filled++;
        }

        private synchronized int median() {
            if (filled == 0) return -1;
            int[] sorted = Arrays.copyOf(samples, filled);
            Arrays.sort(sorted);
            return sorted[filled / 2];
        }
    }
}
