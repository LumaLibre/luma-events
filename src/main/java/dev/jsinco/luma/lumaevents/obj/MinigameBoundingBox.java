package dev.jsinco.luma.lumaevents.obj;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;

public interface MinigameBoundingBox {
    boolean contains(Location location);

    default boolean contains(Entity entity) {
        return contains(entity.getLocation());
    }

    List<Player> getPlayers();

    <T extends Entity> List<T> getEntities(Class<T> clazz);
}
