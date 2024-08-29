package dev.jsinco.lumacarnival.games

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class TaskAttributes(
    val taskTime: Long,
    val async: Boolean
)
