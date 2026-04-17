package dev.lumas.events.explorer.intention

import dev.lumas.events.explorer.containers.ContainerReflective
import org.bukkit.Material
import org.bukkit.World
import kotlin.reflect.KClass

typealias ExplorerIntentEventHandler<T> = (event: T) -> Unit

/**
 * A similar derivative of the [dev.lumas.events.explorer.mile.ExplorerMile] class, but used for just
 * straight-up handling events in specific worlds to enforce certain actions.
 */
class ExplorerIntent<T : Any>(
    val title: String,
    val desc: String,
    private val worldsProvider: () -> List<String>,
    val eventClass: Class<T>,
    val icon: Material = Material.MAP,
    val handler: ExplorerIntentEventHandler<T>,
): ContainerReflective() {

    val worlds: List<String> get() = worldsProvider()
    val isGlobal: Boolean get() = worlds.contains("*")
    val patterns: List<Regex> get() = worlds.mapNotNull {
        if (it == "*") null else Regex(it)
    }


    constructor(title: String, desc: String, world: () -> List<String>, eventClass: KClass<T>, icon: Material, handler: ExplorerIntentEventHandler<T>) : this(
        title,
        desc,
        world,
        eventClass.java,
        icon,
        handler
    )

    /**
     * Checks if the given world matches any of the patterns defined for this intent.
     * @param world the name of the world to check
     * @return true if the world matches any of the patterns or this intent is global, false otherwise
     */
    fun matches(world: String): Boolean {
        return isGlobal || patterns.any { it.matches(world) }
    }

    /**
     * Applies this intent's handler to the given event if the world matches this intent's patterns.
     * @param world the world to check
     * @param event the event to handle
     */
    fun apply(world: World, event: T) {
        if (matches(world.name)) {
            handler(event)
        }
    }

    /**
     * Checks if the given event is an instance of this intent's event class, and if so, applies this intent's handler to it.
     * @param world the world to check
     * @param event the event to check and potentially handle
     */
    fun tryApply(world: World, event: Any) {
        if (eventClass.isInstance(event)) {
            @Suppress("UNCHECKED_CAST")
            (this as ExplorerIntent<Any>).apply(world, event)
        }
    }

    override fun toString(): String {
        return "ExplorerIntent(title='$title', desc='$desc', eventClass=$eventClass)"
    }
}