package dev.lumas.events.utility.effects;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@NullMarked
public record ParticleDefinition(Particle particleKey, double spawnProbability, int count,
                                 @Nullable BoundingBox spawnArea,
                                 @Nullable Object options) {

    public void showAt(Location location) {
        Random random = ThreadLocalRandom.current();
        if (random.nextDouble(100d) > spawnProbability) {
            return;
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
        location.getWorld().spawnParticle(particleKey, location.clone().add(offset), count, options);
    }

    public void showAt(Entity entity) {
        showAt(entity.getLocation());
    }
}
