package dev.lumas.events.explorer.containers

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ContainerType(val value: KClass<*>)
