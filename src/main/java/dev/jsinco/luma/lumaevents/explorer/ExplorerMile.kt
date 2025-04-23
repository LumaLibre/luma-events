package dev.jsinco.luma.lumaevents.explorer

typealias ExplorerMileEventHandler<T> = (event: T, levelSnapshot: ExplorerMileLevelSnapshot, data: MutableMap<String, Any>) -> Unit

open class ExplorerMile<T>(
    val title: String,
    val desc: String,
    //val objective: String = "No objective written...",
    val quantity: Int = 1,
    val levels: Int = 1,
    val levelMultiplier: Double = 1.0,
    val eventClass: Class<T>,
    val handler: ExplorerMileEventHandler<T>,
) {

    var FIELD_NAME: String? = null

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