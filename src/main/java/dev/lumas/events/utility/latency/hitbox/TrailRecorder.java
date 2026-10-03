package dev.lumas.events.utility.latency.hitbox;

import dev.lumas.events.utility.Executors;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.LivingEntity;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@NullMarked
public class TrailRecorder {

    private final ConcurrentHashMap<UUID, Trail> trails = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, EntityRecorder> recorders = new ConcurrentHashMap<>();
    private final int historyTicks;
    private final Supplier<Collection<LivingEntity>> participantsSupplier;

    public TrailRecorder(int historyTicks, Supplier<Collection<LivingEntity>> participantsSupplier) {
        this.historyTicks = historyTicks;
        this.participantsSupplier = participantsSupplier;
    }

    private void tick() {
        Set<UUID> live = new HashSet<>();

        for (LivingEntity entity : participantsSupplier.get()) {
            live.add(entity.getUniqueId());
            startRecording(entity);
        }

        recorders.entrySet().removeIf(entry -> {
            if (live.contains(entry.getKey())) return false;
            entry.getValue().task().cancel();
            return true;
        });
        trails.keySet().removeIf(uuid -> !live.contains(uuid));
    }

    public void startRecording(LivingEntity entity) {
        UUID uuid = entity.getUniqueId();
        EntityRecorder running = recorders.get(uuid);
        if (running != null && running.entity() == entity && !running.task().isCancelled()) return;

        if (running != null) running.task().cancel();

        Trail trail = new Trail(historyTicks);
        ScheduledTask task = Executors.repeatingSync(entity, 1L, _ -> trail.record(entity));
        if (task == null) return; // Already gone, the next sweep can try again

        trails.put(uuid, trail);
        recorders.put(uuid, new EntityRecorder(entity, task));
    }

    public void stopRecording() {
        recorders.values().forEach(recorder -> recorder.task().cancel());
        recorders.clear();
        trails.clear();
    }

    public @Nullable Trail getEntityTrail(UUID uniqueId) {
        return trails.get(uniqueId);
    }

    private record EntityRecorder(LivingEntity entity, ScheduledTask task) {
    }
}
