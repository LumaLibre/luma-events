package dev.lumas.events.trickortreat

import dev.lumas.core.annotation.Autowire
import dev.lumas.core.annotation.CommandMeta
import dev.lumas.core.annotation.Register
import dev.lumas.core.model.command.AbstractCommand
import dev.lumas.events.EventMain
import dev.lumas.events.utility.Util
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.command.CommandSender

@Register(Autowire.COMMAND)
@CommandMeta(
    name = "pumpkinparticles",
    permission = TrickOrTreat.ADMIN,
    description = "Spawn the pumpkin particles, only for one player if given",
    usage = "/<command> [player]",
)
class PumpkinParticlesCommand : AbstractCommand() {

    override fun handle(sender: CommandSender, label: String, args: Array<String>): Boolean {
        if (args.size > 1) return sender.error("Usage: /$label [player]")
        val player = args.firstOrNull()?.let { name ->
            sender.server.getPlayerExact(name) ?: return sender.error("No online player named $name.")
        }
        val world = sender.server.getWorld(WORLD) ?: return sender.error("World $WORLD is not loaded.")
        val locations = POSITIONS.map { (x, y, z) -> Location(world, x, y, z) }
        val plugin = EventMain.getInstance()
        plugin.server.regionScheduler.execute(plugin, locations.first()) {
            locations.forEach { location ->
                if (player != null) {
                    player.spawnParticle(Particle.END_ROD, location, COUNT, SPREAD, SPREAD, SPREAD, SPEED, null, true)
                } else {
                    world.spawnParticle(Particle.END_ROD, location, COUNT, SPREAD, SPREAD, SPREAD, SPEED, null, true)
                }
            }
        }
        return true
    }

    override fun handleTabComplete(sender: CommandSender, label: String, args: Array<String>): List<String> {
        if (args.size != 1) return emptyList()
        return sender.server.onlinePlayers.map { it.name }.filter { it.startsWith(args[0], ignoreCase = true) }
    }

    private fun CommandSender.error(message: String): Boolean {
        Util.sendMsg(this, "<red>$message")
        return true
    }

    companion object {
        private val WORLD = NamespacedKey("worlds", "special")
        private const val COUNT = 15
        private const val SPREAD = 0.2
        private const val SPEED = 0.05
        private val POSITIONS = listOf(
            Triple(4186.0, -48.0, 315.0),
            Triple(4189.0, -48.0, 315.0),
            Triple(4191.0, -45.5, 314.0),
            Triple(4193.0, -45.5, 314.0),
            Triple(4195.0, -45.5, 314.0),
            Triple(4200.0, -45.5, 314.0),
            Triple(4202.0, -45.5, 314.0),
            Triple(4204.0, -45.5, 314.0),
            Triple(4206.0, -45.5, 314.0),
            Triple(4208.0, -45.5, 314.0),
        )
    }
}
