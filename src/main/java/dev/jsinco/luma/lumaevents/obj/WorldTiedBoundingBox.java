package dev.jsinco.luma.lumaevents.obj;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

@ToString
@Getter
@Setter
public class WorldTiedBoundingBox extends BoundingBox implements MinigameBoundingBox {

    private World world;

    public WorldTiedBoundingBox(World world, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        super(minX, minY, minZ, maxX, maxY, maxZ);
        this.world = world;
    }

    @Override
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

    public void operate(Consumer<Block> consumer) {
        for (int x = (int) this.getMinX(); x <= (int) this.getMaxX(); ++x) {
            for (int y = (int) this.getMinY(); y <= (int) this.getMaxY(); ++y) {
                for (int z = (int) this.getMinZ(); z <= (int) this.getMaxZ(); ++z) {
                    consumer.accept(this.world.getBlockAt(x, y, z));
                }
            }
        }
    }

    public Location getRandomLocation() {
        final Random rand = new Random();
        final double x = rand.nextDouble(Math.abs(this.getMaxX() - this.getMinX()) + 1) + this.getMinX();
        final double y = rand.nextDouble(Math.abs(this.getMaxY() - this.getMinY()) + 1) + this.getMinY();
        final double z = rand.nextDouble(Math.abs(this.getMaxZ() - this.getMinZ()) + 1) + this.getMinZ();
        return new Location(this.world, x, y, z);
    }

    public boolean isInWithMarge(final Location loc, final double marge) {
        return loc.getWorld() == this.world && loc.getX() >= this.getMinX() - marge && loc.getX() <= this.getMaxX() + marge && loc.getY() >= this.getMinY() - marge && loc
                .getY() <= this.getMaxY() + marge && loc.getZ() >= this.getMinZ() - marge && loc.getZ() <= this.getMaxZ() + marge;
    }

    @Override
    public Location getCenterLocation() {
        return new Location(this.getWorld(), this.getCenterX(), this.getCenterY(), this.getCenterZ());
    }

    public Location getCenterLocation(float pitch, float yaw) {
        return new Location(this.getWorld(), this.getCenterX(), this.getCenterY(), this.getCenterZ(), yaw, pitch);
    }

    @Override
    public List<Player> getPlayers() {
        List<Player> players = new LinkedList<>();
        for (Player player : this.world.getPlayers()) {
            if (this.contains(player)) {
                players.add(player);
            }
        }
        return players;
    }

    @Override
    public <T extends Entity> List<T> getEntities(Class<T> clazz) {
        List<T> entities = new LinkedList<>();
        for (Entity entity : this.world.getEntitiesByClass(clazz)) {
            if (this.contains(entity)) {
                entities.add(clazz.cast(entity));
            }
        }
        return entities;
    }

    public WorldTiedBoundingBox move(double x, double y, double z) {
        return new WorldTiedBoundingBox(this.world, this.getMinX() - x, this.getMinY() - y, this.getMinZ() - z, this.getMaxX() + x, this.getMaxY() + y, this.getMaxZ() + z);
    }

    public WorldTiedBoundingBox resize(Location loc1, Location loc2) {
        return new WorldTiedBoundingBox(this.world, loc1.getX(), loc1.getY(), loc1.getZ(), loc2.getX(), loc2.getY(), loc2.getZ());
    }

    @NotNull
    public static WorldTiedBoundingBox of(Location location, Location location2) {
        return new WorldTiedBoundingBox(location.getWorld(), location.getX(), location.getY(), location.getZ(), location2.getX(), location2.getY(), location2.getZ());
    }

    /**
     * Compares each location to find the min and max coordinates to create a bounding box.
     * @param locations the list of locations to encompass
     * @return a WorldTiedBoundingBox that contains all the provided locations
     */
    public static WorldTiedBoundingBox fromLocations(List<Location> locations, double additionalMargin, double overrideMinY) {
        Preconditions.checkArgument(!locations.isEmpty(), "Location list cannot be empty");

        World world = locations.getFirst().getWorld();
        double minX = Double.MAX_VALUE;
        //double minY = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;

        for (Location loc : locations) {
            Preconditions.checkArgument(loc.getWorld().equals(world), "All locations must be in the same world");
            if (loc.getX() < minX) minX = loc.getX();
            //if (loc.getY() < minY) minY = loc.getY();
            if (loc.getZ() < minZ) minZ = loc.getZ();
            if (loc.getX() > maxX) maxX = loc.getX();
            if (loc.getY() > maxY) maxY = loc.getY();
            if (loc.getZ() > maxZ) maxZ = loc.getZ();
        }
        return new WorldTiedBoundingBox(world, minX - additionalMargin, overrideMinY, minZ - additionalMargin,
                maxX + additionalMargin, maxY + additionalMargin, maxZ + additionalMargin);
    }
}
