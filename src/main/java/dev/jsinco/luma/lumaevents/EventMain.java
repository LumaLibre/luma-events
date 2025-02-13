package dev.jsinco.luma.lumaevents;

import dev.jsinco.luma.lumacore.manager.modules.ModuleManager;
import dev.jsinco.luma.lumaevents.configurable.Config;
import dev.jsinco.luma.lumaevents.configurable.ConfigManager;
import dev.jsinco.luma.lumaevents.customitems.StartMinigameItem;
import dev.jsinco.luma.lumaevents.customitems.SwapTeamsItem;
import dev.jsinco.luma.lumaevents.games.CountdownBossBar;
import dev.jsinco.luma.lumaevents.games.MinigameManager;
import dev.jsinco.luma.lumaevents.games.logic.Minigame;
import dev.jsinco.luma.lumaitems.api.LumaItemsAPI;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class EventMain extends JavaPlugin {

    @Getter
    private static EventMain instance;
    @Getter
    private static Config okaeriConfig;
    private static ModuleManager moduleManager;

    @Override
    public void onEnable() {
        instance = this;
        okaeriConfig = new ConfigManager().getConfig();
        moduleManager = new ModuleManager(this);
        moduleManager.reflectivelyRegisterModules();
        EventPlayerManager.loadAll();
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, EventPlayerManager::saveAll, 0, 12000);

        MinigameManager.getInstance().runTaskTimerAsynchronously(this, 0, 600); // 30 seconds

        LumaItemsAPI lumaItemsAPI = LumaItemsAPI.getInstance();
        lumaItemsAPI.registerCustomItem(new SwapTeamsItem());
        lumaItemsAPI.registerCustomItem(new StartMinigameItem());
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
}
