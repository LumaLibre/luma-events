package dev.lumas.events.utility.vector;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public record BlockPos(int x, int y, int z) {

    public static BlockPos of(Block block) {
        return new BlockPos(block.getX(), block.getY(), block.getZ());
    }

    public static BlockPos of(Location location) {
        return new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public BlockPos add(int dx, int dy, int dz) {
        return new BlockPos(x + dx, y + dy, z + dz);
    }

    public BlockPos add(BlockPos other) {
        return add(other.x, other.y, other.z);
    }

    public BlockPos subtract(BlockPos other) {
        return add(-other.x, -other.y, -other.z);
    }

    public long chunkKey() {
        return Chunk.getChunkKey(x >> 4, z >> 4);
    }

    public Location toLocation(World world) {
        return new Location(world, x, y, z);
    }

    public Location center(World world) {
        return new Location(world, x + 0.5, y, z + 0.5);
    }
}
