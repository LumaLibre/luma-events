package dev.lumas.events.explorer.order

import dev.lumas.events.explorer.containers.ExplorerRegistry

object ExplorerOrderRegistry : ExplorerRegistry<ExplorerOrder<*>>() {
    @JvmStatic
    fun jvmRegister(container: ExplorerOrderContainer) = register(container)

    @JvmStatic
    fun jvmUnifiedMap() = unifiedMap()

    @JvmStatic
    fun jvmUnifiedValues() = unifiedValues()

    @JvmStatic
    fun jvmUnifiedValueOf(name: String) = unifiedValueOf(name)

    @JvmStatic
    fun jvmUnifiedFromString(name: String) = unifiedFromString(name)
}