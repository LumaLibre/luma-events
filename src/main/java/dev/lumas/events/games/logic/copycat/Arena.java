package dev.lumas.events.games.logic.copycat;

import dev.lumas.events.model.WorldTiedBoundingBox;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Getter
public final class Arena {

    private static final BlockData WALL = Material.BARRIER.createBlockData();
    static final BlockData FLOOR = Material.BLACK_TERRACOTTA.createBlockData();
    static final BlockData FOOTPRINT_MARKER = Material.LIGHT_GRAY_TERRACOTTA.createBlockData();
    private static final int PLOT_PADDING = 6;
    private static final int BUILD_ELEVATION = 4;
    private static final int FLIGHT_HEADROOM = 5;
    private static final int CEILING_GAP = 2;
    private static final int OVERVIEW_SPACE = 12;

    private final World world;
    private final List<Plot> plots;
    private final WorldTiedBoundingBox bounds;
    private final BlockPos origin;
    private final int plotSize;
    private final int stride;
    private final int columns;
    private final int rows;
    private final int wallTop;

    public Arena(Location origin, int plotCount, int footprint, int buildHeight) {
        this.world = origin.getWorld();
        this.origin = BlockPos.of(origin);
        this.plotSize = footprint + PLOT_PADDING * 2;
        this.stride = plotSize + 1;
        this.columns = (int) Math.ceil(Math.sqrt(plotCount));
        this.rows = (int) Math.ceil(plotCount / (double) columns);
        int floorY = this.origin.y();
        this.wallTop = floorY + BUILD_ELEVATION + buildHeight + FLIGHT_HEADROOM + CEILING_GAP;
        int ceiling = wallTop - CEILING_GAP;

        this.plots = new ArrayList<>(plotCount);
        for (int i = 0; i < plotCount; i++) {
            int column = i % columns;
            int row = i / columns;
            BlockPos interiorMin = this.origin.add(1 + column * stride, 1, 1 + row * stride);
            plots.add(new Plot(i, world, interiorMin, plotSize, footprint, buildHeight, BUILD_ELEVATION, ceiling, wallTop));
        }

        int width = columns * stride + 1;
        int depth = rows * stride + 1;
        this.bounds = new WorldTiedBoundingBox(world,
            this.origin.x(), floorY, this.origin.z(),
            this.origin.x() + width, wallTop + OVERVIEW_SPACE, this.origin.z() + depth);
    }

    public CompletableFuture<Void> build() {
        return ArenaBlocks.write(world, structure());
    }

    public CompletableFuture<Void> clear() {
        return ArenaBlocks.clear(bounds);
    }

    public boolean contains(Location location) {
        return bounds.contains(location);
    }

    public Location clamp(Location location) {
        return new Location(world,
            Math.clamp(location.getX(), bounds.getMinX() + 1, bounds.getMaxX() - 1),
            Math.clamp(location.getY(), bounds.getMinY() + 1, bounds.getMaxY() - 1),
            Math.clamp(location.getZ(), bounds.getMinZ() + 1, bounds.getMaxZ() - 1),
            location.getYaw(), location.getPitch());
    }

    private Map<BlockPos, BlockData> structure() {
        Map<BlockPos, BlockData> blocks = new HashMap<>();
        int floorY = origin.y();
        int width = columns * stride;
        int depth = rows * stride;

        for (int x = 0; x <= width; x++) {
            for (int z = 0; z <= depth; z++) {
                boolean wall = x % stride == 0 || z % stride == 0;
                blocks.put(origin.add(x, 0, z), FLOOR);
                if (!wall) continue;
                for (int y = 1; y <= wallTop - floorY; y++) {
                    blocks.put(origin.add(x, y, z), WALL);
                }
            }
        }

        for (Plot plot : plots) {
            BlockPos buildOrigin = plot.getBuildOrigin();
            for (int x = 0; x < plot.getFootprint(); x++) {
                for (int z = 0; z < plot.getFootprint(); z++) {
                    blocks.put(new BlockPos(buildOrigin.x() + x, floorY, buildOrigin.z() + z), FOOTPRINT_MARKER);
                }
            }
        }
        return blocks;
    }
}
