package dev.lumas.events.trickortreat

import dev.lumas.events.EventMain
import org.bukkit.plugin.Plugin
import java.util.logging.Level

class TrickOrTreat private constructor(val plugin: Plugin) {

    val store = TrickOrTreatStore(plugin.dataPath.resolve(FILE), EventMain.getConfigManager().serializers)
    val knocker = DoorKnocker(plugin, store)

    init {
        store.load()
        plugin.server.pluginManager.registerEvents(TrickOrTreatListener(knocker), plugin)
    }

    companion object {
        const val ADMIN = "lumaevents.admin"
        private val DEPENDENCIES = listOf("Nexo", "Interactions")
        private const val FILE = "trickortreat.yml"

        @JvmStatic
        var instance: TrickOrTreat? = null
            private set

        @JvmStatic
        fun enable(plugin: Plugin) {
            if (!DEPENDENCIES.all(plugin.server.pluginManager::isPluginEnabled)) {
                return plugin.logger.info("Trick or treat is disabled, it requires ${DEPENDENCIES.joinToString()}.")
            }
            instance = runCatching { TrickOrTreat(plugin) }
                .onFailure { plugin.logger.log(Level.SEVERE, "Failed to enable trick or treat", it) }
                .getOrNull()
        }

        @JvmStatic
        fun disable() {
            instance?.knocker?.abortAll()
            instance = null
        }

        // Not a softdepend: Citizens force-saves every NPC off-thread whenever a plugin depending on it disables
        fun citizensEnabled(plugin: Plugin) = plugin.server.pluginManager.isPluginEnabled("Citizens")
    }
}
