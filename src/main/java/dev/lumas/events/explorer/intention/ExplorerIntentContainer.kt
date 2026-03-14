package dev.lumas.events.explorer.intention

import dev.lumas.events.explorer.containers.Container
import dev.lumas.events.explorer.containers.ContainerType
import dev.lumas.events.explorer.containers.ExplorerRegistry

@ContainerType(ExplorerIntent::class)
abstract class ExplorerIntentContainer : Container<ExplorerIntent<*>>() {

    override fun registry(): ExplorerRegistry<ExplorerIntent<*>> {
        return ExplorerIntentRegistry
    }
}