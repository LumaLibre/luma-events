package dev.lumas.events.utility.latency.hitbox;

import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Trail {
    private final int historyTicks;
    private final TrailSnapshot[] snapshots;
    private volatile int written;

    public Trail(int historyTicks) {
        this.historyTicks = historyTicks;
        this.snapshots = new TrailSnapshot[historyTicks];
    }

    public void record(Hitbox hitbox) {
        int index = this.written;
        snapshots[Math.floorMod(index, historyTicks)] =
            new TrailSnapshot(System.nanoTime(), hitbox);
        this.written = index + 1;
    }

    void clear() {
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
        Hitbox from = older.hitbox();
        Hitbox to = newer.hitbox();

        if (from.shape().size() != to.shape().size() || to.sampleOffsets().length != from.sampleOffsets().length) {
            return new TrailSnapshot(when, to);
        }

        List<Hitbox.Section> shape = new ArrayList<>();
        for (int i = 0; i < from.shape().size(); i++) {
            Hitbox.Section fromPart = from.shape().get(i);
            Hitbox.Section toPart = from.shape().get(i);
            // Edge case: Hitbox changed format, ignore previous format
            if (fromPart.part() != toPart.part()) {
                return new TrailSnapshot(when, to);
            }
            shape.add(
                new Hitbox.Section(fromPart.part(), lerp(fromPart.boundingBox(), toPart.boundingBox(), progress))
            );
        }

        Vector[] sampleOffsets = new Vector[to.sampleOffsets().length];
        for (int i = 0; i < sampleOffsets.length; i++) {
            sampleOffsets[i] = lerp(from.sampleOffsets()[i], to.sampleOffsets()[i], progress);
        }

        return new TrailSnapshot(when, new Hitbox(shape, lerp(from.wholeBounding(), to.wholeBounding(), progress), sampleOffsets));
    }

    private static BoundingBox lerp(BoundingBox from, BoundingBox to, double progress) {
        return new BoundingBox(
            lerp(from.getMinX(), to.getMinX(), progress),
            lerp(from.getMinY(), to.getMinY(), progress),
            lerp(from.getMinZ(), to.getMinZ(), progress),
            lerp(from.getMaxX(), to.getMaxX(), progress),
            lerp(from.getMaxY(), to.getMaxY(), progress),
            lerp(from.getMaxZ(), to.getMaxZ(), progress)
        );
    }

    private static Vector lerp(Vector from, Vector to, double progress) {
        return new Vector(
            lerp(from.getX(), to.getX(), progress),
            lerp(from.getY(), to.getY(), progress),
            lerp(from.getZ(), to.getZ(), progress)
        );
    }

    private static double lerp(double from, double to, double progress) {
        return from + ((to - from) * progress);
    }

}
