package dev.lumas.events.utility.vector;

import org.bukkit.Location;
import org.bukkit.World;

public record Location3i(World world, int x, int y, int z) {

    public static Location3i from(Location location) {
        return new Location3i(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public Location toLocation() {
        return new Location(world, x, y, z);
    }
}
