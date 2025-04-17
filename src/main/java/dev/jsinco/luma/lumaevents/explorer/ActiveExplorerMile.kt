package dev.jsinco.luma.lumaevents.explorer

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import dev.jsinco.luma.lumaevents.explorer.constants.ExplorerMiles
import dev.jsinco.luma.lumaevents.utility.Util

typealias ExplorerMileLevelSnapshotModifier = (levelSnapshot: ExplorerMileLevelSnapshot) -> Unit

class ActiveExplorerMile(
    val mile: ExplorerMile<*>,
    var currentQuantity: Int,
    var currentLevel: Int,
) {

    constructor(mile: ExplorerMile<*>) : this(mile, 0, 0)

    private var data: MutableMap<String, Any> = mutableMapOf()
    fun data(): MutableMap<String, Any> = data


    fun <T> apply(event: T) {
        val levelSnapshot = ExplorerMileLevelSnapshot(
            maxQuantity = mile.quantity,
            currentQuantity = this.currentQuantity,
            maxLevels = mile.levels,
            currentLevel = this.currentLevel,
            levelMultiplier = mile.levelMultiplier
        )
        if (levelSnapshot.isCompleted()) {
            Util.log("Skipping completed ExplorerMile: $this")
            return
        }

        @Suppress("UNCHECKED_CAST")
        (mile.handler as ExplorerMileEventHandler<T>)(event, levelSnapshot, data)


        levelSnapshot.tryProgressLevel()
        this.currentQuantity = levelSnapshot.currentQuantity
        this.currentLevel = levelSnapshot.currentLevel

        // TODO: Needs handling of rewards

    }

    fun modifyLevelSnapshot(explorerSnapshotModifier: ExplorerMileLevelSnapshotModifier) {
        val levelSnapshot = ExplorerMileLevelSnapshot(
            maxQuantity = mile.quantity,
            currentQuantity = this.currentQuantity,
            maxLevels = mile.levels,
            currentLevel = this.currentLevel,
            levelMultiplier = mile.levelMultiplier
        )
        explorerSnapshotModifier(levelSnapshot)

        this.currentQuantity = levelSnapshot.currentQuantity
        this.currentLevel = levelSnapshot.currentLevel
    }

    fun getUnchangeableLevelSnapshot(): ExplorerMileLevelSnapshot {
        return ExplorerMileLevelSnapshot(
            maxQuantity = mile.quantity,
            currentQuantity = this.currentQuantity,
            maxLevels = mile.levels,
            currentLevel = this.currentLevel,
            levelMultiplier = mile.levelMultiplier
        )
    }

    fun hasProgress(): Boolean {
        return currentQuantity > 0 || currentLevel > 0
    }

    override fun toString(): String {
        return "ActiveExplorerMile(mile=$mile, currentQuantity=$currentQuantity, data=$data)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ActiveExplorerMile

        return mile == other.mile
    }


    class GsonTypeAdapter : TypeAdapter<ActiveExplorerMile>() {
        override fun write(writer: JsonWriter, activeExplorerMile: ActiveExplorerMile) {
            val mileImplName = activeExplorerMile.mile.FIELD_NAME
                ?: throw IllegalArgumentException("${activeExplorerMile.mile.FIELD_NAME} must not be null")

            writer.beginObject()
            writer.name("mileImplName").value(mileImplName)
            writer.name("currentQuantity").value(activeExplorerMile.currentQuantity)
            writer.name("currentLevel").value(activeExplorerMile.currentLevel)

            // Serialize the data map
            if (activeExplorerMile.data.isNotEmpty()) {
                writer.name("data")
                writer.beginObject()  // Start the object for the data map
                for ((key, value) in activeExplorerMile.data()) {
                    writer.name(key)

                    when (value) {
                        is String -> writer.value(value)
                        is Number -> writer.value(value)
                        is Boolean -> writer.value(value)
                        is List<*> -> {  // Handle List<Any>
                            writer.beginArray()  // Start the array for the list
                            for (item in value) {
                                when (item) {
                                    is String -> writer.value(item)
                                    is Number -> writer.value(item)
                                    is Boolean -> writer.value(item)
                                    else -> writer.value(item.toString()) // Fallback for unsupported types
                                }
                            }
                            writer.endArray()  // Close the array
                        }
                        else -> writer.value(value.toString()) // Fallback for unsupported types
                    }
                }
                writer.endObject()  // End the data object
            }
            writer.endObject()  // End the main object
        }

        override fun read(reader: JsonReader): ActiveExplorerMile? {
            var mileImplName: String? = null
            var currentQuantity = 0
            var currentLevel = 0
            val dataMap: MutableMap<String, Any> = mutableMapOf()

            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "mileImplName" -> {
                        mileImplName = reader.nextString()
                    }

                    "currentQuantity" -> {
                        currentQuantity = reader.nextInt()
                    }
                    "currentLevel" -> {
                        currentLevel = reader.nextInt()
                    }
                    "data" -> {
                        reader.beginObject()
                        while (reader.hasNext()) {
                            val key = reader.nextName()
                            val value: Any = when (reader.peek()) {
                                JsonToken.STRING -> reader.nextString()
                                JsonToken.NUMBER -> reader.nextDouble()
                                JsonToken.BOOLEAN -> reader.nextBoolean()
                                JsonToken.BEGIN_ARRAY -> {
                                    val list = mutableListOf<Any>()
                                    reader.beginArray()
                                    while (reader.hasNext()) {
                                        list.add(when (reader.peek()) {
                                            JsonToken.STRING -> reader.nextString()
                                            JsonToken.NUMBER -> reader.nextDouble()
                                            JsonToken.BOOLEAN -> reader.nextBoolean()
                                            else -> reader.nextString() // Fallback for unsupported types
                                        })
                                    }
                                    reader.endArray()
                                    list
                                }
                                else -> reader.nextString() // Fallback for unsupported types
                            }
                            dataMap[key] = value
                        }
                        reader.endObject()
                    }
                }
            }
            reader.endObject()

            val explorerMile = ExplorerMiles.asMap()[mileImplName] ?: run {
                Util.log("Missing an ExplorerMile implementation for $mileImplName, was it removed?")
                return null
            }

            val activeExplorerMile = ActiveExplorerMile(explorerMile, currentQuantity, currentLevel)
            if (dataMap.isNotEmpty()) {
                activeExplorerMile.data = dataMap
            }
            return activeExplorerMile
        }
    }

}