package dev.lumas.events;

import dev.lumas.events.games.events.LateJoinListener;
import dev.lumas.events.items.CaramelAppleItem;
import dev.lumas.events.tasks.PlaytimeCounterTask;
import dev.lumas.lumacore.manager.modules.ModuleManager;
import dev.lumas.events.configurable.Config;
import dev.lumas.events.configurable.ConfigManager;
import dev.lumas.events.configurable.MinigameState;
import dev.lumas.events.games.models.CountdownBossBar;
import dev.lumas.events.games.MinigameManager;
import dev.lumas.events.games.interfaces.Minigame;
import dev.lumas.events.items.StartMinigameItem;
import dev.lumas.events.items.CandiedAppleItem;
import dev.lumas.events.items.LocalCustomItemManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;

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
        Bukkit.getPluginManager().registerEvents(new LateJoinListener(), this);

        LocalCustomItemManager.addCustomItem(new CandiedAppleItem());
        LocalCustomItemManager.addCustomItem(new StartMinigameItem());
        LocalCustomItemManager.addCustomItem(new CaramelAppleItem());
        LocalCustomItemManager.registerCustomItems();

        // playtime counter
        PlaytimeCounterTask task = new PlaytimeCounterTask(60, TimeUnit.SECONDS);
        Bukkit.getAsyncScheduler().runAtFixedRate(this, task, 0, 60, TimeUnit.SECONDS);
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
