package dev.jsinco.luma.lumaevents;

import dev.jsinco.luma.lumacore.manager.modules.ModuleManager;
import dev.jsinco.luma.lumaevents.configurable.Config;
import dev.jsinco.luma.lumaevents.configurable.ConfigManager;
import dev.jsinco.luma.lumaevents.configurable.MinigameState;
import dev.jsinco.luma.lumaevents.games.obj.CountdownBossBar;
import dev.jsinco.luma.lumaevents.games.MinigameManager;
import dev.jsinco.luma.lumaevents.games.interfaces.Minigame;
import dev.jsinco.luma.lumaevents.items.RefinedSummerOpal;
import dev.jsinco.luma.lumaevents.items.SummerOpal;
import dev.jsinco.luma.lumaevents.items.LocalCustomItemManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class EventMain extends JavaPlugin {

    private static EventMain instance;
    private static ConfigManager okaeriConfigManager;
    private static ModuleManager moduleManager;

    @Override
    public void onEnable() {
        instance = this;
        okaeriConfigManager = new ConfigManager();
        moduleManager = new ModuleManager(this);
        moduleManager.reflectivelyRegisterModules();


        EventPlayerManager.loadAll();
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, EventPlayerManager::saveAll, 0, 12000);

        MinigameManager.getInstance().runTaskTimerAsynchronously(this, 0, 600); // 30 seconds

        LocalCustomItemManager.addCustomItem(new SummerOpal());
        LocalCustomItemManager.addCustomItem(new RefinedSummerOpal());
        LocalCustomItemManager.registerCustomItems();
    }

    @Override
    public void onDisable() {
        moduleManager.unregisterModules();

        EventPlayerManager.saveAll();
        Minigame current = MinigameManager.getInstance().getCurrent();
        if (current.isActive()) {
            current.stop();
        }
        CountdownBossBar.stopAll(false);
    }

    @SuppressWarnings("")
    public static EventMain getInstance() {
        return instance;
    }

    public static Config getOkaeriConfig() {
        return okaeriConfigManager.getConfig();
    }

    public static MinigameState getMinigameState() {
        return okaeriConfigManager.getMinigameState();
    }
}
