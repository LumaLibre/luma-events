package dev.jsinco.luma.lumaevents.explorer

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import dev.jsinco.luma.lumaevents.EventMain
import dev.jsinco.luma.lumaevents.explorer.constants.ExplorerMiles
import dev.jsinco.luma.lumaevents.npc.constants.TutorialSection
import dev.jsinco.luma.lumaevents.obj.DialogueText
import dev.jsinco.luma.lumaevents.obj.EventPlayer
import dev.jsinco.luma.lumaevents.tokens.TokenExchanging
import dev.jsinco.luma.lumaevents.utility.Util
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player

typealias ExplorerMileLevelSnapshotModifier = (levelSnapshot: ExplorerMileLevelSnapshot) -> Unit

class ActiveExplorerMile(
    val mile: ExplorerMile<*>,
    var currentQuantity: Int,
    var currentLevel: Int,
) {

    companion object {
        private const val TOKENS_PER_LEVEL = 8
    }

    constructor(mile: ExplorerMile<*>) : this(mile, 0, 0)

    private var data: MutableMap<String, Any> = mutableMapOf()
    fun data(): MutableMap<String, Any> = data


    fun <T> apply(event: T, eventPlayer: EventPlayer) {
        val levelSnapshot = ExplorerMileLevelSnapshot(
            maxQuantity = mile.quantity,
            currentQuantity = this.currentQuantity,
            maxLevels = mile.levels,
            currentLevel = this.currentLevel,
            levelMultiplier = mile.levelMultiplier
        )
        if (levelSnapshot.isCompleted()) {
            return
        }

        @Suppress("UNCHECKED_CAST")
        (mile.handler as ExplorerMileEventHandler<T>)(event, levelSnapshot, data)


        this.currentQuantity = levelSnapshot.currentQuantity
        this.currentLevel = levelSnapshot.currentLevel

        if (!levelSnapshot.tryProgressLevel()) {
            return
        }

        var multiplier = 1.0
        if (levelSnapshot.currentLevel > 1) {
            multiplier = 1.5
        }

        // reward
      val amount = TOKENS_PER_LEVEL * levelSnapshot.currentLevel * multiplier
//        val player = eventPlayer.player
//        TokenExchanging.give(
//            player,
//            TokenExchanging.TokenType.CARROT,
//            amount.toInt()
//        )
      eventPlayer.sendMessage("DEBUG (EXPLORERMILES): You have received $amount tokens for leveling up your ${mile.title}!")
//        //Bukkit.getScheduler().runTask()
//
//        // TODO: Needs handling of rewards testing
//        val dialogueText = DialogueText(eventPlayer)
//        dialogueText.queueText("You've leveled up your ${mile.title} to level ${levelSnapshot.currentLevel}!")
//        if (levelSnapshot.isCompleted()) {
//            dialogueText.queueText("You completed a mile! (${mile.title})")
//            if (!eventPlayer.hasCompletedTutorialSection(TutorialSection.MILES_COMMAND) && eventPlayer.completedExplorerMiles >= 5) {
//                TutorialSection.MILES_COMMAND.completeTutorial(eventPlayer, dialogueText, null)
//            } else {
//                dialogueText.sendQueuedText();
//            }
//        }
//
//        // little noise :)
//        Bukkit.getScheduler().runTask(EventMain.getInstance(), Runnable {
//            player?.playSound(player.location, Sound.ENTITY_FIREWORK_ROCKET_BLAST_FAR, 1f, 1f)
//        })
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

    fun playMilesUnlockEffect(eventPlayer: EventPlayer) {
        val player = eventPlayer.player
        Bukkit.getScheduler().runTask(EventMain.getInstance(), Runnable {
            player?.playSound(player.location, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1f, 1f)
        })
        val dialogueText = DialogueText(eventPlayer)
        dialogueText.ifAbsentColor = NamedTextColor.YELLOW
        dialogueText.queueText("You've unlocked a new Explorer Mile! (${mile.title})")
        dialogueText.sendQueuedText()
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