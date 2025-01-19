package dev.jsinco.luma.lumaevents;

import dev.jsinco.luma.lumacore.manager.modules.ModuleManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class EventMain extends JavaPlugin {

    private static EventMain instance;
    private static ModuleManager moduleManager;

    @Override
    public void onEnable() {
        instance = this;
        moduleManager = new ModuleManager(this);
        moduleManager.reflectivelyRegisterModules();
        EventPlayerManager.loadAll();
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, EventPlayerManager::saveAll, 0, 12000);
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
