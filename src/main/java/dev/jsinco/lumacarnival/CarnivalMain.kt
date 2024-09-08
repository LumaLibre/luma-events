package dev.jsinco.lumacarnival

import dev.jsinco.abstractjavafilelib.FileLibSettings
import dev.jsinco.abstractjavafilelib.schemas.JsonSavingSchema
import dev.jsinco.abstractjavafilelib.schemas.SnakeYamlConfig
import dev.jsinco.lumacarnival.games.impls.AppleBobbingGame
import dev.jsinco.lumacarnival.games.GameManager
import dev.jsinco.lumacarnival.games.impls.PrisonMineGame
import dev.jsinco.lumacarnival.games.impls.TargetPracticeGame
import dev.jsinco.lumacarnival.games.impls.UnderwaterAnimalCatchingGame
import org.bukkit.plugin.java.JavaPlugin


class CarnivalMain : JavaPlugin() {

    companion object {
        @JvmStatic
        lateinit var instance: CarnivalMain private set
        lateinit var config: SnakeYamlConfig private set
        lateinit var saves: JsonSavingSchema private set
        lateinit var gameManager: GameManager private set
    }

    override fun onEnable() {
        instance = this
        FileLibSettings.set(dataFolder)
        CarnivalMain.config = SnakeYamlConfig("config.yml")
        CarnivalMain.saves = JsonSavingSchema("saves.json")
        gameManager = GameManager()
            .registerGame(TargetPracticeGame())
            .registerGame(AppleBobbingGame())
            .registerGame(UnderwaterAnimalCatchingGame())
            .registerGame(PrisonMineGame())
            .startGameTicker()
    }

    override fun onDisable() {
        gameManager.stop()
    }
}