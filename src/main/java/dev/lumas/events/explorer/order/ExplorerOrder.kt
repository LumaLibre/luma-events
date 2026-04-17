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
    private val worldsProvider: () -> List<String>,
    val eventClass: Class<T>,
    val icon: Material,
    biome: PaleSideBiome? = null,
    val handler: ExplorerOrderEventHandler<T>
) : ContainerReflective() {

    val worlds: List<String> get() = worldsProvider()
    val isGlobal: Boolean get() = worlds.contains("*")
    val patterns: List<Regex> get() = worlds.mapNotNull {
        if (it == "*") null else Regex(it)
    }

    val biome: PaleSideBiome? by lazy {
        biome?.simplify(this)
    }

    constructor(name: String, objective: String, quantity: Int, souls: Int, world: () -> List<String>, eventClass: KClass<T>, icon: Material, biome: PaleSideBiome? = null, handler: ExplorerOrderEventHandler<T>) : this(
        name,
        objective,
        quantity,
        souls,
        world,
        eventClass.java,
        icon,
        biome,
        handler
    )

    fun matches(world: String): Boolean {
        return isGlobal || patterns.any { it.matches(world) }
    }
}