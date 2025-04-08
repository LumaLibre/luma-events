package dev.jsinco.luma.lumaevents.explorer

typealias ExplorerMileEventHandler<T> = (event: T, levelSnapshot: ExplorerMileLevelSnapshot, data: MutableMap<String, Any>) -> Unit

open class ExplorerMile<T>(
    val title: String,
    val desc: String,
    val quantity: Int = 1,
    val levels: Int = 1,
    val levelMultiplier: Double = 1.0,
    val eventClass: Class<T>,
    val handler: ExplorerMileEventHandler<T>,
) {

    var FIELD_NAME: String? = null

}