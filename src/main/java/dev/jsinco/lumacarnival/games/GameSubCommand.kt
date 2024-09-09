package dev.jsinco.lumacarnival.games

import java.lang.reflect.Method

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class GameSubCommand(
    val name: String,
    val permission: String,
    val playerOnly: Boolean = false,
    val executeAsync: Boolean = false
)
