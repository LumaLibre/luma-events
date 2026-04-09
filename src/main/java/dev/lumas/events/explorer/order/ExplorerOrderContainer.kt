package dev.lumas.events.explorer.order

import dev.lumas.events.explorer.containers.Container
import dev.lumas.events.explorer.containers.ContainerType

@ContainerType(ExplorerOrder::class)
abstract class ExplorerOrderContainer : Container<ExplorerOrder<*>>(ExplorerOrderRegistry)