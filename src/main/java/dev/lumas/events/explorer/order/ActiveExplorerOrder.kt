package dev.lumas.events.explorer.order

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import dev.lumas.core.util.ContextLogger
import dev.lumas.events.model.EventPlayer
import org.bukkit.World

class ActiveExplorerOrder(
    val explorerOrder: ExplorerOrder<*>,
    var currentQuantity: Int,
    var completed: Boolean,
    var completedAt: Long
) {

    companion object {
        private val LOGGER = ContextLogger.getLogger()
    }

    fun apply(world: World, event: Any, eventPlayer: EventPlayer) {
        if (explorerOrder.matches(world.name)) {
            val completion = ExplorerOrderCompletion(explorerOrder, currentQuantity, explorerOrder.quantity)
            if (!this.completed) {
                @Suppress("UNCHECKED_CAST")
                val handler = explorerOrder.handler as Function2<Any, ExplorerOrderCompletion, Unit>
                handler(event, completion)
                currentQuantity = completion.currentQuantity.coerceAtMost(completion.maxQuantity)

                if (completion.isCompleted()) {
                    this.completed = true
                    this.completedAt = System.currentTimeMillis()
                    completion.completionEffects(eventPlayer)
                    eventPlayer.resortExplorerOrders()
                }
            }
        }
    }

    fun getImmutableCompletion() = ExplorerOrderCompletion(explorerOrder, currentQuantity, explorerOrder.quantity)

    class GsonTypeAdapter : TypeAdapter<ActiveExplorerOrder>() {
        override fun write(out: JsonWriter, aside: ActiveExplorerOrder?) {
            if (aside == null) {
                return
            }
            out.beginObject()
            out.name("orderImplName").value(aside.explorerOrder.FIELD_NAME)
            out.name("currentQuantity").value(aside.currentQuantity)
            out.name("completed").value(aside.completed)
            out.name("completedAt").value(aside.completedAt)
            out.endObject()
        }

        override fun read(reader: JsonReader): ActiveExplorerOrder? {
            val asideImplName: String
            var currentQuantity = 0
            var completed = false
            var completedAt = -1L

            when (reader.peek()) {
                JsonToken.STRING -> {
                    asideImplName = reader.nextString()
                }
                JsonToken.BEGIN_OBJECT -> {
                    reader.beginObject()
                    var name: String? = null
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "orderImplName" -> name = reader.nextString()
                            "currentQuantity" -> currentQuantity = reader.nextInt()
                            "completed" -> completed = reader.nextBoolean()
                            "completedAt" -> completedAt = reader.nextLong()
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                    asideImplName = name ?: return null
                }
                else -> {
                    reader.skipValue()
                    return null
                }
            }

            val aside = ExplorerOrderRegistry.unifiedValueOf(asideImplName) ?: run {
                LOGGER.info("Missing an ExplorerAside implementation for $asideImplName, was it removed?")
                return null
            }

            return ActiveExplorerOrder(aside, currentQuantity, completed, completedAt)
        }
    }
}