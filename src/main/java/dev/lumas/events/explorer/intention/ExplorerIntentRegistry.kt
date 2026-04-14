package dev.lumas.events.explorer.intention

import dev.lumas.events.explorer.containers.ExplorerRegistry

/**
 * @see ExplorerRegistry
 * @see ExplorerIntent
 */
object ExplorerIntentRegistry : ExplorerRegistry<ExplorerIntent<*>>() {
    @JvmStatic
    fun jvmRegister(container: ExplorerIntentContainer) = register(container)

    @JvmStatic
    fun jvmUnifiedMap() = unifiedMap()

    @JvmStatic
    fun jvmUnifiedValues() = unifiedValues()

    @JvmStatic
    fun jvmUnifiedValueOf(name: String) = unifiedValueOf(name)

    @JvmStatic
    fun jvmUnifiedFromString(name: String) = unifiedFromString(name)
}