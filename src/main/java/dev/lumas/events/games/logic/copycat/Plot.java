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
    private final WorldTiedBoundingBox interior;
    private final WorldTiedBoundingBox buildArea;
    private final Location spawn;
    private final Location overview;

    Plot(int index, World world, BlockPos interiorMin, int interiorSize, int footprint, int height, int elevation, int ceiling, int wallTop) {
        this.index = index;
        this.world = world;
        this.footprint = footprint;
        this.height = height;
        int padding = (interiorSize - footprint) / 2;
        this.buildOrigin = interiorMin.add(padding, elevation, padding);
        this.interior = new WorldTiedBoundingBox(world,
            interiorMin.x(), interiorMin.y(), interiorMin.z(),
            interiorMin.x() + interiorSize, ceiling, interiorMin.z() + interiorSize);
        this.buildArea = new WorldTiedBoundingBox(world,
            buildOrigin.x(), buildOrigin.y(), buildOrigin.z(),
            buildOrigin.x() + footprint, buildOrigin.y() + height, buildOrigin.z() + footprint);
        double centerX = interiorMin.x() + interiorSize / 2.0;
        double centerZ = interiorMin.z() + interiorSize / 2.0;
        this.spawn = new Location(world, interiorMin.x() + 1.5, interiorMin.y() + elevation, interiorMin.z() + 1.5, -45f, 10f);
        this.overview = new Location(world, centerX, wallTop + 4, centerZ, 0f, 80f);
    }

    public BlockPos placementOffset(CopycatBuild build) {
        return buildOrigin.add((footprint - build.getSizeX()) / 2, 0, (footprint - build.getSizeZ()) / 2);
    }

    public Map<BlockPos, BlockData> place(CopycatBuild build) {
        BlockPos offset = placementOffset(build);
        Map<BlockPos, BlockData> placed = new HashMap<>(build.blockCount());
        build.getBlocks().forEach((pos, data) -> placed.put(pos.add(offset), data));
        return placed;
    }

    public List<BlockPos> buildAreaPositions() {
        List<BlockPos> positions = new ArrayList<>(footprint * footprint * height);
        for (int x = 0; x < footprint; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < footprint; z++) {
                    positions.add(buildOrigin.add(x, y, z));
                }
            }
        }
        return positions;
    }

    public boolean inBuildArea(BlockPos blockPos) {
        return blockPos.x() >= buildOrigin.x() && blockPos.x() < buildOrigin.x() + footprint
            && blockPos.y() >= buildOrigin.y() && blockPos.y() < buildOrigin.y() + height
            && blockPos.z() >= buildOrigin.z() && blockPos.z() < buildOrigin.z() + footprint;
    }

    public Location clamp(Location location) {
        return new Location(world,
            Math.clamp(location.getX(), interior.getMinX() + 0.5, interior.getMaxX() - 0.5),
            Math.clamp(location.getY(), interior.getMinY(), interior.getMaxY() - 1),
            Math.clamp(location.getZ(), interior.getMinZ() + 0.5, interior.getMaxZ() - 0.5),
            location.getYaw(), location.getPitch());
    }
}
