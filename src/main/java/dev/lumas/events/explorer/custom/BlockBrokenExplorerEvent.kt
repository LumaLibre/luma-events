package dev.lumas.events.explorer.custom

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block

// More props can be added as needed
/**
 * Thread safe
 */
class BlockBrokenExplorerEvent(
    val type: Material,
    val location: Location,
) {
    constructor(block: Block) : this(block.type, block.location)
}