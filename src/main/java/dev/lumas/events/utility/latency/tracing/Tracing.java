package dev.lumas.events.utility.latency.tracing;

import dev.lumas.events.configurable.sectors.tracing.BeamConfig;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.latency.LatencyHandler;
import dev.lumas.events.utility.latency.hitbox.Trail;
import dev.lumas.events.utility.latency.hitbox.TrailRecorder;
import dev.lumas.events.utility.latency.hitbox.TrailSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Tracing {

    private final Supplier<Collection<LivingEntity>> targetSupplier;

    private final TrailRecorder recorder;
    private final LatencyHandler latency;

    public Tracing(Supplier<Collection<LivingEntity>> targetSupplier, TrailRecorder recorder, LatencyHandler latency) {
        this.targetSupplier = targetSupplier;
        this.recorder = recorder;
        this.latency = latency;
    }

    // Where this entity was at <at>, or null if nothing has been recorded
    // for it and we are on the wrong thread to look its hitbox up live
    @Nullable
    private Target targetAt(LivingEntity entity, long at, double expandBy) {
        Trail trail = recorder.getEntityTrail(entity.getUniqueId());
        TrailSnapshot snapshot = trail == null ? null : trail.at(at);
        if (snapshot != null) return Target.ofHumanoid(entity, expandBy, snapshot);

        return Bukkit.isOwnedByCurrentRegion(entity)
                ? Target.ofHumanoid(entity, expandBy)
                : null;
    }

    public CompletableFuture<Collection<BeamTraceResult>> traceBeam(Player shooter, Predicate<LivingEntity> filter, BeamConfig beamConfig) {
        long firedAt = latency.timeOf(shooter);

        Location eye = shooter.getEyeLocation();
        Vector direction = eye.getDirection().normalize();

        return advanceBeam(
                eye,
                direction,
                0.0, beamConfig
        ).thenApply(beamLength -> toBeamResult(targetSupplier.get()
                        .stream()
                        .filter(filter)
                        .filter(entity -> !shooter.equals(entity))
                        .map(target -> targetAt(target, firedAt, beamConfig.getHitMargin()))
                        .toList(),
                eye,
                direction,
                beamLength,
                beamConfig)
        );
    }

    private Collection<BeamTraceResult> toBeamResult(Collection<Target> targets, Location eye, Vector direction, double beamLength, BeamConfig config) {
        List<BeamTraceResult> hits = new ArrayList<>();

        for (Target target : targets) {
            BoundingBox hitbox = target.hitbox();
            double maxReach = beamLength + hitbox.getHeight();
            if (eye.toVector().distanceSquared(hitbox.getCenter()) > maxReach * maxReach) {
                continue;
            }

            if (!beamTouches(hitbox, eye, direction, beamLength)) {
                continue;
            }

            for (Map.Entry<String, BoundingBox> entry : target.parts().entrySet()) {
                if (beamTouches(entry.getValue(), eye, direction, beamLength)) {
                    hits.add(new BeamTraceResult(target.entity(), entry.getKey()));
                }
            }
        }
        return hits;
    }

    private static boolean beamTouches(BoundingBox box, Location eye, Vector direction, double maxDistance) {
        Vector origin = eye.toVector();
        return box.contains(origin) || box.rayTrace(origin, direction, maxDistance) != null;
    }

    private CompletableFuture<Double> advanceBeam(Location eye, Vector direction,
                                                  double startDistance, BeamConfig beamConfig) {
        double maxRange = beamConfig.getRange();
        World world = eye.getWorld();
        Vector origin = eye.toVector();
        int step = 0;
        int lastX = Integer.MIN_VALUE;
        int lastY = Integer.MIN_VALUE;
        int lastZ = Integer.MIN_VALUE;
        double drawnTo = startDistance;

        for (double distance = startDistance; distance <= maxRange; distance += beamConfig.getStep(), step++) {
            double x = eye.getX() + (direction.getX() * distance);
            double y = eye.getY() + (direction.getY() * distance);
            double z = eye.getZ() + (direction.getZ() * distance);

            int blockX = (int) Math.floor(x);
            int blockY = (int) Math.floor(y);
            int blockZ = (int) Math.floor(z);

            if (!Bukkit.isOwnedByCurrentRegion(world, blockX >> 4, blockZ >> 4)) {
                double resumeAt = distance;
                CompletableFuture<Double> future = new CompletableFuture<>();
                Executors.sync(world, blockX >> 4, blockZ >> 4,
                        () -> advanceBeam(eye, direction, resumeAt, beamConfig)
                                .thenAccept(future::complete)
                );
                return future;
            }

            // Steps can land in the same block more than once
            boolean sameBlock = blockX == lastX && blockY == lastY && blockZ == lastZ;
            lastX = blockX;
            lastY = blockY;
            lastZ = blockZ;

            double stoppedAt = sameBlock
                    ? -1
                    : solidHitDistance(world.getBlockAt(blockX, blockY, blockZ), origin, direction, maxRange);

            if (stoppedAt >= 0) {
                // Fences and walls collide above their own block
                double impactAt = Math.max(stoppedAt, drawnTo);
                Location impact = new Location(world,
                        eye.getX() + (direction.getX() * impactAt),
                        eye.getY() + (direction.getY() * impactAt),
                        eye.getZ() + (direction.getZ() * impactAt));

                beamConfig.getHitParticles().forEach(particle -> particle.showAt(impact));
                beamConfig.getHitSounds().forEach(sound -> sound.playAt(impact));
                return CompletableFuture.completedFuture(impactAt);
            }

            Location beamLocation = new Location(world, x, y, z);
            beamConfig.getBeamParticles().forEach(particle -> particle.showAt(beamLocation));
            beamConfig.getBeamSounds().forEach(sound -> sound.playAt(beamLocation));
            drawnTo = distance;
        }

        return CompletableFuture.completedFuture(maxRange);
    }

    private static boolean couldStopShots(Block block) {
        return !block.isPassable() && block.getType() != Material.BARRIER;
    }

    // How far along the ray it first meets something solid inside this block, or -1 if it doesn't
    private static double solidHitDistance(Block block, Vector origin, Vector direction, double maxDistance) {
        if (!couldStopShots(block)) return -1;

        BoundingBox bounds = block.getBoundingBox();
        if (bounds.getVolume() <= 0) return -1;
        if (bounds.contains(origin)) return 0; // Fired from inside the block itself

        // <direction> is normalized, so this is the distance along the ray
        RayTraceResult hit = bounds.rayTrace(origin, direction, maxDistance);
        return hit == null ? -1 : hit.getHitPosition().distance(origin);
    }

}
