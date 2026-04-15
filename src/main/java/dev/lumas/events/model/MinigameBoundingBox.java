package dev.lumas.events.model;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;

public interface MinigameBoundingBox {
    boolean contains(Location location);

    default boolean contains(Entity entity) {
        return contains(entity.getLocation());
    }

    List<Player> getPlayers();

    <T extends Entity> List<T> getEntities(Class<T> clazz);

    Location getCenterLocation();

    void operate(Consumer<Block> consumer);
}
