package dev.lumas.events.games.logic.manor2.roles.predicate;

import dev.lumas.events.games.logic.manor2.roles.Manor2Role;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.function.Predicate;

public record IsNearPredicate(Location target, double radius) implements Predicate<Manor2Role> {

    @Override
    public boolean test(Manor2Role role) {
        if (role.getEventPlayer().getPlayer() instanceof Player player) {
            return player.getLocation().distanceSquared(target) < radius * radius;
        }
        return false;
    }
}
