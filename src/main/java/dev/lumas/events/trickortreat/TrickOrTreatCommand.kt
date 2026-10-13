package dev.lumas.events.trickortreat

import com.nexomc.nexo.api.NexoFurniture
import dev.lumas.core.annotation.Autowire
import dev.lumas.core.annotation.CommandMeta
import dev.lumas.core.annotation.Register
import dev.lumas.events.EventMain
import dev.lumas.events.commands.CommandManager
import dev.lumas.events.commands.CommandModule
import dev.lumas.events.trickortreat.NpcSettings.Companion.toBlockCenter
import dev.lumas.events.utility.Util
import net.citizensnpcs.api.CitizensAPI
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.util.logging.Level

@Register(value = [Autowire.SUBCOMMAND], requires = "Nexo")
@CommandMeta(
    name = "trickortreat",
    permission = TrickOrTreat.ADMIN,
    description = "Manage trick or treat flows",
    parent = CommandManager::class,
    usage = "/<command> trickortreat <door|conversation|npc|openat|delete|list|reload>",
)
class TrickOrTreatCommand : CommandModule {

    private val feature get() = TrickOrTreat.instance!!
    private val plugin get() = feature.plugin
    private val store get() = feature.store
    private val knocker get() = feature.knocker

    override fun execute(eventMain: EventMain, sender: CommandSender, label: String, args: Array<String>): Boolean {
        if (TrickOrTreat.instance == null) {
            sender.error("Trick or treat is not enabled. It requires Nexo and Interactions, check the console for errors.")
            return true
        }
        val command = "$label trickortreat"
        when (args.firstOrNull()?.lowercase()) {
            "door" -> door(sender, command, args.drop(1))
            "conversation" -> conversation(sender, command, args.drop(1))
            "npc" -> npc(sender, command, args.drop(1))
            "openat" -> openAt(sender, command, args.drop(1))
            "delete" -> delete(sender, command, args.getOrNull(1))
            "list" -> list(sender)
            "reload" -> reload(sender)
            else -> sender.error("Usage: /$command <${SUBCOMMANDS.joinToString("|")}>")
        }
        return true
    }

    override fun tabComplete(eventMain: EventMain, sender: CommandSender, args: Array<String>): List<String> {
        if (TrickOrTreat.instance == null || args.isEmpty()) return emptyList()
        val subcommand = args.first().lowercase()
        val options = when {
            args.size == 1 -> SUBCOMMANDS
            args.size == 2 && subcommand in listOf("door", "conversation", "npc", "openat", "delete") -> store.flows.keys.toList()
            args.size == 3 && subcommand == "conversation" -> KnockLevel.entries.map { it.key }
            args.size == 4 && subcommand == "conversation" -> InteractionsHook.conversations() + "reset"
            args.size in 3..5 && subcommand == "door" -> listOf("~")
            args.size == 3 && subcommand == "npc" -> NPC_OPTIONS
            args.size == 4 && subcommand == "npc" && args[2].equals("id", ignoreCase = true) && TrickOrTreat.citizensEnabled(plugin) -> CitizensAPI.getNPCRegistry().sorted().map { it.id.toString() }
            args.size == 3 && subcommand == "openat" -> listOf("add", "remove")
            args.size == 4 && subcommand == "openat" -> openAtSuggestions(args[1], args[2])
            else -> emptyList()
        }
        return options.filter { it.startsWith(args.last(), ignoreCase = true) }
    }

    private fun door(sender: CommandSender, command: String, args: List<String>) {
        val player = sender as? Player ?: return sender.error("Only players can set doors.")
        if (args.size != 1 && args.size != 4) return sender.error("Usage: /$command door <flow> [x y z]")
        val name = args.first()
        if (!NAME.matches(name)) return sender.error("Flow names may only contain letters, digits, - and _.")
        if (args.size == 1) {
            val furniture = NexoFurniture.findTargetFurniture(player) ?: return sender.error("Look at a Nexo furniture or pass its coordinates.")
            return setDoor(sender, name, furniture)
        }
        val current = player.location
        val x = args[1].toCoordinate(current.blockX)
        val y = args[2].toCoordinate(current.blockY)
        val z = args[3].toCoordinate(current.blockZ)
        if (x == null || y == null || z == null) return sender.error("Coordinates must be whole numbers or ~.")
        val location = Location(player.world, x.toDouble(), y.toDouble(), z.toDouble())
        plugin.server.regionScheduler.execute(plugin, location) {
            val furniture = NexoFurniture.baseEntity(location) ?: return@execute sender.error("No Nexo furniture at $x, $y, $z.")
            setDoor(sender, name, furniture)
        }
    }

    private fun setDoor(sender: CommandSender, name: String, furniture: Entity) {
        val door = Door.at(furniture.location)
        val owner = store.flowWithDoor(door)
        if (owner != null && owner.name != name) return sender.error("That door already belongs to ${owner.name}.")
        store.put((store.flows[name] ?: Flow(name)).copy(door = door))
        val id = NexoFurniture.furnitureMechanic(furniture)?.itemID ?: "furniture"
        sender.info("Door for $name set to $id at $door")
        warnIncomplete(sender, name)
    }

    private fun conversation(sender: CommandSender, command: String, args: List<String>) {
        if (args.size != 3) return sender.error("Usage: /$command conversation <flow> <${KnockLevel.entries.joinToString("|") { it.key }}> <conversation|reset>")
        val (name, levelKey, conversation) = args
        if (!NAME.matches(name)) return sender.error("Flow names may only contain letters, digits, - and _.")
        val level = KnockLevel.of(levelKey) ?: return sender.error("Unknown knock level $levelKey.")
        val flow = store.flows[name] ?: Flow(name)
        if (conversation.equals("reset", ignoreCase = true)) {
            store.put(flow.copy(conversations = flow.conversations - level))
            sender.info("Removed the ${level.key} conversation from $name.")
        } else {
            if (conversation !in InteractionsHook.conversations()) return sender.error("Interactions has no conversation named $conversation.")
            store.put(flow.copy(conversations = flow.conversations + (level to conversation)))
            sender.info("${level.key.replaceFirstChar(Char::uppercase)} conversation for $name set to $conversation.")
        }
        warnIncomplete(sender, name)
    }

    private fun npc(sender: CommandSender, command: String, args: List<String>) {
        val usage = "Usage: /$command npc <flow> <${NPC_OPTIONS.joinToString("|")}> [id]"
        if (args.size !in 2..3) return sender.error(usage)
        val name = args[0]
        if (!NAME.matches(name)) return sender.error("Flow names may only contain letters, digits, - and _.")
        val flow = store.flows[name] ?: Flow(name)
        val keeper = flow.doorkeeper
        val option = args[1].lowercase()
        val updated = when (option) {
            "id" -> {
                if (!TrickOrTreat.citizensEnabled(plugin)) return sender.error("Citizens is not enabled.")
                val npc = args.getOrNull(2)?.let { id ->
                    val number = id.toIntOrNull() ?: return sender.error("NPC ids must be whole numbers.")
                    CitizensAPI.getNPCRegistry().getById(number) ?: return sender.error("No NPC with id $number.")
                } ?: CitizensAPI.getDefaultNPCSelector().getSelected(sender) ?: return sender.error("Select an NPC or pass its id.")
                sender.info("NPC for $name set to ${npc.name} (${npc.id}).")
                keeper.copy(npc = npc.id)
            }
            "standby", "inside", "outside" -> {
                val player = sender as? Player ?: return sender.error("Only players can set NPC locations.")
                val location = player.location.toBlockCenter()
                sender.info("${option.replaceFirstChar(Char::uppercase)} location for $name set to ${location.describe()}")
                when (option) {
                    "standby" -> keeper.copy(standby = location)
                    "inside" -> keeper.copy(inside = location)
                    else -> keeper.copy(outside = location)
                }
            }
            "reset" -> {
                sender.info("Removed the NPC from $name.")
                Doorkeeper()
            }
            else -> return sender.error(usage)
        }
        store.put(flow.copy(doorkeeper = updated))
        knocker.rest(store.flows.getValue(name))
        warnIncomplete(sender, name)
    }

    private fun openAt(sender: CommandSender, command: String, args: List<String>) {
        val usage = "Usage: /$command openat <flow> <add|remove> <file:conversation:dialogue>"
        if (args.size != 3) return sender.error(usage)
        val (name, action, value) = args
        if (!NAME.matches(name)) return sender.error("Flow names may only contain letters, digits, - and _.")
        val point = DialoguePoint.parse(value) ?: return sender.error("Dialogue points look like file:conversation:dialogue.")
        val flow = store.flows[name] ?: Flow(name)
        when (action.lowercase()) {
            "add" -> {
                if (point !in InteractionsHook.points(point.file)) return sender.error("Interactions has no dialogue $point.")
                store.put(flow.copy(openAt = flow.openAt + point))
                sender.info("The door of $name now opens at $point.")
            }
            "remove" -> {
                if (point !in flow.openAt) return sender.error("$name doesn't open at $point.")
                store.put(flow.copy(openAt = flow.openAt - point))
                sender.info("The door of $name no longer opens at $point.")
            }
            else -> return sender.error(usage)
        }
        warnIncomplete(sender, name)
    }

    private fun openAtSuggestions(name: String, action: String): List<String> {
        val flow = store.flows[name] ?: return emptyList()
        return when (action.lowercase()) {
            "add" -> flow.conversations.values.distinct().ifEmpty { InteractionsHook.conversations() }
                .flatMap(InteractionsHook::points).filter { it !in flow.openAt }.map(DialoguePoint::toString)
            "remove" -> flow.openAt.map(DialoguePoint::toString)
            else -> emptyList()
        }
    }

    private fun reload(sender: CommandSender) {
        try {
            store.load()
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to reload trickortreat.yml", e)
            return sender.error("Failed to reload trickortreat.yml: ${e.message}")
        }
        store.flows.values.forEach(knocker::rest)
        sender.info("Reloaded ${store.flows.size} trick or treat flows.")
    }

    private fun delete(sender: CommandSender, command: String, name: String?) {
        if (name == null) return sender.error("Usage: /$command delete <flow>")
        if (store.remove(name)) sender.info("Flow $name deleted.") else sender.error("No flow named $name.")
    }

    private fun list(sender: CommandSender) {
        if (store.flows.isEmpty()) return sender.info("No trick or treat flows defined.")
        store.flows.values.forEach {
            val conversations = KnockLevel.entries.joinToString { level -> "${level.key}: ${it.conversations[level] ?: "-"}" }
            sender.info("- ${it.name}: door ${it.door ?: "not set"}, $conversations")
            val keeper = it.doorkeeper
            if (keeper != Doorkeeper()) {
                val locations = listOf("standby" to keeper.standby, "inside" to keeper.inside, "outside" to keeper.outside)
                    .joinToString { (key, location) -> "$key: ${location?.describe() ?: "-"}" }
                sender.info("  npc: ${keeper.npc ?: "-"}, $locations")
            }
            if (it.openAt.isNotEmpty()) sender.info("  opens at: ${it.openAt.joinToString()}")
        }
    }

    private fun warnIncomplete(sender: CommandSender, name: String) {
        val flow = store.flows[name] ?: return
        if (flow.door == null) sender.error("$name has no door yet and is inactive.")
        if (flow.conversations.isEmpty()) sender.error("$name has no conversations yet and is inactive.")
        val keeper = flow.doorkeeper
        if (keeper == Doorkeeper() && flow.openAt.isEmpty()) return
        val missing = listOfNotNull(
            "npc id".takeIf { keeper.npc == null },
            "standby location".takeIf { keeper.standby == null },
            "inside location".takeIf { keeper.inside == null },
            "outside location".takeIf { keeper.outside == null },
            "dialogue to open at".takeIf { flow.openAt.isEmpty() },
        )
        if (missing.isNotEmpty()) sender.error("The NPC of $name won't step out until it has: ${missing.joinToString()}.")
    }

    private fun CommandSender.info(message: String) = Util.sendMsg(this, MiniMessage.miniMessage().escapeTags(message))

    private fun CommandSender.error(message: String) = Util.sendMsg(this, "<red>" + MiniMessage.miniMessage().escapeTags(message))

    private fun Location.describe() = "${world.name} (%.1f, %.1f, %.1f)".format(x, y, z)

    private fun String.toCoordinate(current: Int) = if (this == "~") current else toIntOrNull()

    companion object {
        private val SUBCOMMANDS = listOf("door", "conversation", "npc", "openat", "delete", "list", "reload")
        private val NPC_OPTIONS = listOf("id", "standby", "inside", "outside", "reset")
        private val NAME = Regex("[A-Za-z0-9_-]+")
    }
}
