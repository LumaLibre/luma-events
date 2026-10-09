package dev.lumas.events.utility.latency.tracing;

import dev.lumas.events.configurable.sectors.tracing.BeamConfig;
import dev.lumas.events.configurable.sectors.tracing.ConeConfig;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.latency.LatencyHandler;
import dev.lumas.events.utility.latency.hitbox.Hitbox;
import dev.lumas.events.utility.latency.hitbox.Trail;
import dev.lumas.events.utility.latency.hitbox.TrailRecorder;
import dev.lumas.events.utility.latency.hitbox.TrailSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
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


    public CompletableFuture<Collection<BeamTraceResult>> traceBeam(TraceShooter shooter, Predicate<LivingEntity> filter, BeamConfig beamConfig) {
        long firedAt = shooter.nanoTime(latency);
        Location eye = shooter.shootPos();
        Vector direction = eye.getDirection().normalize();

        return advanceBeam(
            eye,
            direction,
            0.0, beamConfig
        ).thenApply(beamLength -> toBeamResult(targetSupplier.get()
                .stream()
                .filter(filter)
                .filter(entity -> !shooter.equals(entity))
                .map(target -> targetAt(target, firedAt))
                .toList(),
            eye,
            direction,
            beamLength,
            beamConfig)
        );
    }

    public Collection<ConeTraceResult> traceCone(TraceShooter shooter, Predicate<LivingEntity> filter,
                                                 Comparator<ConeTraceResult> comparator, ConeConfig coneConfig) {
        Location muzzle = shooter.shootPos();
        Vector direction = muzzle.getDirection().normalize();
        double range = Math.max(0.5, coneConfig.getRange());

        double halfAngle = Math.toRadians(Math.max(1.0, coneConfig.getConeDegrees()) / 2.0);
        double tanHalfAngle = Math.tan(halfAngle);
        double cosHalfAngle = Math.cos(halfAngle);
        long shootTime = shooter.nanoTime(latency);
        Vector apex = muzzle.toVector();

        List<ConeTraceResult> output = new ArrayList<>();
        for (Target target : targetSupplier.get().stream()
            .filter(filter)
            .filter(shooter::notShooter)
            .map(target -> targetAt(target, shootTime))
            .toList()
        ) {
            Hitbox hitbox = target.hitbox();
            if (hitbox.shape().isEmpty()) {
                continue;
            }
            BoundingBox boundingBox = hitbox.shape().getFirst().boundingBox();
            Collection<ConeTraceResult> matchingTraces = coneHit(apex, direction, range, tanHalfAngle, cosHalfAngle, hitbox, target.entity());
            if (matchingTraces.isEmpty()) {
                continue;
            }
            // Occlusion has to be checked towards the target, not down the middle of the cone
            Vector toTarget = boundingBox.getCenter().subtract(apex);
            double length = toTarget.length();
            if (length > 1.0E-4 && !hasClearShot(muzzle, toTarget.multiply(1.0 / length), length, coneConfig.getTraceStep())) {
                continue;
            }
            matchingTraces.stream()
                .max(comparator)
                .ifPresent(output::add);
        }
        return output;
    }

    public Collection<ProximityTraceResult> traceProximity(TraceShooter shooter, Predicate<LivingEntity> filter, double maxProximity) {
        Vector center = shooter.shootPos().toVector();
        double maxProximitySquared = maxProximity * maxProximity;
        List<ProximityTraceResult> output = new ArrayList<>();
        World targetWorld = shooter.shootPos().getWorld();
        long shootTime = shooter.nanoTime(latency);

        for (Target target : targetSupplier.get().stream()
            .filter(filter)
            .filter(shooter::notShooter)
            .map(target -> targetAt(target, shootTime))
            .toList()
        ) {
            for (Vector sample : target.hitbox().sampleOffsets()) {
                double proximitySquared = target.hitbox().wholeBounding().getCenter().clone()
                    .add(sample)
                    .distanceSquared(center);
                if (targetWorld != target.entity().getWorld() || proximitySquared > maxProximitySquared) {
                    continue;
                }
                output.add(new ProximityTraceResult(target.entity(), Math.sqrt(proximitySquared)));
            }
        }
        return output;
    }

    private static Collection<ConeTraceResult> coneHit(Vector apex, Vector axis, double range, double tanHalfAngle, double cosHalfAngle,
                                                       Hitbox hitbox, LivingEntity target) {
        BoundingBox approximateBounding = hitbox.wholeBounding();
        double lookForRadius = Math.max(approximateBounding.getHeight() / 2d, Math.max(approximateBounding.getWidthX(), approximateBounding.getWidthZ()) / 2d);
        double lookForRadiusSquared = lookForRadius * lookForRadius;
        // Nothing beyond this can reach the cone
        double maxReach = range + (approximateBounding.getHeight() / 2d) + lookForRadius;
        Vector centerPos = approximateBounding.getCenter();
        if (apex.distanceSquared(centerPos) > maxReach * maxReach) {
            return List.of();
        }

        List<ConeTraceResult> matches = new ArrayList<>();
        for (Vector sampleOffset : hitbox.sampleOffsets()) {
            Vector delta = centerPos.clone()
                .add(sampleOffset)
                .subtract(apex);

            double distanceSquared = delta.lengthSquared();
            if (distanceSquared <= lookForRadiusSquared) {
                return List.of(new ConeTraceResult(target, 0, 0)); // muzzle is inside target
            }
            double alongAxis = axis.dot(delta);
            if (alongAxis < 0 || alongAxis - lookForRadius > range) {
                continue;
            }

            Vector projectedAxis = axis.clone().multiply(alongAxis);
            Vector remainder = delta.clone().subtract(projectedAxis); // Orthogonal to axis vector
            double maxAllowedRemainderLength = (alongAxis * tanHalfAngle) + (lookForRadius / cosHalfAngle);

            // The cone widens with distance, and a sphere of <radius> widens what counts as touching it
            double remainderLengthSquared = remainder.lengthSquared();
            if (remainderLengthSquared > maxAllowedRemainderLength * maxAllowedRemainderLength) {
                continue;
            }

            double toSurface = Math.max(0, Math.sqrt(distanceSquared) - lookForRadius);

            if (toSurface <= range) {
                matches.add(new ConeTraceResult(target, alongAxis, Math.sqrt(remainderLengthSquared)));
            }
        }
        return matches;
    }

    // Where this entity was at <at>, or null if nothing has been recorded
    // for it and we are on the wrong thread to look its hitbox up live
    @Nullable
    private Target targetAt(LivingEntity entity, long at) {
        Trail trail = recorder.getEntityTrail(entity.getUniqueId());
        TrailSnapshot snapshot = trail == null ? null : trail.at(at);
        if (snapshot != null) {
            return new Target(entity, snapshot.hitbox());
        }
        return recorder.targetNow(entity);
    }

    private Collection<BeamTraceResult> toBeamResult(Collection<Target> targets, Location eye, Vector direction, double beamLength, BeamConfig config) {
        List<BeamTraceResult> hits = new ArrayList<>();

        for (Target target : targets) {
            Hitbox hitbox = target.hitbox();
            BoundingBox approximateBounding = hitbox.wholeBounding().clone().expand(config.getHitMargin());
            double maxReach = beamLength + approximateBounding.getHeight();
            if (eye.toVector().distanceSquared(approximateBounding.getCenter()) > maxReach * maxReach) {
                continue;
            }

            if (!beamTouches(
                approximateBounding,
                eye,
                direction,
                beamLength
            )) {
                continue;
            }

            for (Hitbox.Section section : hitbox.shape()) {
                if (approximateBounding.equals(section.boundingBox()) || beamTouches(section.boundingBox(), eye, direction, beamLength)) {
                    hits.add(new BeamTraceResult(target.entity(), section.part()));
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

    // Cheap-ish line of sight check to make sure shots don't travel through walls
    public static boolean hasClearShot(Location from, Vector direction, double distance, double traceStep) {
        World world = from.getWorld();
        Vector origin = from.toVector();
        int lastX = Integer.MIN_VALUE;
        int lastY = Integer.MIN_VALUE;
        int lastZ = Integer.MIN_VALUE;

        for (double travelled = traceStep; travelled < distance; travelled += traceStep) {
            int blockX = (int) Math.floor(from.getX() + (direction.getX() * travelled));
            int blockY = (int) Math.floor(from.getY() + (direction.getY() * travelled));
            int blockZ = (int) Math.floor(from.getZ() + (direction.getZ() * travelled));

            if (blockX == lastX && blockY == lastY && blockZ == lastZ) continue; // Same block as the last step
            lastX = blockX;
            lastY = blockY;
            lastZ = blockZ;

            if (!Bukkit.isOwnedByCurrentRegion(world, blockX >> 4, blockZ >> 4)) {
                return true;
            }
            if (solidHitDistance(world.getBlockAt(blockX, blockY, blockZ), origin, direction, distance) >= 0) {
                return false;
            }
        }
        return true;
    }

}
