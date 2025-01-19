package dev.jsinco.luma.lumaevents.configurable;

import dev.jsinco.luma.lumaevents.EventMain;
import eu.okaeri.configs.serdes.standard.StandardSerdes;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import lombok.Getter;

public class ConfigManager {

    @Getter
    private final Config config;
    @Getter
    private static final ConfigManager instance = new ConfigManager();

    public ConfigManager() {
        this.config = eu.okaeri.configs.ConfigManager.create(Config.class, (it) -> {
            it.withConfigurer(new YamlBukkitConfigurer(), new StandardSerdes());
            it.withRemoveOrphans(true);
            it.withBindFile(EventMain.getInstance().getDataPath().resolve("config.yml"));

            it.withSerdesPack(registry -> {
                registry.register(new LocationTransformer());
            });
            it.saveDefaults();
            it.load(true);
        });
    }
}
