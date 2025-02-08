package dev.jsinco.luma.lumaevents;

import dev.jsinco.luma.lumacore.manager.modules.ModuleManager;
import dev.jsinco.luma.lumaevents.configurable.Config;
import dev.jsinco.luma.lumaevents.configurable.ConfigManager;
import dev.jsinco.luma.lumaevents.placeholders.PlaceholderManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class EventMain extends JavaPlugin {

    private static EventMain instance;
    private static ModuleManager moduleManager;
    @Getter
    private static Config okaeriConfig;
    private static PlaceholderManager papiManager;

    @Override
    public void onEnable() {
        instance = this;
        moduleManager = new ModuleManager(this);
        moduleManager.reflectivelyRegisterModules();
        EventPlayerManager.loadAll();
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, EventPlayerManager::saveAll, 0, 12000);

        okaeriConfig = new ConfigManager().getConfig();

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            papiManager = new PlaceholderManager();
            papiManager.register();
        }
    }

    @Override
    public void onDisable() {
        moduleManager.unregisterModules();

        EventPlayerManager.saveAll();

        if (papiManager != null) {
            papiManager.unregister();
        }
    }

    public static EventMain getInstance() {
        return instance;
    }
}
