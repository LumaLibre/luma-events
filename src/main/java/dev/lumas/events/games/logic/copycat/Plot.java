package dev.lumas.events.games.logic.copycat;

import dev.lumas.events.model.WorldTiedBoundingBox;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public final class Plot {

    private final int index;
    private final World world;
    private final BlockPos buildOrigin;
    private final int footprint;
    private final int height;
    private final int elevation;
    private final WorldTiedBoundingBox interior;
    private final Location spawn;
    private final Location overview;
    private volatile BlockPos activeMin;
    private volatile BlockPos activeSize;

    Plot(int index, World world, BlockPos interiorMin, int interiorSize, int footprint, int height, int elevation, int ceiling, int wallTop) {
        this.index = index;
        this.world = world;
        this.footprint = footprint;
        this.height = elevation + height;
        this.elevation = elevation;
        int padding = (interiorSize - footprint) / 2;
        this.buildOrigin = interiorMin.add(padding, 0, padding);
        this.interior = new WorldTiedBoundingBox(world,
            interiorMin.x(), interiorMin.y(), interiorMin.z(),
            interiorMin.x() + interiorSize, ceiling, interiorMin.z() + interiorSize);
        this.activeMin = buildOrigin;
        this.activeSize = new BlockPos(footprint, this.height, footprint);
        double centerX = interiorMin.x() + interiorSize / 2.0;
        double centerZ = interiorMin.z() + interiorSize / 2.0;
        this.spawn = new Location(world, interiorMin.x() + 1.5, interiorMin.y() + 1, interiorMin.z() + 1.5, -45f, 10f);
        this.overview = new Location(world, centerX, wallTop + 4, centerZ, 0f, 80f);
    }

    public BlockPos placementOffset(CopycatBuild build) {
        int lift = build.getSettings().offGround() ? elevation : 0;
        return buildOrigin.add((footprint - build.getSizeX()) / 2, lift, (footprint - build.getSizeZ()) / 2);
    }

    public Map<BlockPos, BlockData> place(CopycatBuild build) {
        BlockPos offset = placementOffset(build);
        Map<BlockPos, BlockData> placed = new HashMap<>(build.blockCount());
        build.getBlocks().forEach((pos, data) -> placed.put(pos.add(offset), data));
        return placed;
    }

    public Map<BlockPos, BlockData> activate(CopycatBuild build) {
        BlockPos min = placementOffset(build);
        this.activeMin = min;
        this.activeSize = new BlockPos(build.getSizeX(), build.getSizeY(), build.getSizeZ());
        Map<BlockPos, BlockData> floor = new HashMap<>(footprint * footprint);
        int floorY = buildOrigin.y() - 1;
        for (int x = 0; x < footprint; x++) {
            for (int z = 0; z < footprint; z++) {
                int worldX = buildOrigin.x() + x;
                int worldZ = buildOrigin.z() + z;
                boolean marked = worldX >= min.x() && worldX < min.x() + build.getSizeX()
                    && worldZ >= min.z() && worldZ < min.z() + build.getSizeZ();
                floor.put(new BlockPos(worldX, floorY, worldZ), marked ? Arena.FOOTPRINT_MARKER : Arena.FLOOR);
            }
        }
        return floor;
    }

    public List<BlockPos> buildAreaPositions() {
        return positions(activeMin, activeSize);
    }

    public List<BlockPos> fullAreaPositions() {
        return positions(buildOrigin, new BlockPos(footprint, height, footprint));
    }

    private static List<BlockPos> positions(BlockPos min, BlockPos size) {
        List<BlockPos> positions = new ArrayList<>(size.x() * size.y() * size.z());
        for (int x = 0; x < size.x(); x++) {
            for (int y = 0; y < size.y(); y++) {
                for (int z = 0; z < size.z(); z++) {
                    positions.add(min.add(x, y, z));
                }
            }
        }
        return positions;
    }

    public boolean inBuildArea(BlockPos blockPos) {
        BlockPos min = activeMin;
        BlockPos size = activeSize;
        return blockPos.x() >= min.x() && blockPos.x() < min.x() + size.x()
            && blockPos.y() >= min.y() && blockPos.y() < min.y() + size.y()
            && blockPos.z() >= min.z() && blockPos.z() < min.z() + size.z();
    }

    public Location clamp(Location location) {
        return new Location(world,
            Math.clamp(location.getX(), interior.getMinX() + 0.5, interior.getMaxX() - 0.5),
            Math.clamp(location.getY(), interior.getMinY(), interior.getMaxY() - 1),
            Math.clamp(location.getZ(), interior.getMinZ() + 0.5, interior.getMaxZ() - 0.5),
            location.getYaw(), location.getPitch());
    }
}
