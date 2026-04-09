package dev.lumas.events.explorer.intention

import dev.lumas.events.explorer.containers.Container
import dev.lumas.events.explorer.containers.ContainerType

@ContainerType(ExplorerIntent::class)
abstract class ExplorerIntentContainer : Container<ExplorerIntent<*>>(ExplorerIntentRegistry)