package dev.jsinco.lumacarnival

import dev.jsinco.abstractjavafilelib.FileLibSettings
import dev.jsinco.abstractjavafilelib.schemas.JsonSavingSchema
import dev.jsinco.abstractjavafilelib.schemas.SnakeYamlConfig
import dev.jsinco.lumacarnival.games.impls.AppleBobbingGame
import dev.jsinco.lumacarnival.games.GameManager
import dev.jsinco.lumacarnival.games.impls.PrisonMineGame
import dev.jsinco.lumacarnival.games.impls.TargetPracticeGame
import dev.jsinco.lumacarnival.games.impls.UnderwaterAnimalCatchingGame
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin


class CarnivalMain : JavaPlugin() {

    companion object {
        @JvmStatic
        lateinit var instance: CarnivalMain private set
        lateinit var config: SnakeYamlConfig private set
        lateinit var saves: JsonSavingSchema private set
        lateinit var gameManager: GameManager private set


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

        getCommand("lumacarnival")!!.setExecutor(gameManager)


        Bukkit.getScheduler().runTaskTimerAsynchronously(this, Runnable {
            gameManager.saveAll()
            logger.info("Autosaved!")
        }, 0, 12000L)
    }

    override fun onDisable() {
        gameManager.saveAll()
        gameManager.stop()
    }
}