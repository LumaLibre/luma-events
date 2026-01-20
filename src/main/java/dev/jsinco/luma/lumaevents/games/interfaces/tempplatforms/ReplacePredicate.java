package dev.jsinco.luma.lumaevents.games.interfaces.tempplatforms;

import org.bukkit.block.Block;

@FunctionalInterface
public interface ReplacePredicate {
    boolean canReplace(Block block);
}
