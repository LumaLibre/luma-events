package dev.jsinco.luma;

import dev.jsinco.luma.commands.CommandManager;
import dev.jsinco.luma.config.Config;
import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import eu.okaeri.configs.yaml.bukkit.serdes.SerdesBukkit;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public final class ThanksgivingEvent extends JavaPlugin {

    private static ThanksgivingEvent instance;
    private static Config config;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        config = loadConfig();
        getServer().getPluginManager().registerEvents(new PrisonMineGame(), this);
        getCommand("event").setExecutor(new CommandManager());
        PrisonMinePlayerManager.loadAll();


        Bukkit.getScheduler().runTaskTimerAsynchronously(this, PrisonMinePlayerManager::saveAll, 0, 12000);
    }

    @Override
    public void onDisable() {
        PrisonMinePlayerManager.saveAll();
    }

    public static ThanksgivingEvent getInstance() {
        return instance;
    }

    public static Config getOkaeriConfig() {
        return config;
    }


    private Config loadConfig() {
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