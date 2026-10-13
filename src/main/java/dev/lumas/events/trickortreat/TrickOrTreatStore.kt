package dev.lumas.events.trickortreat

import io.leangen.geantyref.TypeToken
import org.bukkit.Location
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializerCollection
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.nio.file.Path

class TrickOrTreatStore(path: Path, serializers: TypeSerializerCollection) {

    private val loader = YamlConfigurationLoader.builder()
        .path(path)
        .nodeStyle(NodeStyle.BLOCK)
        .indent(2)
        .defaultOptions { it.serializers(serializers.childBuilder().register(DoorSerializer).build()) }
        .build()

    private var root: CommentedConfigurationNode = loader.createNode()

    @Volatile var flows: Map<String, Flow> = emptyMap()
        private set

    @Volatile var knock = KnockSettings()
        private set

    @Volatile var levels = KnockLevels()
        private set

    @Synchronized
    fun load() {
        val root = loader.load()
        val knock = root.node(KNOCK).get(KnockSettings::class.java) ?: KnockSettings()
        val levels = root.node(LEVELS).get(KnockLevels::class.java) ?: KnockLevels()
        val flows = root.node(FLOWS).get(FLOWS_TYPE).orEmpty()

        root.node(KNOCK).set(KnockSettings::class.java, knock)
        root.node(LEVELS).set(KnockLevels::class.java, levels)
        root.node(FLOWS).set(FLOWS_TYPE, flows)
        loader.save(root)

        this.root = root
        this.knock = knock
        this.levels = levels
        this.flows = flows.mapValues { (name, flow) -> flow.toFlow(name) }
    }

    fun flowAt(location: Location) = flows.values.firstOrNull { it.isComplete && it.door!!.matches(location) }

    fun flowWithDoor(door: Door) = flows.values.firstOrNull { it.door == door }

    fun levelFor(knocks: Int) = KnockLevel.entries.lastOrNull { knocks >= levels[it].minKnocks } ?: KnockLevel.MILD

    @Synchronized
    fun put(flow: Flow) {
        flows = flows + (flow.name to flow)
        root.node(FLOWS, flow.name).set(FlowSettings::class.java, FlowSettings.of(flow))
        loader.save(root)
    }

    @Synchronized
    fun remove(name: String): Boolean {
        if (name !in flows) return false
        flows = flows - name
        root.node(FLOWS).removeChild(name)
        loader.save(root)
        return true
    }

    companion object {
        private const val KNOCK = "knock"
        private const val LEVELS = "levels"
        private const val FLOWS = "flows"
        private val FLOWS_TYPE = object : TypeToken<Map<String, @JvmSuppressWildcards FlowSettings>>() {}
    }
}
