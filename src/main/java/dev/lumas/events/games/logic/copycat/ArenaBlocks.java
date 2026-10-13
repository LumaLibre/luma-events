package dev.lumas.events.games.logic.copycat;

import dev.lumas.events.model.WorldTiedBoundingBox;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.vector.BlockPos;
import lombok.experimental.UtilityClass;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

@UtilityClass
public final class ArenaBlocks {

    public static final BlockData AIR = Material.AIR.createBlockData();

    public static CompletableFuture<Void> write(World world, Map<BlockPos, BlockData> blocks) {
        return perChunk(world, blocks.keySet(), (chunk, positions) -> {
            for (BlockPos blockPos : positions) {
                chunk.getBlock(blockPos.x() & 15, blockPos.y(), blockPos.z() & 15).setBlockData(blocks.get(blockPos), false);
            }
        });
    }

    public static CompletableFuture<Void> fill(World world, Collection<BlockPos> positions, BlockData data) {
        return perChunk(world, positions, (chunk, chunkPositions) -> {
            for (BlockPos blockPos : chunkPositions) {
                chunk.getBlock(blockPos.x() & 15, blockPos.y(), blockPos.z() & 15).setBlockData(data, false);
            }
        });
    }

    public static CompletableFuture<Map<BlockPos, Placed>> read(World world, Collection<BlockPos> positions) {
        Map<BlockPos, Placed> result = new ConcurrentHashMap<>();
        return perChunk(world, positions, (chunk, chunkPositions) -> {
            for (BlockPos blockPos : chunkPositions) {
                result.put(blockPos, Placed.of(chunk.getBlock(blockPos.x() & 15, blockPos.y(), blockPos.z() & 15)));
            }
        }).thenApply(ignored -> result);
    }

    public static CompletableFuture<Void> decorate(World world, Map<BlockPos, TileDetail> details) {
        if (details.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return perChunk(world, details.keySet(), (chunk, chunkPositions) -> {
            for (BlockPos blockPos : chunkPositions) {
                BlockState state = chunk.getBlock(blockPos.x() & 15, blockPos.y(), blockPos.z() & 15).getState();
                details.get(blockPos).applyTo(state);
                state.update(true, false);
            }
        });
    }

    public static CompletableFuture<Void> clear(WorldTiedBoundingBox box) {
        World world = box.getWorld();
        int minX = (int) Math.floor(box.getMinX());
        int maxX = (int) Math.floor(box.getMaxX());
        int minZ = (int) Math.floor(box.getMinZ());
        int maxZ = (int) Math.floor(box.getMaxZ());
        int minY = Math.max(world.getMinHeight(), (int) Math.floor(box.getMinY()));
        int maxY = Math.min(world.getMaxHeight() - 1, (int) Math.floor(box.getMaxY()));

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                int cx = chunkX;
                int cz = chunkZ;
                futures.add(onChunk(world, cx, cz, chunk -> {
                    for (Entity entity : chunk.getEntities()) {
                        if (!(entity instanceof Player) && box.contains(entity.getLocation())) {
                            entity.remove();
                        }
                    }
                    ChunkSnapshot snapshot = chunk.getChunkSnapshot(false, false, false);
                    for (int x = Math.max(minX, cx << 4); x <= Math.min(maxX, (cx << 4) + 15); x++) {
                        for (int z = Math.max(minZ, cz << 4); z <= Math.min(maxZ, (cz << 4) + 15); z++) {
                            for (int y = minY; y <= maxY; y++) {
                                if (!snapshot.getBlockType(x & 15, y, z & 15).isAir()) {
                                    chunk.getBlock(x & 15, y, z & 15).setBlockData(AIR, false);
                                }
                            }
                        }
                    }
                }));
            }
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private static CompletableFuture<Void> perChunk(World world, Collection<BlockPos> positions, BiConsumer<Chunk, List<BlockPos>> action) {
        Map<Long, List<BlockPos>> byChunk = new HashMap<>();
        for (BlockPos blockPos : positions) {
            byChunk.computeIfAbsent(blockPos.chunkKey(), key -> new ArrayList<>()).add(blockPos);
        }
        List<CompletableFuture<Void>> futures = new ArrayList<>(byChunk.size());
        byChunk.forEach((chunkKey, chunkPositions) -> {
            int chunkX = (int) (long) chunkKey;
            int chunkZ = (int) (chunkKey >>> 32);
            futures.add(onChunk(world, chunkX, chunkZ, chunk -> action.accept(chunk, chunkPositions)));
        });
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private static CompletableFuture<Void> onChunk(World world, int chunkX, int chunkZ, java.util.function.Consumer<Chunk> action) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        world.getChunkAtAsync(chunkX, chunkZ).whenComplete((chunk, throwable) -> {
            if (throwable != null) {
                future.completeExceptionally(throwable);
                return;
            }
            Executors.runSync(world, chunkX, chunkZ, () -> {
                try {
                    action.accept(chunk);
                    future.complete(null);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        });
        return future;
    }
}
