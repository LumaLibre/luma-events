package dev.lumas.events.games.interfaces.structures;

import dev.lumas.events.utility.Executors;
import dev.thorinwasher.schem.Schematic;
import dev.thorinwasher.schem.SchematicReader;
import lombok.Getter;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.joml.Matrix3d;
import org.joml.Vector3i;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.DoubleConsumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

@Getter
public class GenericStructure extends Structure {

    private static final BlockData AIR_BLOCK_DATA = Material.AIR.createBlockData();
    Matrix3d transformation = new Matrix3d();

    private final Schematic schematic;

    public GenericStructure(Location origin, String localSchemPath) {
        super(origin, localSchemPath);
        Path path = SCHEMATIC_DIR.resolve(localSchemPath);
        if (!path.toFile().exists()) {
            throw new IllegalArgumentException("Schematic file does not exist at path: " + path.toAbsolutePath());
        }
        try (InputStream inputStream = new FileInputStream(path.toFile())) {
            this.schematic = new SchematicReader().read(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void paste() {
        paste((vector3i, blockData) -> true);
    }

    @Override
    public void remove() {
        remove((vector3i, blockData) -> true);
    }

    // Liberally borrowed from ThorinWasher in Garden

    public void paste(BiPredicate<Vector3i, BlockData> prePastePredicate) {
        paste(prePastePredicate, UnaryOperator.identity());
    }

    // blockDataMapper lets callers swap the schematic's blocks for something else (both the predicate and the
    // world receive the mapped data, so callers never have to reason about the raw palette)
    public void paste(BiPredicate<Vector3i, BlockData> prePastePredicate, UnaryOperator<BlockData> blockDataMapper) {
        Vector3i size = schematic.size(transformation);
        Vector3i offset = new Vector3i(size.x() / 2, 0, size.z() / 2);
        World world = origin.getWorld();
        schematic.apply(transformation, (vector3i, blockData) -> {

            int worldX = origin.getBlockX() + vector3i.x() - offset.x();
            int worldZ = origin.getBlockZ() + vector3i.z() - offset.z();
            int chunkX = worldX >> 4;
            int chunkZ = worldZ >> 4;

            Executors.runSync(world, chunkX, chunkZ, () -> {
                if (blockData.getMaterial().isAir()) return;

                vector3i.sub(offset);

                BlockData mappedBlockData = blockDataMapper.apply(blockData);
                if (!prePastePredicate.test(vector3i, mappedBlockData)) return;
                Location posToReplace = new Location(world, origin.getX(), origin.getY(), origin.getZ()).add(vector3i.x, vector3i.y, vector3i.z);

                world.setBlockData(posToReplace, mappedBlockData);
            });
        });
    }

    public CompletableFuture<Void> pasteAsync() {
        return pasteAsync((vector3i, blockData) -> true);
    }

    public CompletableFuture<Void> pasteAsync(BiPredicate<Vector3i, BlockData> prePastePredicate) {
        return pasteAsync(prePastePredicate, UnaryOperator.identity());
    }

    public CompletableFuture<Void> pasteAsync(BiPredicate<Vector3i, BlockData> prePastePredicate, UnaryOperator<BlockData> blockDataMapper) {
        return pasteAsync(prePastePredicate, blockDataMapper, progress -> {});
    }

    // onProgress receives the fraction of chunks written so far (0 to 1), from whichever region thread finished one
    public CompletableFuture<Void> pasteAsync(DoubleConsumer onProgress) {
        return pasteAsync((vector3i, blockData) -> true, UnaryOperator.identity(), onProgress);
    }

    // Placed without physics: nothing reacts to a half-built structure (falling sand, flowing water,
    // one half of a double chest before the other exists), and it's much faster
    public CompletableFuture<Void> pasteAsync(BiPredicate<Vector3i, BlockData> prePastePredicate, UnaryOperator<BlockData> blockDataMapper, DoubleConsumer onProgress) {
        World world = origin.getWorld();
        return applyByChunk((relative, blockData) -> {
            BlockData mappedBlockData = blockDataMapper.apply(blockData);
            if (!prePastePredicate.test(relative, mappedBlockData)) return;
            world.getBlockAt(origin.getBlockX() + relative.x, origin.getBlockY() + relative.y, origin.getBlockZ() + relative.z)
                    .setBlockData(mappedBlockData, false);
        }, onProgress);
    }

    public CompletableFuture<Void> removeAsync() {
        return removeAsync((vector3i, blockData) -> true);
    }

    // Removed without physics, same as pasting: containers don't spill their contents and nothing falls
    public CompletableFuture<Void> removeAsync(BiPredicate<Vector3i, BlockData> preRemovePredicate) {
        World world = origin.getWorld();
        return applyByChunk((relative, blockData) -> {
            if (!preRemovePredicate.test(relative, blockData)) return;
            world.getBlockAt(origin.getBlockX() + relative.x, origin.getBlockY() + relative.y, origin.getBlockZ() + relative.z)
                    .setBlockData(AIR_BLOCK_DATA, false);
        }, progress -> {});
    }

    private CompletableFuture<Void> applyByChunk(BiConsumer<Vector3i, BlockData> action, DoubleConsumer onProgress) {
        Vector3i size = schematic.size(transformation);
        Vector3i offset = new Vector3i(size.x() / 2, 0, size.z() / 2);
        World world = origin.getWorld();

        Map<Long, List<Placement>> byChunk = new HashMap<>();
        schematic.apply(transformation, (vector3i, blockData) -> {
            if (blockData.getMaterial().isAir()) return;
            Vector3i relative = new Vector3i(vector3i).sub(offset);
            long chunkKey = Chunk.getChunkKey((origin.getBlockX() + relative.x) >> 4, (origin.getBlockZ() + relative.z) >> 4);
            byChunk.computeIfAbsent(chunkKey, k -> new ArrayList<>()).add(new Placement(relative, blockData));
        });

        int total = byChunk.size();
        AtomicInteger done = new AtomicInteger();
        List<CompletableFuture<Void>> futures = new ArrayList<>(total);
        byChunk.forEach((chunkKey, placements) -> {
            int chunkX = (int) (long) chunkKey;
            int chunkZ = (int) (chunkKey >>> 32);
            CompletableFuture<Void> future = new CompletableFuture<>();
            world.getChunkAtAsync(chunkX, chunkZ).whenComplete((chunk, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                    return;
                }
                Executors.runSync(world, chunkX, chunkZ, () -> {
                    try {
                        for (Placement placement : placements) {
                            action.accept(placement.relative(), placement.blockData());
                        }
                        // Report before completing, so the last update can't land after the caller has moved on
                        onProgress.accept((double) done.incrementAndGet() / total);
                        future.complete(null);
                    } catch (Throwable t) {
                        future.completeExceptionally(t);
                    }
                });
            });
            futures.add(future);
        });
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private record Placement(Vector3i relative, BlockData blockData) {}

    // Reads only the in-memory schematic (no world access), so this is safe to call from any thread
    public List<Location> findBlocks(Predicate<BlockData> filter) {
        return new ArrayList<>(findBlockData(filter).keySet());
    }

    // Like findBlocks, but keeps each match's block data. In schematic order.
    public Map<Location, BlockData> findBlockData(Predicate<BlockData> filter) {
        Vector3i size = schematic.size(transformation);
        Vector3i offset = new Vector3i(size.x() / 2, 0, size.z() / 2);
        World world = origin.getWorld();
        Map<Location, BlockData> found = new LinkedHashMap<>();
        schematic.apply(transformation, (vector3i, blockData) -> {
            if (!filter.test(blockData)) return;
            Vector3i relative = new Vector3i(vector3i).sub(offset);
            found.put(new Location(world, origin.getBlockX() + relative.x, origin.getBlockY() + relative.y, origin.getBlockZ() + relative.z), blockData);
        });
        return found;
    }

    public CompletableFuture<Map<Location, BlockData>> findBlockDataAsync(Predicate<BlockData> filter) {
        CompletableFuture<Map<Location, BlockData>> future = new CompletableFuture<>();
        Executors.runAsync(() -> {
            try {
                future.complete(findBlockData(filter));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    public void remove(BiPredicate<Vector3i, BlockData> preRemovePredicate) {
        Vector3i size = schematic.size(transformation);
        Vector3i offset = new Vector3i(size.x() / 2, 0, size.z() / 2);
        World world = origin.getWorld();
        schematic.apply(transformation, (vector3i, blockData) -> {

            int worldX = origin.getBlockX() + vector3i.x() - offset.x();
            int worldZ = origin.getBlockZ() + vector3i.z() - offset.z();
            int chunkX = worldX >> 4;
            int chunkZ = worldZ >> 4;

            Executors.runSync(world, chunkX, chunkZ, () -> {
                if (blockData.getMaterial().isAir()) return;

                vector3i.sub(offset);
                if (!preRemovePredicate.test(vector3i, blockData)) return;
                Location location = new Location(world, origin.getX(), origin.getY(), origin.getZ()).add(vector3i.x, vector3i.y, vector3i.z);

                location.getBlock().setBlockData(AIR_BLOCK_DATA);
            });
        });
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        GenericStructure structure = (GenericStructure) obj;

        if (!origin.equals(structure.origin)) return false;
        return schematic.equals(structure.schematic);
    }

    @Override
    public int hashCode() {
        int result = origin.hashCode();
        result = 31 * result + schematic.hashCode();
        return result;
    }

}
