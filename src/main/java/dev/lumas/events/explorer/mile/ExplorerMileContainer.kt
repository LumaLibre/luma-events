package dev.lumas.events.explorer.mile

import dev.lumas.events.explorer.containers.Container
import dev.lumas.events.explorer.containers.ContainerType

@ContainerType(ExplorerMile::class)
abstract class ExplorerMileContainer : Container<ExplorerMile<*>>(ExplorerMileRegistry) {

    // Lazy init for external plugins and problematic ExplorerMiles
    protected inline fun <T> safeLazy(crossinline block: () -> T?): Lazy<T?> {
        return lazy {
            try {
                block()
            } catch (_: ClassNotFoundException) {
                null
            } catch (_: NoClassDefFoundError) {
                null
            } catch (e: Exception) {
                LOGGER.error("Failed to initialize 'safeLazy' ExplorerMile", e)
                null
            }
        }
    }
}