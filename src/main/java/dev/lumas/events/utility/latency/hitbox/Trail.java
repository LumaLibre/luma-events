package dev.lumas.events.utility.latency.hitbox;

import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class Trail {
    private final int historyTicks;
    private final TrailSnapshot[] snapshots;
    private volatile int written;

    public Trail(int historyTicks) {
        this.historyTicks = historyTicks;
        this.snapshots = new TrailSnapshot[historyTicks];
    }

    public void record(LivingEntity entity) {
        int index = this.written;
        snapshots[Math.floorMod(index, historyTicks)] =
                new TrailSnapshot(System.nanoTime(), entity.getBoundingBox(), entity.getEyeHeight());
        this.written = index + 1;
    }

    private void clear() {
        Arrays.fill(snapshots, null);
        this.written = 0;
    }

    // The hitbox this entity had at <when>, interpolated between the ticks either side of it
    @Nullable
    public TrailSnapshot at(long when) {
        int count = this.written; // Reading this first "publishes" every snapshot below it

        TrailSnapshot newer = null;
        for (int age = 0; age < Math.min(count, historyTicks); age++) {
            TrailSnapshot snapshot = snapshots[Math.floorMod(count - 1 - age, historyTicks)];
            if (snapshot == null) break;
            if (snapshot.takenAt() <= when) {
                return newer == null ? snapshot : between(snapshot, newer, when);
            }
            newer = snapshot;
        }
        return newer;
    }

    private static TrailSnapshot between(TrailSnapshot older, TrailSnapshot newer, long when) {
        long span = newer.takenAt() - older.takenAt();
        if (span <= 0) return newer;

        double progress = Math.clamp((double) (when - older.takenAt()) / span, 0.0, 1.0);
        BoundingBox from = older.hitbox();
        BoundingBox to = newer.hitbox();

        return new TrailSnapshot(when, new BoundingBox(
                lerp(from.getMinX(), to.getMinX(), progress),
                lerp(from.getMinY(), to.getMinY(), progress),
                lerp(from.getMinZ(), to.getMinZ(), progress),
                lerp(from.getMaxX(), to.getMaxX(), progress),
                lerp(from.getMaxY(), to.getMaxY(), progress),
                lerp(from.getMaxZ(), to.getMaxZ(), progress)),
                lerp(older.eyeHeight(), newer.eyeHeight(), progress));
    }

    private static double lerp(double from, double to, double progress) {
        return from + ((to - from) * progress);
    }

}
