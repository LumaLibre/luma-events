package dev.jsinco.luma.lumaevents;

import dev.jsinco.luma.lumacore.manager.modules.ModuleManager;
import dev.jsinco.luma.lumaevents.configurable.Config;
import dev.jsinco.luma.lumaevents.configurable.ConfigManager;
import dev.jsinco.luma.lumaevents.explorer.events.hooks.DiscordSRVListeners;
import dev.jsinco.luma.lumaevents.games.CountdownBossBar;
import dev.jsinco.luma.lumaevents.games.MinigameManager;
import dev.jsinco.luma.lumaevents.games.logic.Minigame;
import github.scarsz.discordsrv.DiscordSRV;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class EventMain extends JavaPlugin {

    @Getter
    private static EventMain instance;
    @Getter
    private static Config okaeriConfig;
    private static ModuleManager moduleManager;
    private static DiscordSRVListeners discordSRVListeners;

    @Override
    public void onEnable() {
        instance = this;
        okaeriConfig = new ConfigManager().getConfig();
        moduleManager = new ModuleManager(this);
        moduleManager.reflectivelyRegisterModules();
        EventPlayerManager.loadAll();
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, EventPlayerManager::saveAll, 0, 12000);

        MinigameManager.getInstance().runTaskTimerAsynchronously(this, 0, 600); // 30 seconds
        if (Bukkit.getPluginManager().isPluginEnabled("DiscordSRV")) {
            discordSRVListeners = new DiscordSRVListeners();
            DiscordSRV.api.subscribe(discordSRVListeners);
        }
        // TODO: Reload this plugin when LumaItems is reloaded
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

        if (discordSRVListeners != null) {
            DiscordSRV.api.unsubscribe(discordSRVListeners);
        }
    }
}
