package dev.lumas.events.games.logic.manor2.artifact;

import dev.lumas.events.EventMain;
import dev.lumas.events.games.logic.manor2.Manor2;
import dev.lumas.events.games.logic.manor2.roles.GhostHunter;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.model.WorldTiedBoundingBox;
import dev.lumas.events.utility.Util;
import dev.lumas.events.utility.latency.tracing.Tracing;
import dev.lumas.events.utility.vector.Location3i;
import dev.lumas.events.utility.vector.Vector2i;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ArtifactManager {
    private static final int ARTIFACT_SPAWN_DELAY = 30 * 20;
    private static final int ARTIFACT_RESPAWN_DELAY = 40 * 20;
    private static final double ARTIFACT_TRACE_STEP = 0.5;
    private static final int MAX_REGION_FAILS = 20;
    private static final int MIN_RESPAWN_PLAYER_PROXIMITY = 10;
    private static final String REVEAL_ARTIFACT_MESSAGE = "<gray>Click on the artifact to get it";
    private static final double LIGHT_ANGLE_REVEAL_MULTIPLIER = 0.5;

    private long time = 0;
    private List<ArtifactRegion> spawningRegions = new ArrayList<>();
    private final List<ArtifactType> artifactTypes;
    private final Map<ArtifactRegion, Integer> regionFails = new ConcurrentHashMap<>();
    private final Map<ArtifactRegion, Long> occupiedRegions = new ConcurrentHashMap<>();
    private final Map<ArtifactRegion, Long> regionDelay = new ConcurrentHashMap<>();
    private final Map<Location3i, ArtifactType> artifacts = new ConcurrentHashMap<>();
    private final Map<Location3i, ArtifactEntity> placedArtifacts = new ConcurrentHashMap<>();
    private final Manor2 manor2;
    private final AtomicInteger artifactPickedUpCount = new AtomicInteger(0);

    public ArtifactManager(Manor2 manor2) {
        this.manor2 = manor2;
        Bukkit.getRegionScheduler().run(EventMain.getInstance(), manor2.getDefinition().getRegion().toWorldTiedBoundingBox().getCenterLocation(), ignored -> {
            this.spawningRegions.addAll(compileRegions(manor2.getDefinition().getRegion().toWorldTiedBoundingBox()));
        });
        this.artifactTypes = manor2.getDefinition().getArtifacts();
    }

    public void tick() {
        regionFails.entrySet().stream()
            .filter(entry -> entry.getValue() > MAX_REGION_FAILS)
            .map(Map.Entry::getKey)
            .forEach(spawningRegions::remove);
        for (ArtifactRegion spawningRegion : spawningRegions) {
            if (regionDelay.get(spawningRegion) instanceof Long aLong && aLong < time) {
                continue;
            }
            WorldTiedBoundingBox expanded = (WorldTiedBoundingBox) spawningRegion.boundingBox().expand(MIN_RESPAWN_PLAYER_PROXIMITY);
            if (manor2.getRoleMap().getMatching(GhostHunter.class).stream()
                .map(GhostHunter::getEventPlayer)
                .map(EventPlayer::getPlayer)
                .filter(Objects::nonNull)
                .map(Player::getLocation)
                .anyMatch(expanded::contains)
            ) {
                continue;
            }
            if (!occupiedRegions.containsKey(spawningRegion)) {
                final long finalTimeStamp = time + ARTIFACT_RESPAWN_DELAY;
                occupiedRegions.put(spawningRegion, finalTimeStamp);
                Location3i pos = randomPos(spawningRegion);
                artifacts.put(pos, Util.getRandom(artifactTypes));
            }
        }
        for (Map.Entry<Location3i, ArtifactEntity> entityEntry : placedArtifacts.entrySet()) {
            entityEntry.getValue().ensureValid(time)
                .thenAccept(valid -> {
                    if (!valid) {
                        placedArtifacts.remove(entityEntry.getKey(), entityEntry.getValue());
                    }
                });
            entityEntry.getValue().rotate(time);
        }
        time++;
    }

    private List<ArtifactRegion> compileRegions(WorldTiedBoundingBox all) {
        double xWidth = all.getWidthX();
        double zWidth = all.getWidthZ();
        double regionWidth = manor2.getDefinition().getAverageRegionWidth();
        List<ArtifactRegion> output = new ArrayList<>();
        for (double x = 0; x < xWidth; x += regionWidth) {
            for (double z = 0; z < zWidth; z += regionWidth) {
                output.addAll(ArtifactRegion.within(
                    all.getWorld(),
                    (int) Math.floor(x + all.getMinX()),
                    (int) Math.floor(all.getMinY()),
                    (int) Math.floor(z + all.getMinZ()),
                    (int) Math.floor(Math.min(x + regionWidth + all.getMinX(), all.getMaxX())),
                    (int) Math.floor(all.getMaxY()),
                    (int) Math.floor(Math.min(z + regionWidth + all.getMinZ(), all.getMaxZ())))
                );
            }
        }
        return output;
    }

    private Location3i randomPos(ArtifactRegion spawningRegion) {
        Vector2i xzPos = Util.getRandom(spawningRegion.floorMap().keySet());
        int yPos = spawningRegion.floorMap().get(xzPos);
        return new Location3i(spawningRegion.boundingBox().getWorld(), xzPos.x(), yPos, xzPos.z());
    }

    public boolean pickup(@NotNull Location location) {
        Location3i at = Location3i.from(location);
        artifacts.remove(at);
        ArtifactEntity entity = placedArtifacts.remove(at);
        if (entity != null) {
            entity.remove();
            Util.getRandom(manor2.getDefinition().getArtifactPickupSounds())
                .playAt(location);
            artifactPickedUpCount.incrementAndGet();
        }
        for (ArtifactRegion region : occupiedRegions.keySet()) {
            if (region.boundingBox().contains(location)) {
                occupiedRegions.remove(region);
                regionDelay.put(region, time + ARTIFACT_SPAWN_DELAY);
                break;
            }
        }
        return entity != null;
    }

    public void tryReveal(Location eyeLocation, double coneDegrees, double maxSearch, Audience audience) {
        Location3i eye3i = Location3i.from(eyeLocation);
        Vector eyeDirection = eyeLocation.getDirection();
        double minShading = Math.cos(Math.toRadians(coneDegrees)) * LIGHT_ANGLE_REVEAL_MULTIPLIER;
        double maxSearchSquared = maxSearch * maxSearch;
        for (Map.Entry<Location3i, ArtifactType> entry : artifacts.entrySet()) {
            Location3i artifactPos3i = entry.getKey();
            if (Math.abs(eye3i.x() - artifactPos3i.x()) + Math.abs(eye3i.y() - artifactPos3i.y()) + Math.abs(eye3i.z() - artifactPos3i.z()) > maxSearch + 1) {
                continue;
            }
            Location artifactPos = artifactPos3i.toLocation();
            Vector diff = artifactPos.toVector().subtract(eyeLocation.toVector());
            double diffLengthSquared = diff.lengthSquared();
            Vector diffNormalized = diff.clone().normalize();
            if (diffNormalized.dot(eyeDirection) < minShading || maxSearchSquared < diffLengthSquared ||
                !Tracing.hasClearShot(eyeLocation, diffNormalized, Math.sqrt(diffLengthSquared), ARTIFACT_TRACE_STEP)
            ) {
                continue;
            }
            if (!(placedArtifacts.get(artifactPos3i) instanceof ArtifactEntity entity)) {
                audience.sendActionBar(
                    Util.color(REVEAL_ARTIFACT_MESSAGE)
                );
                Util.getRandom(manor2.getDefinition().getArtifactRevealSounds())
                    .playAt(artifactPos);
                placedArtifacts.put(artifactPos3i, ArtifactEntity.spawn(artifactPos3i.toLocation().add(0.5, 0, 0.5), entry.getValue(), time));
            } else {
                placedArtifacts.put(artifactPos3i, entity.withTimeStamp(time));
            }
        }
    }

    public int artifactsPickedUp() {
        return artifactPickedUpCount.get();
    }
}
