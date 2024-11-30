package dev.jsinco.luma;

import dev.jsinco.abstractjavafilelib.FileLibSettings;
import dev.jsinco.luma.commands.CommandManager;
import dev.jsinco.luma.config.Config;
import dev.jsinco.luma.shop.ShopListener;
import dev.jsinco.luma.shop.ShopManager;
import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import eu.okaeri.configs.yaml.bukkit.serdes.SerdesBukkit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public final class ThanksgivingEvent extends JavaPlugin {

    private static ThanksgivingEvent instance;
    private static Config config;
    private static ShopManager shopManager;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        FileLibSettings.set(getDataFolder());

        config = loadConfig();
        getServer().getPluginManager().registerEvents(new MineListeners(), this);
        getServer().getPluginManager().registerEvents(new ShopListener(), this);
        getCommand("event").setExecutor(new CommandManager());
        PrisonMinePlayerManager.loadAll();

        shopManager = new ShopManager();

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, PrisonMinePlayerManager::saveAll, 0, 12000);
    }

    @Override
    public void onDisable() {
        PrisonMinePlayerManager.saveAll();

        for (Player player : Bukkit.getOnlinePlayers()) {
            var holder = player.getOpenInventory().getTopInventory().getHolder(false);

            if (holder instanceof ShopManager) {
                player.closeInventory();
            }
        }
    }

    public static ThanksgivingEvent getInstance() {
        return instance;
    }

    public static void setConfig(Config config) {
        ThanksgivingEvent.config = config;
    }

    public static Config getOkaeriConfig() {
        return config;
    }

    public static ShopManager getShopManager() {
        return shopManager;
    }

    public Config loadConfig() {
        return ConfigManager.create(Config.class, (it) -> {
            it.withConfigurer(new YamlBukkitConfigurer(), new SerdesBukkit());
            it.withBindFile(getDataPath().resolve("config.yml"));
            it.withRemoveOrphans(true);
            it.saveDefaults();
            //it.withSerdesPack(registry -> registry.register(new MaterialTransformer()));
            it.load(true);
        });
    }
}