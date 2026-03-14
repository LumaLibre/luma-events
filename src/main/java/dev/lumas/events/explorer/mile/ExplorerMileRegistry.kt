package dev.lumas.events.explorer.mile

import dev.lumas.events.explorer.containers.ExplorerRegistry

object ExplorerMileRegistry : ExplorerRegistry<ExplorerMile<*>>() {
    @JvmStatic
    fun jvmRegister(container: ExplorerMileContainer) = register(container)

    @JvmStatic
    fun jvmUnifiedMap() = unifiedMap()

    @JvmStatic
    fun jvmUnifiedValues() = unifiedValues()

    @JvmStatic
    fun jvmUnifiedValueOf(name: String) = unifiedValueOf(name)

    @JvmStatic
    fun jvmUnifiedFromString(name: String) = unifiedFromString(name)
}
