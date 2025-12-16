package dev.jsinco.luma.lumaevents;

import dev.jsinco.luma.lumacore.manager.modules.ModuleManager;
import dev.jsinco.luma.lumaevents.configurable.Config;
import dev.jsinco.luma.lumaevents.configurable.ConfigManager;
import dev.jsinco.luma.lumaevents.configurable.MinigameState;
import dev.jsinco.luma.lumaevents.games.obj.CountdownBossBar;
import dev.jsinco.luma.lumaevents.games.MinigameManager;
import dev.jsinco.luma.lumaevents.games.interfaces.Minigame;
import dev.jsinco.luma.lumaevents.items.PresentItem;
import dev.jsinco.luma.lumaevents.items.StartMinigameItem;
import dev.jsinco.luma.lumaevents.items.WinterStampItem;
import dev.jsinco.luma.lumaevents.items.LocalCustomItemManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class EventMain extends JavaPlugin {

    private static EventMain instance;
    private static ConfigManager okaeriConfigManager;
    private static ModuleManager moduleManager;

    public static boolean STOPPING = false;

    @Override
    public void onEnable() {
        instance = this;
        okaeriConfigManager = new ConfigManager();
        moduleManager = new ModuleManager(this);
        moduleManager.reflectivelyRegisterModules();


        EventPlayerManager.loadAll();
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, EventPlayerManager::saveAll, 0, 12000);

        MinigameManager.getInstance().runTaskTimerAsynchronously(this, 0, 600); // 30 seconds

        LocalCustomItemManager.addCustomItem(new WinterStampItem());
        LocalCustomItemManager.addCustomItem(new StartMinigameItem());
        LocalCustomItemManager.addCustomItem(new PresentItem());
        LocalCustomItemManager.registerCustomItems();
    }

    @Override
    public void onDisable() {
        STOPPING = true;
        moduleManager.unregisterModules();

        EventPlayerManager.saveAll();
        Minigame current = MinigameManager.getInstance().getCurrent();
        if (current.isActive()) {
            current.stop();
        }
        CountdownBossBar.stopAll(false);

        for (BossBar bossBar : CountdownBossBar.activeCountdowns.stream().map(CountdownBossBar::getBossBar).toList()) {
            bossBar.removeViewer(Audience.audience(Bukkit.getOnlinePlayers()));
        }
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
