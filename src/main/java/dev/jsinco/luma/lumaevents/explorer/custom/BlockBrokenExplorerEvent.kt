package dev.jsinco.luma.lumaevents.explorer.custom

import org.bukkit.Material
import org.bukkit.block.Block

// More props can be added as needed.
class BlockBrokenExplorerEvent(
    val type: Material
) {

    constructor(block: Block): this(block.type)
}