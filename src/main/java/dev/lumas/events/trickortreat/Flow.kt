package dev.lumas.events.trickortreat

import org.bukkit.Location
import org.bukkit.World
import kotlin.math.abs

data class Door(val world: String, val x: Int, val y: Int, val z: Int) {

    fun matches(location: Location) =
        location.world.name == world && location.blockX == x && location.blockY == y && location.blockZ == z

    override fun toString() = "$world ($x, $y, $z)"

    fun location(world: World) = Location(world, x + 0.5, y + 0.5, z + 0.5)

    companion object {
        fun at(location: Location) = Door(location.world.name, location.blockX, location.blockY, location.blockZ)
    }
}

enum class KnockLevel {
    MILD, MEDIUM, AGGRESSIVE;

    val key = name.lowercase()

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key.lowercase() }
    }
}

data class DialoguePoint(val file: String, val conversation: String, val dialogue: String) {

    override fun toString() = "$file:$conversation:$dialogue"

    companion object {
        fun parse(value: String) = value.split(':').takeIf { it.size == 3 }?.let { DialoguePoint(it[0], it[1], it[2]) }
    }
}

data class Doorkeeper(
    val npc: Int? = null,
    val standby: Location? = null,
    val inside: Location? = null,
    val outside: Location? = null,
    val hideWhileClosed: Boolean = false,
    val openAt: Set<DialoguePoint> = emptySet(),
) {
    val isComplete get() = npc != null && standby != null && inside != null && outside != null

    val isActive get() = isComplete && openAt.isNotEmpty()
}

data class Flow(
    val name: String,
    val door: Door? = null,
    val conversations: Map<KnockLevel, String> = emptyMap(),
    val doorkeepers: Map<String, Doorkeeper> = emptyMap(),
    val groupRadius: Double? = null,
    val groupMaxYDiff: Double? = null,
) {

    val isComplete get() = door != null && conversations.isNotEmpty()

    fun doorkeeper(name: String) = doorkeepers[name] ?: Doorkeeper()

    fun withDoorkeeper(name: String, keeper: Doorkeeper) =
        copy(doorkeepers = if (keeper == Doorkeeper()) doorkeepers - name else doorkeepers + (name to keeper))

    fun opensAt(point: DialoguePoint) = doorkeepers.values.any { it.isActive && point in it.openAt }

    fun watches(file: String) = doorkeepers.values.any { keeper -> keeper.openAt.any { it.file == file } }

    // Falls back to the closest configured level, preferring the milder one on ties
    fun conversationFor(level: KnockLevel) = KnockLevel.entries
        .sortedBy { abs(it.ordinal - level.ordinal) * 2 + if (it > level) 1 else 0 }
        .firstNotNullOfOrNull(conversations::get)

    companion object {
        const val DEFAULT_KEEPER = "default"
    }
}
