package dev.lumas.events.games.logic.manor2.artifact;

import dev.lumas.events.EventMain;
import dev.lumas.events.games.logic.manor2.Manor2;
import dev.lumas.events.games.logic.manor2.roles.GhostHunter;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.model.WorldTiedBoundingBox;
import dev.lumas.events.utility.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ArtifactManager {
    private static final int POS_LOOK_TRIES = 10;
    private static final int ARTIFACT_SPAWN_DELAY = 30 * 20;
    private static final int ARTIFACT_RESPAWN_DELAY = 40 * 20;
    private static final int ARTIFACT_SPAWN_MINIMUM_DISTANCE_AWAY = 10;
    private static final int MAX_REGION_FAILS = 20;

    private long time = 0;
    private final List<WorldTiedBoundingBox> spawningRegions;
    private final List<ArtifactType> artifactTypes;
    private final Map<WorldTiedBoundingBox, Integer> regionFails = new ConcurrentHashMap<>();
    private final Map<WorldTiedBoundingBox, Long> occupiedRegions = new ConcurrentHashMap<>();
    private final Map<WorldTiedBoundingBox, Long> regionDelay = new ConcurrentHashMap<>();
    private final Map<Location3i, ArtifactType> artifacts = new ConcurrentHashMap<>();
    private final Map<Location3i, ArtifactEntity> placedArtifacts = new ConcurrentHashMap<>();
    private final Manor2 manor2;

    public ArtifactManager(Manor2 manor2) {
        this.manor2 = manor2;
        this.spawningRegions = compileRegions(manor2.getDefinition().getRegion().toWorldTiedBoundingBox());
        this.artifactTypes = manor2.getDefinition().getArtifacts();
    }

    public void tick() {
        regionFails.entrySet().stream()
                .filter(entry -> entry.getValue() > MAX_REGION_FAILS)
                .map(Map.Entry::getKey)
                .forEach(spawningRegions::remove);
        for (WorldTiedBoundingBox spawningRegion : spawningRegions) {
            if (regionDelay.get(spawningRegion) instanceof Long aLong && aLong < time) {
                continue;
            }
            if (!occupiedRegions.containsKey(spawningRegion)) {
                final long finalTimeStamp = time + ARTIFACT_RESPAWN_DELAY;
                occupiedRegions.put(spawningRegion, finalTimeStamp);
                findPos(spawningRegion)
                        .thenAccept(locationOptional -> locationOptional.ifPresentOrElse(location ->
                                        artifacts.put(Location3i.from(location), Util.getRandom(artifactTypes)),
                                () -> occupiedRegions.remove(spawningRegion, finalTimeStamp))
                        );
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

    public void reveal(Location location) {
        Location3i at = Location3i.from(location);
        if (artifacts.get(at) instanceof ArtifactType artifactType) {
            if (!(placedArtifacts.get(at) instanceof ArtifactEntity entity)) {
                Bukkit.broadcastMessage("Revealing artifact at %s".formatted(location));
                placedArtifacts.put(at, ArtifactEntity.spawn(at.toLocation().add(0.5, 0, 0.5), artifactType, time));
            } else {
                placedArtifacts.put(at, entity.withTimeStamp(time));
            }
        }
    }

    private List<WorldTiedBoundingBox> compileRegions(WorldTiedBoundingBox all) {
        double xWidth = all.getWidthX();
        double zWidth = all.getWidthZ();
        double height = all.getHeight();
        double regionHeight = manor2.getDefinition().getRegionHeight();
        double regionWidth = manor2.getDefinition().getAverageRegionWidth();
        List<WorldTiedBoundingBox> output = new ArrayList<>();
        for (double x = 0; x < xWidth; x += regionWidth) {
            for (double y = 0; y < height; y += regionHeight) {
                for (double z = 0; z < zWidth; z += regionWidth) {
                    output.add(
                            new WorldTiedBoundingBox(all.getWorld(),
                                    all.getMinX() + x,
                                    all.getMinY() + y,
                                    all.getMinZ() + z,
                                    Math.min(all.getMinX() + x + regionWidth, all.getMaxX()),
                                    Math.min(all.getMinY() + y + regionHeight, all.getMaxY()),
                                    Math.min(all.getMinZ() + z + regionWidth, all.getMaxZ())
                            )
                    );
                }
            }
        }
        return output;
    }

    private CompletableFuture<Optional<Location>> findPos(WorldTiedBoundingBox spawningRegion) {
        CompletableFuture<Optional<Location>> future = new CompletableFuture<>();
        WorldTiedBoundingBox lookFor = (WorldTiedBoundingBox) spawningRegion.clone().expand(ARTIFACT_SPAWN_MINIMUM_DISTANCE_AWAY);
        Bukkit.getRegionScheduler().run(EventMain.getInstance(), spawningRegion.getCenterLocation(), ignored -> {
            // Avoid filling a room with an artifact if it contains a player
            if (manor2.getRoleMap().getMatching(GhostHunter.class).stream()
                    .map(GhostHunter::getEventPlayer)
                    .map(EventPlayer::getPlayer)
                    .filter(Objects::nonNull)
                    .map(Player::getLocation)
                    .anyMatch(lookFor::contains)
            ) {
                future.complete(Optional.empty());
                regionFails.merge(spawningRegion, 1, Integer::sum);
                return;
            }
            Location attempt = spawningRegion.getRandomLocation();
            int attemptsLeft = POS_LOOK_TRIES;
            while (attempt.getBlock().isSolid()) {
                if (attemptsLeft-- <= 0) {
                    Bukkit.broadcastMessage("Gave up");
                    regionFails.merge(spawningRegion, 1, Integer::sum);
                    future.complete(Optional.empty());
                    return;
                }
                attempt = spawningRegion.getRandomLocation();
            }
            while (!attempt.getBlock().getRelative(BlockFace.DOWN).isSolid()) {
                attempt.subtract(0, 1, 0);
            }
            Location finalLocation = attempt.toCenterLocation();
            if (spawningRegion.contains(finalLocation)) {
                Bukkit.broadcastMessage("Found artifact pos at %s".formatted(attempt.toCenterLocation()));
                regionFails.put(spawningRegion, 0);
                future.complete(Optional.of(finalLocation));
            } else {
                Bukkit.broadcastMessage("Invalid pos, outside region");
                regionFails.merge(spawningRegion, 1, Integer::sum);
                future.complete(Optional.empty());
            }
        });
        return future;
    }

    public boolean pickup(@NotNull Location location) {
        Location3i at = Location3i.from(location);
        artifacts.remove(at);
        ArtifactEntity entity = placedArtifacts.remove(at);
        if (entity != null) {
            entity.remove();
        }
        for (WorldTiedBoundingBox region : occupiedRegions.keySet()) {
            if (region.contains(location)) {
                occupiedRegions.remove(region);
                regionDelay.put(region, time + ARTIFACT_SPAWN_DELAY);
                break;
            }
        }
        return entity != null;
    }

    private record Location3i(World world, int x, int y, int z) {

        public static Location3i from(Location location) {
            return new Location3i(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }

        public Location toLocation() {
            return new Location(world, x, y, z);
        }
    }
}
