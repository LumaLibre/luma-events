package dev.lumas.events.games.logic.manor2.artifact;

import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.sectors.tracing.ConeConfig;
import dev.lumas.events.games.logic.manor2.Manor2;
import dev.lumas.events.games.logic.manor2.roles.Artifact;
import dev.lumas.events.games.logic.manor2.roles.GhostHunter;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.model.WorldTiedBoundingBox;
import dev.lumas.events.utility.Util;
import dev.lumas.events.utility.latency.tracing.ConeTraceResult;
import dev.lumas.events.utility.latency.tracing.TraceShooter;
import dev.lumas.events.utility.latency.tracing.Tracing;
import dev.lumas.events.utility.vector.Location3i;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.ValidationResult;
import net.objecthunter.exp4j.function.Function;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ArtifactManager {
    private static final int ARTIFACT_SPAWN_DELAY = 30 * 20;
    private static final int ARTIFACT_RESPAWN_DELAY = 40 * 20;
    private static final double ARTIFACT_TRACE_STEP = 0.5;
    private static final int MIN_RESPAWN_PLAYER_PROXIMITY = 10;
    private static final String REVEAL_ARTIFACT_MESSAGE = "<gray>Click on the artifact to get it";
    private static final double LIGHT_ANGLE_REVEAL_MULTIPLIER = 0.8;

    private long time = 0;
    private final List<ArtifactRegion> spawningRegions = new ArrayList<>();
    private final List<ArtifactType> artifactTypes;
    private final List<ArtifactType> playerArtifactTypes;
    private final Map<ArtifactRegion, Long> occupiedRegions = new ConcurrentHashMap<>();
    private final Map<ArtifactRegion, Long> regionDelay = new ConcurrentHashMap<>();
    private final Map<Location3i, ArtifactType> artifacts = new ConcurrentHashMap<>();
    private final Map<Location3i, ArtifactEntity> placedArtifacts = new ConcurrentHashMap<>();
    private final Map<Location3i, ArtifactRegion> pickedUpPos = new ConcurrentHashMap<>();
    private final Map<UUID, ArtifactType> playerArtifacts = new ConcurrentHashMap<>();
    private final Manor2 manor2;
    private final AtomicInteger artifactPickedUpCount = new AtomicInteger(0);
    private boolean fillFactorFunctionIssueSent = false;

    public ArtifactManager(Manor2 manor2) {
        this.manor2 = manor2;
        Bukkit.getRegionScheduler().run(EventMain.getInstance(), manor2.getDefinition().getRegion().toWorldTiedBoundingBox().getCenterLocation(), ignored -> {
            this.spawningRegions.addAll(compileRegions(manor2.getDefinition().getRegion().toWorldTiedBoundingBox()));
        });
        this.artifactTypes = manor2.getDefinition().getArtifacts();
        this.playerArtifactTypes = manor2.getDefinition().getPlayerArtifacts();
    }

    public void tick() {
        double fillFactor = fillFactor();
        int remainingRegionsToPopulate = (int) (spawningRegions.size() * fillFactor - artifacts.size());
        if (remainingRegionsToPopulate > 0) {
            spawnInRegions(remainingRegionsToPopulate);
        }
        for (Map.Entry<Location3i, ArtifactEntity> entityEntry : placedArtifacts.entrySet()) {
            entityEntry.getValue().ensureValid(time)
                .thenAccept(valid -> {
                    if (!valid) {
                        Artifact artifactRole = entityEntry.getValue().boundRole();
                        if (artifactRole != null) {
                            artifactRole.unfreeze();
                        }
                        placedArtifacts.remove(entityEntry.getKey(), entityEntry.getValue());
                    }
                });
            entityEntry.getValue().rotate(time);
        }
        time++;
    }

    public void stop() {
        placedArtifacts.values().forEach(ArtifactEntity::remove);
    }

    public void spawnInRegions(int remainingRegionsToPopulate) {
        List<ArtifactRegion> regions = spawningRegions.stream()
            .filter(spawningRegion -> !(regionDelay.get(spawningRegion) instanceof Long aLong) || aLong >= time)
            .filter(this::noNearbyPlayer)
            .filter(spawningRegion -> !occupiedRegions.containsKey(spawningRegion))
            .collect(Collectors.toList());
        Collections.shuffle(regions);
        for (ArtifactRegion spawningRegion : regions) {
            if (remainingRegionsToPopulate-- <= 0) {
                return;
            }
            final long finalTimeStamp = time + ARTIFACT_RESPAWN_DELAY;
            occupiedRegions.put(spawningRegion, finalTimeStamp);
            Location3i pos = spawningRegion.randomPos();
            artifacts.put(pos, Util.getRandom(artifactTypes));
        }
    }

    public ArtifactPickupResult pickup(@NotNull Location location) {
        Location3i at = Location3i.from(location);
        artifacts.remove(at);
        ArtifactEntity entity = placedArtifacts.remove(at);
        if (entity == null) {
            return ArtifactPickupResult.NOT_FOUND;
        }

        for (ArtifactRegion region : occupiedRegions.keySet()) {
            if (region.boundingBox().contains(location)) {
                occupiedRegions.remove(region);
                regionDelay.put(region, time + ARTIFACT_SPAWN_DELAY);
                pickedUpPos.put(at, region);
                break;
            }
        }
        entity.remove();
        Util.getRandom(manor2.getDefinition().getArtifactPickupSounds())
            .playAt(location);
        artifactPickedUpCount.incrementAndGet();
        if (entity.boundRole() != null) {
            entity.boundRole().die();
            playerArtifacts.remove(entity.boundRole().getUuid());
            return ArtifactPickupResult.FOUND_PLAYER_ARTIFACT;
        }
        return ArtifactPickupResult.FOUND_NORMAL_ARTIFACT;
    }

    public void tryReveal(Player hunter, double coneDegrees, double maxSearch) {
        Location eyeLocation = hunter.getEyeLocation();
        Location3i eye3i = Location3i.from(eyeLocation);
        Vector eyeDirection = eyeLocation.getDirection();
        double minShading = Math.cos(Math.toRadians(coneDegrees) / 2) * LIGHT_ANGLE_REVEAL_MULTIPLIER;
        double maxSearchSquared = maxSearch * maxSearch;
        for (Map.Entry<Location3i, ArtifactType> entry : artifacts.entrySet()) {
            Location3i artifactPos3i = entry.getKey();
            if (Math.abs(eye3i.x() - artifactPos3i.x()) + Math.abs(eye3i.y() - artifactPos3i.y()) + Math.abs(eye3i.z() - artifactPos3i.z()) > maxSearch + 1) {
                continue;
            }
            Location artifactPos = artifactPos3i.toLocation().add(0.5, 0, 0.5);
            if (!reveals(eyeLocation, eyeDirection, artifactPos, minShading, maxSearch, maxSearchSquared)) {
                continue;
            }
            reveal(artifactPos, hunter, entry.getValue(), null);
        }
        ConeConfig coneConfig = new ConeConfig();
        coneConfig.setConeDegrees(coneDegrees);
        coneConfig.setRange(maxSearch);
        coneConfig.setTraceStep(ARTIFACT_TRACE_STEP);

        Collection<ConeTraceResult> results = manor2.getTracing()
            .traceCone(new TraceShooter.PlayerTraceShooter(hunter), entity -> manor2.getRoleMap()
                    .is(entity.getUniqueId(), Artifact.class),
                Comparator.comparingDouble(ConeTraceResult::r),
                coneConfig
            );
        long atNano = manor2.getLatencyHandler().timeOf(hunter);
        for (ConeTraceResult result : results) {
            if (!(manor2.getRoleMap().get(result.target().getUniqueId()) instanceof Artifact artifact) || artifact.started() >= atNano) {
                continue;
            }
            artifact.freeze();
            reveal(result.target().getLocation(),
                hunter,
                playerArtifacts.computeIfAbsent(artifact.getUuid(), ignored -> Util.getRandom(playerArtifactTypes)),
                artifact
            );
        }
    }

    public Optional<ArtifactSpawnInformation> allocateRegion() {
        List<Map.Entry<Location3i, ArtifactRegion>> allowedRegions = pickedUpPos.entrySet().stream()
            .filter(entry -> noNearbyPlayer(entry.getValue()))
            .toList();
        if (allowedRegions.isEmpty()) {
            return Optional.empty();
        }
        Map.Entry<Location3i, ArtifactRegion> artifactRegion = Util.getRandom(allowedRegions);
        for (Location3i placedArtifactPos : artifacts.keySet()) {
            if (artifactRegion.getValue().boundingBox().contains(placedArtifactPos.toLocation())) {
                artifacts.remove(placedArtifactPos);
                ArtifactEntity entity = placedArtifacts.remove(placedArtifactPos);
                if (entity != null) {
                    entity.remove();
                }
                break;
            }
        }
        occupiedRegions.put(artifactRegion.getValue(), time);
        return Optional.of(new ArtifactSpawnInformation(artifactRegion.getValue(), artifactRegion.getKey()));
    }

    private void reveal(Location artifactPos, Player hunter, ArtifactType artifactType, @Nullable Artifact boundToRole) {
        Location3i artifactPos3i = Location3i.from(artifactPos);
        if (!(placedArtifacts.get(artifactPos3i) instanceof ArtifactEntity entity)) {
            hunter.sendActionBar(
                Util.color(REVEAL_ARTIFACT_MESSAGE)
            );
            Util.getRandom(manor2.getDefinition().getArtifactRevealSounds())
                .playAt(artifactPos);
            placedArtifacts.put(artifactPos3i, ArtifactEntity.spawn(artifactPos, artifactType, time, boundToRole, boundToRole != null));
        } else {
            placedArtifacts.put(artifactPos3i, entity.withTimeStamp(time));
        }
        for (ArtifactRegion region : occupiedRegions.keySet()) {
            if (region.boundingBox().contains(artifactPos)) {
                break;
            }
        }
    }

    private boolean reveals(Location eyeLocation, Vector eyeDirection, Location artifactPos, double minShading, double maxSearch, double maxSearchSquared) {
        if (Math.abs(eyeLocation.x() - artifactPos.x()) + Math.abs(eyeLocation.y() - artifactPos.y()) + Math.abs(eyeLocation.z() - artifactPos.z()) > maxSearch + 1) {
            return false;
        }
        Vector diff = artifactPos.toVector().subtract(eyeLocation.toVector());
        double diffLengthSquared = diff.lengthSquared();
        Vector diffNormalized = diff.clone().normalize();
        return !(diffNormalized.dot(eyeDirection) < minShading) && !(maxSearchSquared < diffLengthSquared) &&
            Tracing.hasClearShot(eyeLocation, diffNormalized, Math.sqrt(diffLengthSquared), ARTIFACT_TRACE_STEP);
    }

    public int artifactsPickedUp() {
        return artifactPickedUpCount.get();
    }


    private boolean noNearbyPlayer(ArtifactRegion spawningRegion) {
        WorldTiedBoundingBox expanded = (WorldTiedBoundingBox) spawningRegion.boundingBox().clone().expand(MIN_RESPAWN_PLAYER_PROXIMITY);
        return manor2.getRoleMap().getMatching(GhostHunter.class).stream()
            .map(GhostHunter::getEventPlayer)
            .map(EventPlayer::getPlayer)
            .filter(Objects::nonNull)
            .map(Player::getLocation)
            .noneMatch(expanded::contains);
    }

    private double fillFactor() {
        try {
            ExpressionBuilder builder = new ExpressionBuilder(manor2.getDefinition().getRegionFillFactor());
            builder.variable("hunter_count");
            builder.function(new Function("max", 2) {
                @Override
                public double apply(double... doubles) {
                    return Math.max(doubles[0], doubles[1]);
                }
            });
            builder.function(new Function("min", 2) {
                @Override
                public double apply(double... doubles) {
                    return Math.min(doubles[0], doubles[1]);
                }
            });
            Expression expression = builder.build();
            expression.setVariable("hunter_count", manor2.getRoleMap().getMatching(GhostHunter.class).size());
            ValidationResult result = expression.validate();
            if (!result.isValid()) {
                if (!fillFactorFunctionIssueSent) {
                    fillFactorFunctionIssueSent = true;
                    EventMain.getInstance().getLogger().warning("Manor2 region fill factor is invalidly specified: %s".formatted(result.getErrors()));
                }
                return 1D;
            }
            return expression.evaluate();
        } catch (IllegalArgumentException e) {
            if (!fillFactorFunctionIssueSent) {
                fillFactorFunctionIssueSent = true;
                EventMain.getInstance().getLogger().warning("Manor2 region fill factor is invalidly specified: %s".formatted(e.getMessage()));
            }
            return 1D;
        }
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
}
