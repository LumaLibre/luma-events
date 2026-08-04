package dev.lumas.events;

import dev.lumas.core.manager.Modules;
import dev.lumas.events.configurable.Config;
import dev.lumas.events.configurable.ConfigManager;
import dev.lumas.events.configurable.PersistentStates;
import dev.lumas.events.games.MinigameManager;
import dev.lumas.events.games.interfaces.Minigame;
import dev.lumas.events.games.models.CountdownBossBar;
import dev.lumas.events.items.LocalCustomItemManager;
import dev.lumas.events.items.StartMinigameItem;
import dev.lumas.events.items.SummerDollopItem;
import dev.lumas.events.manager.EventPlayerManager;
import dev.lumas.events.manager.LeaderboardCacheManager;
import dev.lumas.events.tasks.PlaytimeCounterTask;
import dev.lumas.events.utility.Externals;
import dev.lumas.lumaitems.util.extensions.Executors;
import lombok.Getter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;

public final class EventMain extends JavaPlugin {

    private static EventMain instance;
    private static ConfigManager okaeriConfigManager;
    private static Modules moduleManager;
    @Getter
    private static boolean withMcMMO = false;

    public static boolean STOPPING = false;

    @Override
    public void onEnable() {
        instance = this;

        okaeriConfigManager = new ConfigManager();
        moduleManager = new Modules(this);

        moduleManager.register();
        Executors.globalDelayed(1L, t -> {
            EventPlayerManager.loadOnlinePlayers();
        });
        LeaderboardCacheManager.start();

        Executors.asyncTimer(20 * 60L, 20 * 60L, _ -> EventPlayerManager.evictStale());
        MinigameManager.getInstance().repeatingAsync(0, 600);

        LocalCustomItemManager.addCustomItem(new SummerDollopItem());
        LocalCustomItemManager.addCustomItem(new StartMinigameItem());
        LocalCustomItemManager.registerCustomItems();

        // playtime counter
        PlaytimeCounterTask task = new PlaytimeCounterTask(60, TimeUnit.SECONDS);
        Bukkit.getAsyncScheduler().runAtFixedRate(this, task, 0, 60, TimeUnit.SECONDS);

        if (Externals.pluginExists("mcMMO")) {
            withMcMMO = true;
        }
    }

    @Override
    public void onDisable() {
        STOPPING = true;

        EventPlayerManager.saveAllAndClear();
        CountdownBossBar.stopAll(false);
        HandlerList.unregisterAll(this);
        for (BossBar bossBar : CountdownBossBar.activeCountdowns.stream().map(CountdownBossBar::getBossBar).toList()) {
            bossBar.removeViewer(Audience.audience(Bukkit.getOnlinePlayers()));
        }
        Minigame current = MinigameManager.getInstance().getCurrent();
        if (current.isActive()) {
            current.stop();
        }

        try {
            moduleManager.unregister();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    @SuppressWarnings("") // lombok
    public static EventMain getInstance() {
        return instance;
    }

    public static Config getOkaeriConfig() {
        return okaeriConfigManager.getConfig();
    }

    public static PersistentStates getPersistentStates() {
        return okaeriConfigManager.getPersistentStates();
    }
}
