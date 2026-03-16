package dev.lumas.events.explorer.containers

import dev.lumas.core.util.ContextLogger

abstract class ExplorerRegistry<T : ContainerReflective> {

    companion object {
        private val LOGGER = ContextLogger.getLogger()
    }
    private val containers: MutableSet<Container<T>> = mutableSetOf()

    /**
     * Register a [Container] to be included in the unified map and value lookups.
     */
    internal fun register(container: Container<T>) {
        containers.add(container)
        LOGGER.info("Registered explorer container: ${container.javaClass.simpleName}")
    }

    /**
     * Combine all container maps into one.
     * @return a map of all [T]s in all registered containers, keyed by their field name.
     * @throws IllegalStateException if duplicate field names are found across containers.
     */
    fun unifiedMap(): Map<String, T> {
        val combined = LinkedHashMap<String, T>()
        containers.forEach { container ->
            container.asMap().forEach { (fieldName, obj) ->
                check(fieldName !in combined) { "Duplicate container object field name: '$fieldName'" }
                combined[fieldName] = obj
            }
        }
        return combined
    }

    /**
     * Combine all container values into one.
     * @return a collection of all [T]s in all registered containers.
     */
    fun unifiedValues(): Collection<T> {
        return containers.flatMap { it.values() }
    }

    /**
     * Search for an [T] by name across all registered containers.
     * @param name the field name of the [T] to search for.
     * @return the [T] with the given name, or null if not
     */
    fun unifiedValueOf(name: String): T? {
        return containers.firstNotNullOfOrNull { it.valueOf(name) }
    }

    /**
     * Alias for [unifiedValueOf]
     */
    fun unifiedFromString(name: String) = unifiedValueOf(name)
}