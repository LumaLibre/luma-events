package dev.lumas.events.trickortreat

import dev.lumas.events.utility.effects.SoundDefinition
import org.bukkit.Location
import org.bukkit.Sound
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type
import java.util.function.Predicate

@ConfigSerializable
class KnockSettings(
    var sound: SoundDefinition = SoundDefinition(Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 0.5f, 1f, null),
    var windowTicks: Long = 60,
    var answerDelayTicks: Long = 20,
    var groupRadius: Double = 18.0,
)

@ConfigSerializable
class LevelSettings(var minKnocks: Int = 1, var chance: Double = 0.0)

@ConfigSerializable
class KnockLevels(
    var mild: LevelSettings = LevelSettings(1, 0.15),
    var medium: LevelSettings = LevelSettings(4, 0.3),
    var aggressive: LevelSettings = LevelSettings(8, 0.6),
) {
    operator fun get(level: KnockLevel) = when (level) {
        KnockLevel.MILD -> mild
        KnockLevel.MEDIUM -> medium
        KnockLevel.AGGRESSIVE -> aggressive
    }
}

@ConfigSerializable
class NpcSettings(
    var id: Int? = null,
    var hideWhileClosed: Boolean = false,
    var standby: Location? = null,
    var inside: Location? = null,
    var outside: Location? = null,
    var openAt: List<String>? = null,
) {
    fun toDoorkeeper(extraOpenAt: List<String>? = null) = Doorkeeper(
        id,
        standby?.toBlockCenter(),
        inside?.toBlockCenter(),
        outside?.toBlockCenter(),
        hideWhileClosed,
        (openAt.orEmpty() + extraOpenAt.orEmpty()).toDialoguePoints(),
    )

    companion object {
        fun of(keeper: Doorkeeper, withOpenAt: Boolean) = keeper.copy(openAt = if (withOpenAt) keeper.openAt else emptySet())
            .takeIf { it != Doorkeeper() }
            ?.let { NpcSettings(it.npc, it.hideWhileClosed, it.standby, it.inside, it.outside, it.openAt.toStrings()) }

        fun Location.toBlockCenter() = Location(world, blockX + 0.5, blockY.toDouble(), blockZ + 0.5, yaw, pitch)
    }
}

@ConfigSerializable
class FlowSettings(
    var door: Door? = null,
    var conversations: Map<String, String>? = null,
    var npc: NpcSettings? = null,
    var openAt: List<String>? = null,
    var npcs: Map<String, NpcSettings>? = null,
) {
    fun toFlow(name: String): Flow {
        val default = (npc ?: NpcSettings()).toDoorkeeper(openAt).takeIf { it != Doorkeeper() }
        return Flow(
            name,
            door,
            KnockLevel.entries.mapNotNull { level -> conversations?.get(level.key)?.let { level to it } }.toMap(),
            listOfNotNull(default?.let { Flow.DEFAULT_KEEPER to it }).toMap() + npcs.orEmpty().mapValues { it.value.toDoorkeeper() },
        )
    }

    companion object {
        fun of(flow: Flow): FlowSettings {
            val default = flow.doorkeepers[Flow.DEFAULT_KEEPER]
            return FlowSettings(
                flow.door,
                flow.conversations.mapKeys { it.key.key }.ifEmpty { null },
                default?.let { NpcSettings.of(it, withOpenAt = false) },
                default?.openAt?.toStrings(),
                (flow.doorkeepers - Flow.DEFAULT_KEEPER).mapNotNull { (name, keeper) -> NpcSettings.of(keeper, withOpenAt = true)?.let { name to it } }
                    .toMap().ifEmpty { null },
            )
        }
    }
}

private fun List<String>.toDialoguePoints() = mapNotNull(DialoguePoint::parse).toSet()

private fun Set<DialoguePoint>.toStrings() = map(DialoguePoint::toString).ifEmpty { null }

// Same world,x,y,z format as locations, but doesn't need the world to be loaded
object DoorSerializer : ScalarSerializer<Door>(Door::class.java) {

    override fun deserialize(type: Type, obj: Any): Door {
        val parts = obj.toString().split(',').map(String::trim)
        val coordinates = parts.drop(1).mapNotNull(String::toIntOrNull)
        if (parts.size != 4 || coordinates.size != 3) throw SerializationException("Invalid door format: $obj")
        return Door(parts[0], coordinates[0], coordinates[1], coordinates[2])
    }

    override fun serialize(item: Door, typeSupported: Predicate<Class<*>>) = "${item.world},${item.x},${item.y},${item.z}"
}
