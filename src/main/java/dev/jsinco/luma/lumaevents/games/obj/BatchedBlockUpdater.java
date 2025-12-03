package dev.jsinco.luma.lumaevents.games.obj;

import com.gamingmesh.jobs.commands.list.log;
import dev.jsinco.luma.lumacore.utility.Logging;
import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import dev.jsinco.luma.lumaevents.utility.Executors;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * A class designed to handle block updates and replacements for an entire bounding
 * box area based on certain criteria. Operations are batched over a set amount of ticks
 * to reduce performance impact on the server.
 */
public class BatchedBlockUpdater {

    private final List<BatchedBlockUpdate> completedOperations;
    private final List<BatchedBlockUpdate> batches;
    @Getter
    @Setter
    private WorldTiedBoundingBox boundingBox;
    private final int batchSize;
    private final int ticksInterval;


    public BatchedBlockUpdater(WorldTiedBoundingBox boundingBox, int batchSize, int ticksInterval) {
        this.completedOperations = new ArrayList<>();
        this.batches = new ArrayList<>();
        this.boundingBox = boundingBox;
        this.batchSize = batchSize;
        this.ticksInterval = ticksInterval;
    }

    public void prepareBatched(Consumer<Block> consumer) {
        this.prepareBatches(consumer, true);
    }

    /**
     * Prepare batches of block updates within the bounding box.
     * @param consumer the operation to perform on each block
     * @param overwriteExisting whether to clear existing batches before preparing new ones
     */
    public void prepareBatches(Consumer<Block> consumer, boolean overwriteExisting) {
        if (overwriteExisting) {
            this.batches.clear();
        }
        List<Block> allBlocks = this.boundingBox.getBlocks();
        int totalBlocks = allBlocks.size();

        for (int i = 0; i < totalBlocks; i += batchSize) {
            int end = Math.min(i + batchSize, totalBlocks);
            Block[] batchBlocks = allBlocks.subList(i, end).toArray(new Block[0]);
            batches.add(new BatchedBlockUpdate(batchBlocks, consumer));
        }
    }

    /**
     * Prepare batches for moving the bounding box to a new location.
     * @param newLoc the new location to move the bounding box to
     * @param undoOperation the operations to undo or modify previous block changes
     * @param consumer the operation to perform on each block in the new location
     */
    public void prepareMoveBatch(Location newLoc, Consumer<Block> undoOperation, Consumer<Block> consumer) {
        // First, schedule undo operations for existing batches
        this.prepareBatches(undoOperation, true);

        this.boundingBox = this.boundingBox.move(newLoc.x(), newLoc.y(), newLoc.z());
        this.prepareBatches(consumer, false);
    }


    /**
     * Schedule the execution of the prepared batches over the defined tick intervals.
     * @return a CompletableFuture that completes when all batches have been executed
     */
    public CompletableFuture<Void> scheduleBatches() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        for (int i = 0; i < this.batches.size(); i++) {
            BatchedBlockUpdate batch = this.batches.get(i);
            long delay = (long) i * this.ticksInterval;
            final int index = i;

            batch.schedule(delay, () -> {
                if (index == this.batches.size() - 1) {
                    future.complete(null);
                }
                // remove from batches list
            });
        }
        return future;
    }

    public void stopAll() {
        for (BatchedBlockUpdate batch : this.batches) {
            if (batch.task != null) {
                batch.task.cancel();
            }
        }
    }

    public void undoAll(Consumer<Block> undoOperation) {
        long delay = 0;
        for (BatchedBlockUpdate batch : this.batches) {
            batch.setConsumer(undoOperation);
            batch.schedule(delay, () -> {});
            delay += this.ticksInterval;
        }
    }



    public static class BatchedBlockUpdate {
        private final Block[] blocks;
        @Setter
        private Consumer<Block> consumer;
        private BukkitTask task;
        @Getter
        private boolean completed = false;

        public BatchedBlockUpdate(Block[] blocks, Consumer<Block> consumer) {
            this.blocks = blocks;
            this.consumer = consumer;
        }

        public void schedule(long delay, Runnable whenComplete) {
            this.task = Executors.delayedSync(delay, () -> {
                long start = System.currentTimeMillis();
                Logging.log("Starting execution of batched block update for " + blocks.length + " blocks.");
                for (Block block : blocks) {
                    this.consumer.accept(block);
                }
                Executors.runAsync(whenComplete);
                this.completed = true;
                Logging.log("Completed execution of batched block update in " + (System.currentTimeMillis() - start) + " ms.");
            });
        }
    }
}
