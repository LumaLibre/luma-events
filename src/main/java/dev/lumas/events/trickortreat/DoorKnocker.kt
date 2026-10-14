package dev.lumas.events.trickortreat

import com.nexomc.nexo.api.NexoFurniture
import org.bukkit.GameMode
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class DoorKnocker(private val plugin: Plugin, private val store: TrickOrTreatStore) {

    private class Session {
        val knocks = ArrayDeque<Long>()
        var answering = false
    }

    private val sessions = ConcurrentHashMap<Door, Session>()
    private val visits = ConcurrentHashMap<Door, DoorVisit>()

    fun knock(player: Player, entity: ItemDisplay): Boolean {
        val flow = store.flowAt(entity.location) ?: return false
        val door = flow.door!!
        if (visits.containsKey(door) || InteractionsHook.isTalking(player)) return true
        val knock = store.knock
        knock.sound.playAt(entity.location)

        val session = sessions.computeIfAbsent(door) { Session() }
        session.knocks.addLast(System.currentTimeMillis())
        val count = session.recentKnocks()
        if (session.answering || Random.nextDouble() >= store.levels[store.levelFor(count)].chance) return true

        session.answering = true
        plugin.server.regionScheduler.runDelayed(plugin, entity.location, {
            if (sessions.remove(door, session)) answer(flow, entity, store.levelFor(session.recentKnocks()))
        }, knock.answerDelayTicks.coerceAtLeast(1))
        return true
    }

    fun rest(flow: Flow) {
        if (!TrickOrTreat.citizensEnabled(plugin) || visits.containsKey(flow.door ?: return)) return
        flow.doorkeepers.values.filter { it.isComplete }.forEach { NpcHook.rest(plugin, it) }
    }

    fun watch(player: Player, file: String) {
        if (store.flows.values.none { it.watches(file) }) return
        // The conversation becomes active a few ticks after the start event
        var waited = 0
        player.scheduler.runAtFixedRate(plugin, { task ->
            val point = InteractionsHook.pointOf(player)
            if (point == null && waited++ < START_TIMEOUT_TICKS) return@runAtFixedRate
            if (point == null || point.file != file) {
                task.cancel()
                return@runAtFixedRate
            }
            waited = START_TIMEOUT_TICKS
            val flow = store.flows.values
                .filter { it.opensAt(point) && it.door?.world == player.world.name && !visits.containsKey(it.door) }
                .minByOrNull { it.door!!.location(player.world).distanceSquared(player.location) } ?: return@runAtFixedRate
            task.cancel()
            open(flow, file, player)
        }, null, 1, 1)
    }

    fun abortAll() {
        visits.values.forEach(DoorVisit::abort)
        visits.clear()
    }

    private fun Session.recentKnocks(): Int {
        val cutoff = System.currentTimeMillis() - store.knock.windowTicks * 50
        while (knocks.firstOrNull()?.let { it < cutoff } == true) knocks.removeFirst()
        return knocks.size
    }

    private fun answer(flow: Flow, entity: ItemDisplay, level: KnockLevel) {
        val conversation = flow.conversationFor(level) ?: return
        if (conversation !in InteractionsHook.conversations()) return plugin.logger.warning("Trick or treat flow ${flow.name} uses unknown conversation $conversation")
        val location = entity.location
        val participants = location.getNearbyPlayers(store.knock.groupRadius)
            .filter { !it.isDead && it.gameMode != GameMode.SPECTATOR && !InteractionsHook.isTalking(it) }
            .filter { InteractionsHook.start(it, conversation, location) }
        visit(flow, entity, conversation, participants)
    }

    private fun open(flow: Flow, file: String, player: Player) {
        val door = flow.door!!
        val location = door.location(player.world)
        plugin.server.regionScheduler.execute(plugin, location) {
            if (visits.containsKey(door)) return@execute
            val entity = location.getNearbyEntitiesByType(ItemDisplay::class.java, 1.5)
                .firstOrNull { door.matches(it.location) && NexoFurniture.isFurniture(it) }
                ?: return@execute plugin.logger.warning("Trick or treat flow ${flow.name} has no door furniture at $door")
            val participants = location.getNearbyPlayers(store.knock.groupRadius).filter { InteractionsHook.conversationOf(it) == file }
            visit(flow, entity, file, (participants + player).distinct())
        }
    }

    private fun visit(flow: Flow, entity: ItemDisplay, conversation: String, participants: List<Player>) {
        if (participants.isEmpty()) return
        val door = flow.door!!
        val visit = DoorVisit(plugin, flow, entity, conversation, participants) { visits.remove(door) }
        visits[door] = visit
        visit.start()
    }

    companion object {
        private const val START_TIMEOUT_TICKS = 100
    }
}
