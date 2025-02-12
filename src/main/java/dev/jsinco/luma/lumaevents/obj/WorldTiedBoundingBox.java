package dev.jsinco.luma.lumaevents.obj;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

@ToString
@Getter
@Setter
public class WorldTiedBoundingBox extends BoundingBox {

    private World world;

    public WorldTiedBoundingBox(World world, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        super(minX, minY, minZ, maxX, maxY, maxZ);
        this.world = world;
    }

    public boolean contains(Entity entity) {
        return this.world.equals(entity.getWorld()) && super.contains(entity.getLocation().toVector());
    }

    public boolean contains(Location location) {
        return this.world.equals(location.getWorld()) && super.contains(location.toVector());
    }

    public List<Block> getBlocks() {
        List<Block> bL = new LinkedList<>();
        for (int x = (int) this.getMinX(); x <= (int) this.getMaxX(); ++x) {
            for (int y = (int) this.getMinY(); y <= (int) this.getMaxY(); ++y) {
                for (int z = (int) this.getMinZ(); z <= (int) this.getMaxZ(); ++z) {
                    bL.add(this.world.getBlockAt(x, y, z));
                }
            }
        }
        return bL;
    }

    public Location getRandomLocation() {
        final Random rand = new Random();
        final double x = rand.nextDouble(Math.abs(this.getMaxX() - this.getMinX()) + 1) + this.getMinX();
        final double y = rand.nextDouble(Math.abs(this.getMaxY() - this.getMinY()) + 1) + this.getMinY();
        final double z = rand.nextDouble(Math.abs(this.getMaxZ() - this.getMinZ()) + 1) + this.getMinZ();
        return new Location(this.world, x, y, z);
    }

    public Location getCenterLocation() {
        return new Location(this.getWorld(), this.getCenterX(), this.getCenterY(), this.getCenterZ());
    }

    public Location getCenterLocation(float pitch, float yaw) {
        return new Location(this.getWorld(), this.getCenterX(), this.getCenterY(), this.getCenterZ(), yaw, pitch);
    }

    public List<Player> getPlayers() {
        List<Player> players = new LinkedList<>();
        for (Player player : this.world.getPlayers()) {
            if (this.contains(player)) {
                players.add(player);
            }
        }
        return players;
    }

    public <T extends Entity> List<T> getEntities(Class<T> clazz) {
        List<T> entities = new LinkedList<>();
        for (Entity entity : this.world.getEntitiesByClass(clazz)) {
            if (this.contains(entity)) {
                entities.add(clazz.cast(entity));
            }
        }
        return entities;
    }

    @NotNull
    public static WorldTiedBoundingBox of(Location location, Location location2) {
        return new WorldTiedBoundingBox(location.getWorld(), location.getX(), location.getY(), location.getZ(), location2.getX(), location2.getY(), location2.getZ());
    }
}
