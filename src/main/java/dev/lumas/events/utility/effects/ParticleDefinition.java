package dev.lumas.events.utility.effects;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

@NullMarked
public record ParticleDefinition(Particle particleKey, double spawnProbability, int count,
                                 @Nullable BoundingBox spawnArea,
                                 @Nullable Object options) {

    private static final double VIEW_DISTANCE = 32;

    public void showAt(Location location) {
        Location spawnLocation = rollSpawnLocation(location);
        if (spawnLocation == null) {
            return;
        }
        location.getWorld().spawnParticle(particleKey, spawnLocation, count, options);
    }

    public void showAt(Location location, Predicate<Player> visibleTo) {
        Location spawnLocation = rollSpawnLocation(location);
        if (spawnLocation == null) {
            return;
        }
        for (Player player : spawnLocation.getNearbyPlayers(VIEW_DISTANCE)) {
            if (visibleTo.test(player)) {
                player.spawnParticle(particleKey, spawnLocation, count, options);
            }
        }
    }

    private @Nullable Location rollSpawnLocation(Location location) {
        Random random = ThreadLocalRandom.current();
        if (random.nextDouble(100d) > spawnProbability) {
            return null;
        }
        Vector offset;
        if (spawnArea == null) {
            offset = new Vector();
        } else {
            offset = new Vector(
                    random.nextDouble(spawnArea.getMinX(), spawnArea.getMaxX()),
                    random.nextDouble(spawnArea.getMinY(), spawnArea.getMaxY()),
                    random.nextDouble(spawnArea.getMinZ(), spawnArea.getMaxZ())
            );
        }
        return location.clone().add(offset);
    }

    public void showAt(Entity entity) {
        showAt(entity.getLocation());
    }
}
