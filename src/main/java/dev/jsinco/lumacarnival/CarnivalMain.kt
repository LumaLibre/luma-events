package dev.jsinco.lumacarnival

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin


class CarnivalMain : JavaPlugin() {

    companion object {
        @JvmStatic
        lateinit var instance: CarnivalMain private set
        lateinit var config: YamlConfiguration private set
    }

    override fun onEnable() {
        super.onEnable()
    }
}