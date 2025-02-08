package dev.jsinco.luma.lumaevents;

import dev.jsinco.luma.lumacore.manager.modules.ModuleManager;
import dev.jsinco.luma.lumaevents.configurable.Config;
import dev.jsinco.luma.lumaevents.configurable.ConfigManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class EventMain extends JavaPlugin {

    private static EventMain instance;
    private static ModuleManager moduleManager;
    @Getter
    private static Config okaeriConfig;

    @Override
    public void onEnable() {
        instance = this;
        moduleManager = new ModuleManager(this);
        moduleManager.reflectivelyRegisterModules();
        EventPlayerManager.loadAll();
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, EventPlayerManager::saveAll, 0, 12000);

        okaeriConfig = new ConfigManager().getConfig();
    }

    @Override
    public void onDisable() {
        moduleManager.unregisterModules();

        EventPlayerManager.saveAll();

        for (Player player : Bukkit.getOnlinePlayers()) {
            var holder = player.getOpenInventory().getTopInventory().getHolder(false);

        }
    }

    public static EventMain getInstance() {
        return instance;
    }
}
