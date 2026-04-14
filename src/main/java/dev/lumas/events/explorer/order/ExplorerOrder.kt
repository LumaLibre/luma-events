package dev.lumas.events.explorer.order

import dev.lumas.events.explorer.containers.ContainerReflective
import org.bukkit.Material
import kotlin.reflect.KClass

typealias ExplorerOrderEventHandler<T> = (event: T, completion: ExplorerOrderCompletion) -> Unit

class ExplorerOrder<T : Any>(
    val name: String,
    val objective: String,
    val quantity: Int,
    val souls: Int,
    val worlds: List<String>,
    val eventClass: Class<T>,
    val icon: Material,
    val handler: ExplorerOrderEventHandler<T>
) : ContainerReflective() {

    val isGlobal: Boolean = worlds.contains("*")
    val patterns: List<Regex> = worlds.mapNotNull {
        if (it == "*") null else Regex(it)
    }

    constructor(name: String, objective: String, quantity: Int, souls: Int, world: List<String>, eventClass: KClass<T>, icon: Material, handler: ExplorerOrderEventHandler<T>) : this(
        name,
        objective,
        quantity,
        souls,
        world,
        eventClass.java,
        icon,
        handler
    )

    fun matches(world: String): Boolean {
        return isGlobal || patterns.any { it.matches(world) }
    }
}