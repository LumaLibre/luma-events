package dev.lumas.events.explorer.custom

import org.bukkit.Material
import org.bukkit.block.Block

/**
 * Thread safe
 */
class BlockPlacedExplorerEvent(
    val type: Material
) {
    constructor(block: Block) : this(block.type)
}