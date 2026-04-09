package dev.lumas.events.explorer.order

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import dev.lumas.core.util.ContextLogger
import dev.lumas.events.obj.EventPlayer
import org.bukkit.World

class ActiveOrder(
    val explorerOrder: ExplorerOrder<*>,
    var currentQuantity: Int,
    var completed: Boolean
) {

    companion object {
        private val LOGGER = ContextLogger.getLogger()
    }

    fun apply(world: World, event: Any, eventPlayer: EventPlayer) {
        if (explorerOrder.matches(world.name)) {
            val completion = OrderCompletion(explorerOrder, currentQuantity, explorerOrder.quantity)
            if (!this.completed) {
                @Suppress("UNCHECKED_CAST")
                val handler = explorerOrder.handler as Function2<Any, OrderCompletion, Unit>
                handler(event, completion)
                currentQuantity = completion.currentQuantity.coerceAtMost(completion.maxQuantity)

                if (completion.isCompleted()) {
                    this.completed = true
                    completion.completionEffects(eventPlayer)
                }
            }
        }
    }


    class GsonTypeAdapter : TypeAdapter<ActiveOrder>() {
        override fun write(out: JsonWriter, aside: ActiveOrder?) {
            if (aside == null) {
                return
            }
            out.beginObject()
            out.name("orderImplName").value(aside.explorerOrder.FIELD_NAME)
            out.name("currentQuantity").value(aside.currentQuantity)
            out.name("completed").value(aside.completed)
            out.endObject()
        }

        override fun read(reader: JsonReader): ActiveOrder? {
            val asideImplName: String
            var currentQuantity = 0
            var completed = false

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

            return ActiveOrder(aside, currentQuantity, completed)
        }
    }
}