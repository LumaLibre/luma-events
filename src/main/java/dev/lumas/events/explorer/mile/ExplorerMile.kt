package dev.lumas.events.explorer.mile

import dev.lumas.events.explorer.containers.ContainerReflective
import kotlin.reflect.KClass

/**
 * Kotlin typealias for handling an Explorer Mile's listened to event.
 */
typealias ExplorerMileEventHandler<T> = (event: T, levelSnapshot: ExplorerMileLevelSnapshot, data: MutableMap<String, Any>) -> Unit

/**
 * Represents an "Explorer Mile", which is a specific task or achievement that players can complete by performing certain actions in the game.
 * Each mile has a title, description, quantity requirement, level progression, and an associated event that players must trigger to progress towards completing the mile.
 */
open class ExplorerMile<T : Any>(
    val title: String,
    val desc: String,
    //val objective: String = "No objective written...",
    val quantity: Int = 1,
    val levels: Int = 1,
    val levelMultiplier: Double = 1.0,
    val eventClass: Class<T>,
    val handler: ExplorerMileEventHandler<T>,
) : ContainerReflective() {

    /**
     * Alternative [KClass] constructor for [eventClass].
     */
    constructor(title: String, desc: String, quantity: Int, levels: Int, levelMultiplier: Double, eventClass: KClass<T>, handler: ExplorerMileEventHandler<T>) : this(
        title,
        desc,
        quantity,
        levels,
        levelMultiplier,
        eventClass.java,
        handler
    );


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExplorerMile<*>

        return FIELD_NAME == other.FIELD_NAME
    }

    override fun hashCode(): Int {
        return FIELD_NAME?.hashCode() ?: 0
    }

    override fun toString(): String {
        return FIELD_NAME ?: "Unknown"
    }
}