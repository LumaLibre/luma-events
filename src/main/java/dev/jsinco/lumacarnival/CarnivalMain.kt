package dev.jsinco.lumacarnival

import dev.jsinco.abstractjavafilelib.FileLibSettings
import dev.jsinco.abstractjavafilelib.schemas.JsonSavingSchema
import dev.jsinco.abstractjavafilelib.schemas.SnakeYamlConfig
import dev.jsinco.lumacarnival.commands.CommandManager
import dev.jsinco.lumacarnival.games.GameManager
import dev.jsinco.lumacarnival.games.impls.AppleBobbingGame
import dev.jsinco.lumacarnival.games.impls.PrisonMineGame
import dev.jsinco.lumacarnival.games.impls.TargetPracticeGame
import dev.jsinco.lumacarnival.games.impls.UnderwaterAnimalCatchingGame
import dev.jsinco.lumacarnival.shop.ShopListener
import dev.jsinco.lumacarnival.shop.ShopManager
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class CarnivalMain : JavaPlugin() {

    companion object {
        @JvmStatic
        lateinit var instance: CarnivalMain private set
        lateinit var config: SnakeYamlConfig private set
        lateinit var saves: JsonSavingSchema private set
        lateinit var gameManager: GameManager private set
        lateinit var shopManager: ShopManager private set


        fun reload() {
            gameManager.saveAll()
            gameManager.stop()
            config = SnakeYamlConfig("config.yml")
            saves = JsonSavingSchema("saves.json")
            gameManager = GameManager()
                .registerGame(TargetPracticeGame())
                .registerGame(AppleBobbingGame())
                .registerGame(UnderwaterAnimalCatchingGame())
                .registerGame(PrisonMineGame())
                .startGameTicker()
        }
    }

    override fun onEnable() {
        instance = this
        FileLibSettings.set(dataFolder)
        CarnivalMain.config = SnakeYamlConfig("config.yml")
        saves = JsonSavingSchema("saves.json")
        gameManager = GameManager()
            .registerGame(TargetPracticeGame())
            .registerGame(AppleBobbingGame())
            .registerGame(UnderwaterAnimalCatchingGame())
            .registerGame(PrisonMineGame())
            .startGameTicker()

        server.pluginManager.registerEvents(ShopListener(), this)
        shopManager = ShopManager()

        server.pluginManager.registerEvents(CarnivalToken, this)

        getCommand("lumacarnival-gamemanager")!!.setExecutor(gameManager)
        getCommand("lumacarnival")!!.setExecutor(CommandManager(this))


        Bukkit.getScheduler().runTaskTimerAsynchronously(this, Runnable {
            gameManager.saveAll()
            logger.info("Autosaved!")
        }, 0, 12000L)
    }

    override fun onDisable() {
        for (player in Bukkit.getOnlinePlayers()) {
            val holder = player.openInventory.topInventory.getHolder(false)

            if (holder != null && holder.javaClass.simpleName == "ShopManager") {
                player.closeInventory()
            }
        }

        gameManager.saveAll()
        gameManager.stop()
    }
}