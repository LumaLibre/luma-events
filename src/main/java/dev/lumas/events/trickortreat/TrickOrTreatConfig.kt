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
    var standby: Location? = null,
    var inside: Location? = null,
    var outside: Location? = null,
    var hideWhileClosed: Boolean = false,
) {
    fun toDoorkeeper() = Doorkeeper(id, standby?.toBlockCenter(), inside?.toBlockCenter(), outside?.toBlockCenter(), hideWhileClosed)

    companion object {
        fun of(keeper: Doorkeeper) = keeper.takeIf { it != Doorkeeper() }
            ?.let { NpcSettings(it.npc, it.standby, it.inside, it.outside, it.hideWhileClosed) }

        fun Location.toBlockCenter() = Location(world, blockX + 0.5, blockY.toDouble(), blockZ + 0.5, yaw, pitch)
    }
}

@ConfigSerializable
class FlowSettings(
    var door: Door? = null,
    var conversations: Map<String, String>? = null,
    var npc: NpcSettings? = null,
    var openAt: List<String>? = null,
) {
    fun toFlow(name: String) = Flow(
        name,
        door,
        KnockLevel.entries.mapNotNull { level -> conversations?.get(level.key)?.let { level to it } }.toMap(),
        npc?.toDoorkeeper() ?: Doorkeeper(),
        openAt.orEmpty().mapNotNull(DialoguePoint::parse).toSet(),
    )

    companion object {
        fun of(flow: Flow) = FlowSettings(
            flow.door,
            flow.conversations.mapKeys { it.key.key }.ifEmpty { null },
            NpcSettings.of(flow.doorkeeper),
            flow.openAt.map(DialoguePoint::toString).ifEmpty { null },
        )
    }
}

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
