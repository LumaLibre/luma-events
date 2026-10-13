package dev.lumas.events.games.logic.copycat;

import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.Nullable;

public record Placed(BlockData data, @Nullable TileDetail detail) {

    public static final Placed AIR = new Placed(ArenaBlocks.AIR, null);

    public static Placed of(Block block) {
        BlockData data = block.getBlockData();
        return new Placed(data, isTile(data) ? TileDetail.of(block.getState()) : null);
    }

    static boolean isTile(BlockData data) {
        String name = data.getMaterial().name();
        return name.endsWith("_HEAD") || name.endsWith("_SKULL") || name.endsWith("_BANNER"); // TODO: use tags
    }

    public boolean isAir() {
        return data.getMaterial().isAir();
    }
}
