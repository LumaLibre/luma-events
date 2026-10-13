package dev.lumas.events.games.logic.skywars;

import com.google.common.base.Preconditions;
import dev.lumas.events.games.interfaces.structures.GenericStructure;
import dev.lumas.events.model.WorldTiedBoundingBox;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class ArenaBorder {

    private static final double PADDING = 2;
    private static final double FINAL_SIZE = 24;
    private static final long SHRINK_DELAY = TimeUnit.SECONDS.toMillis(90);
    private static final long SHRINK_DURATION_TICKS = TimeUnit.MINUTES.toSeconds(6) * 20;
    private static final int WARNING_DISTANCE = 5;
    private static final int WARNING_TICKS = 15 * 20;

    private final WorldBorder border;
    private final double centerX;
    private final double centerZ;
    @Getter
    private final WorldTiedBoundingBox footprint;
    private volatile long activatedAt = -1;
    private volatile boolean shrinking;

    public ArenaBorder(GenericStructure map, Set<Material> ignored) {
        List<Location> solid = map.findBlocks(data -> data.getMaterial().isSolid() && !ignored.contains(data.getMaterial()));
        Preconditions.checkArgument(!solid.isEmpty(), "SkyWars map " + map.getLocalSchemPath() + " has no solid blocks to fit a border around");

        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (Location location : solid) {
            minX = Math.min(minX, location.getBlockX());
            minZ = Math.min(minZ, location.getBlockZ());
            maxX = Math.max(maxX, location.getBlockX());
            maxZ = Math.max(maxZ, location.getBlockZ());
        }

        this.centerX = (minX + maxX + 1) / 2.0;
        this.centerZ = (minZ + maxZ + 1) / 2.0;
        double initialSize = Math.max(maxX - minX, maxZ - minZ) + 1 + PADDING * 2;
        World world = map.getOrigin().getWorld();
        this.footprint = new WorldTiedBoundingBox(world,
            centerX - initialSize / 2, world.getMinHeight(), centerZ - initialSize / 2,
            centerX + initialSize / 2, world.getMaxHeight(), centerZ + initialSize / 2);

        this.border = Bukkit.createWorldBorder();
        border.setCenter(centerX, centerZ);
        border.setSize(initialSize);
        border.setWarningDistance(WARNING_DISTANCE);
        border.setWarningTimeTicks(WARNING_TICKS);
    }

    public void show(Player player) {
        player.setWorldBorder(border);
    }

    public void activate() {
        this.activatedAt = System.currentTimeMillis();
    }

    public boolean tick() {
        if (shrinking || activatedAt < 0 || System.currentTimeMillis() - activatedAt < SHRINK_DELAY) {
            return false;
        }
        shrinking = true;
        border.changeSize(FINAL_SIZE, SHRINK_DURATION_TICKS);
        return true;
    }

    public double distanceOutside(Location location) {
        double half = border.getSize() / 2;
        double x = Math.abs(location.getX() - centerX) - half;
        double z = Math.abs(location.getZ() - centerZ) - half;
        return Math.max(0, Math.max(x, z));
    }
}
