package dev.jsinco.lumacarnival

import dev.jsinco.abstractjavafilelib.FileLibSettings
import dev.jsinco.abstractjavafilelib.schemas.SnakeYamlConfig
import dev.jsinco.lumacarnival.games.AppleBobbingGame
import dev.jsinco.lumacarnival.games.GameManager
import dev.jsinco.lumacarnival.games.TargetPracticeGame
import org.bukkit.plugin.java.JavaPlugin


class CarnivalMain : JavaPlugin() {

    companion object {
        @JvmStatic
        lateinit var instance: CarnivalMain private set
        lateinit var config: SnakeYamlConfig private set
        lateinit var gameManager: GameManager private set
    }

    override fun onEnable() {
        instance = this
        FileLibSettings.set(dataFolder)
        CarnivalMain.config = SnakeYamlConfig("config.yml")
        gameManager = GameManager()
            .registerGame(TargetPracticeGame())
            .registerGame(AppleBobbingGame())
            .startGameTicker()
    }
}