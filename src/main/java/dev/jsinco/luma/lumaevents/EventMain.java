package dev.jsinco.luma.lumaevents;

import dev.jsinco.luma.lumaevents.items.CustomItemsManager;
import dev.jsinco.luma.manager.modules.ModuleManager;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public class EventMain extends JavaPlugin {

    @Getter
    private static EventMain instance;
    private static ModuleManager moduleManager;

    @Override
    public void onEnable() {
        instance = this;
        moduleManager = new ModuleManager(this);
        moduleManager.reflectivelyRegisterModules();
        CustomItemsManager.register();
    }

    @Override
    public void onDisable() {
        moduleManager.unregisterModules();
    }
}
