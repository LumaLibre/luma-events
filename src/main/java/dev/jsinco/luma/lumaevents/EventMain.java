package dev.jsinco.luma.lumaevents;

import dev.jsinco.luma.lumaevents.guis.ChallengesGui;
import dev.jsinco.luma.lumaevents.items.CustomItemsManager;
import dev.jsinco.luma.lumacore.manager.modules.ModuleManager;
import dev.jsinco.luma.lumaevents.obj.EventPlayerManager;
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
        CustomItemsManager.register();
        EventPlayerManager.loadAll();
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, EventPlayerManager::saveAll, 0, 12000);


        getServer().getPluginManager().registerEvents(new HookedListeners(), this);
    }

    @Override
    public void onDisable() {
        moduleManager.unregisterModules();

        EventPlayerManager.saveAll();

        for (Player player : Bukkit.getOnlinePlayers()) {
            var holder = player.getOpenInventory().getTopInventory().getHolder(false);

            if (holder instanceof ChallengesGui) {
                player.closeInventory();
            }
        }
    }

    public static EventMain getInstance() {
        return instance;
    }
}
