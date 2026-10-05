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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ArtifactManager {
    private static final int POS_LOOK_TRIES = 10;
    private static final int ARTIFACT_SPAWN_DELAY = 30 * 20;
    private static final int ARTIFACT_SHOW_DURATION = 20;
    private long time = 0;

    private final List<WorldTiedBoundingBox> spawningRegions;
    private final List<ArtifactType> artifactTypes;
    private final Set<WorldTiedBoundingBox> occupiedRegions = ConcurrentHashMap.newKeySet();
    private final Map<WorldTiedBoundingBox, Long> regionDelay = new ConcurrentHashMap<>();
    private final Map<Location3i, ArtifactType> artifacts = new ConcurrentHashMap<>();
    private final Map<Location3i, ArtifactEntity> placedArtifacts = new ConcurrentHashMap<>();
    private final Manor2 manor2;

    public ArtifactManager(Manor2 manor2) {
        this.spawningRegions = manor2.getDefinition().getArtifactSpawningRegions();
        this.artifactTypes = manor2.getDefinition().getArtifacts();
        this.manor2 = manor2;
    }

    public void tick() {
        for (WorldTiedBoundingBox spawningRegion : spawningRegions) {
            if (regionDelay.get(spawningRegion) instanceof Long aLong && aLong < time) {
                continue;
            }
            if (!occupiedRegions.contains(spawningRegion)) {
                occupiedRegions.add(spawningRegion);
                findPos(spawningRegion)
                        .thenAccept(locationOptional -> locationOptional.ifPresentOrElse(location ->
                                        artifacts.put(Location3i.from(location), Util.getRandom(artifactTypes)),
                                () -> occupiedRegions.remove(spawningRegion))
                        );
            }
        }
        List<Map.Entry<Location3i, ArtifactEntity>> toRemove = new ArrayList<>();
        for (Map.Entry<Location3i, ArtifactEntity> entityEntry : placedArtifacts.entrySet()) {
            if (entityEntry.getValue().isDead()) {
                entityEntry.getValue().remove();
                toRemove.add(entityEntry);
            }
            if (entityEntry.getValue().spawnTimestamp() + ARTIFACT_SHOW_DURATION < time) {
                entityEntry.getValue().remove();
                toRemove.add(entityEntry);
            }
        }
        toRemove.forEach(entry -> placedArtifacts.remove(entry.getKey(), entry.getValue()));
        time++;
    }

    public void reveal(Location location) {
        Location3i at = Location3i.from(location);
        if (artifacts.get(at) instanceof ArtifactType artifactType) {
            if (!(placedArtifacts.get(at) instanceof ArtifactEntity entity)) {
                placedArtifacts.put(at, ArtifactEntity.spawn(at.toLocation().add(0.5, 0, 0.5), artifactType, time));
            } else {
                placedArtifacts.put(at, entity.withTimeStamp(time));
            }
        }
    }

    private CompletableFuture<Optional<Location>> findPos(WorldTiedBoundingBox spawningRegion) {
        CompletableFuture<Optional<Location>> future = new CompletableFuture<>();
        WorldTiedBoundingBox lookFor = (WorldTiedBoundingBox) spawningRegion.expand(20);
        Bukkit.getScheduler().runTask(EventMain.getInstance(), () -> {
            // Avoid filling a room with an artifact if it contains a player
            if (manor2.getRoleMap().getMatching(GhostHunter.class).stream()
                    .map(GhostHunter::getEventPlayer)
                    .map(EventPlayer::getPlayer)
                    .filter(Objects::nonNull)
                    .map(Player::getLocation)
                    .anyMatch(lookFor::contains)
            ) {
                future.complete(Optional.empty());
                return;
            }
            Location attempt = spawningRegion.getRandomLocation();
            int attemptsLeft = POS_LOOK_TRIES;
            while (attempt.getBlock().isSolid()) {
                if (attemptsLeft-- <= 0) {
                    future.complete(Optional.empty());
                    return;
                }
                attempt = spawningRegion.getRandomLocation();
            }
            while (!attempt.getBlock().getRelative(BlockFace.DOWN).isSolid()) {
                attempt.subtract(0, 1, 0);
            }
            future.complete(Optional.of(attempt.toCenterLocation()));
        });
        return future;
    }

    public void pickup(@NotNull Location location) {
        Location3i at = Location3i.from(location);
        artifacts.remove(at);
        ArtifactEntity entity = placedArtifacts.remove(at);
        if (entity != null) {
            entity.remove();
        }
        for (WorldTiedBoundingBox region : occupiedRegions) {
            if (region.contains(location)) {
                occupiedRegions.remove(region);
                regionDelay.put(region, time + ARTIFACT_SPAWN_DELAY);
                break;
            }
        }
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
