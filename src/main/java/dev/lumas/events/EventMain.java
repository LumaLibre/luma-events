package dev.lumas.events;

import dev.lumas.core.manager.Modules;
import dev.lumas.events.configurable.Config;
import dev.lumas.events.configurable.ConfigManager;
import dev.lumas.events.configurable.PersistentStates;
import dev.lumas.events.games.MinigameManager;
import dev.lumas.events.games.interfaces.Minigame;
import dev.lumas.events.games.models.CountdownBossBar;
import dev.lumas.events.items.CandiedAppleItem;
import dev.lumas.events.items.CaramelAppleItem;
import dev.lumas.events.items.LocalCustomItemManager;
import dev.lumas.events.items.StartMinigameItem;
import dev.lumas.events.manager.EventPlayerManager;
import dev.lumas.events.manager.EventTeamManager;
import dev.lumas.events.manager.LeaderboardCacheManager;
import dev.lumas.events.shop.ShopManager;
import dev.lumas.events.tasks.PlaytimeCounterTask;
import dev.lumas.lumaitems.util.extensions.Executors;
import lombok.Getter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
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
        ShopManager.getInstance().load();
        moduleManager = new Modules(this);

        moduleManager.register();

        EventPlayerManager.loadOnlinePlayers();
        EventTeamManager.loadAll();
        LeaderboardCacheManager.start();

        Executors.asyncTimer(20 * 60L, 20 * 60L, t -> EventPlayerManager.evictStale());
        MinigameManager.getInstance().repeatingAsync(0, 600);

        LocalCustomItemManager.addCustomItem(new CandiedAppleItem());
        LocalCustomItemManager.addCustomItem(new StartMinigameItem());
        LocalCustomItemManager.addCustomItem(new CaramelAppleItem());
        LocalCustomItemManager.registerCustomItems();

        // playtime counter
        PlaytimeCounterTask task = new PlaytimeCounterTask(60, TimeUnit.SECONDS);
        Bukkit.getAsyncScheduler().runAtFixedRate(this, task, 0, 60, TimeUnit.SECONDS);

        if (Bukkit.getPluginManager().getPlugin("mcMMO") != null) {
            withMcMMO = true;
        }
    }

    @Override
    public void onDisable() {
        STOPPING = true;
        moduleManager.unregister();

        EventPlayerManager.saveAllAndClear();
        Minigame current = MinigameManager.getInstance().getCurrent();
        if (current.isActive()) {
            current.stop();
        }
        ShopManager.getInstance().shutdown();
        CountdownBossBar.stopAll(false);

        for (BossBar bossBar : CountdownBossBar.activeCountdowns.stream().map(CountdownBossBar::getBossBar).toList()) {
            bossBar.removeViewer(Audience.audience(Bukkit.getOnlinePlayers()));
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
