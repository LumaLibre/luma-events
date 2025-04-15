package dev.jsinco.luma.lumaevents.explorer.custom

import org.bukkit.Material
import org.bukkit.block.Block

class BlockPlacedExplorerEvent(
    val type: Material
) {

    constructor(block: Block): this(block.type)
}