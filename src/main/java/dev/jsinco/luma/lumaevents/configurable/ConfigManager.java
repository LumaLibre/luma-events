package dev.jsinco.luma.lumaevents.configurable;

import dev.jsinco.luma.lumaevents.EventMain;
import eu.okaeri.configs.serdes.standard.StandardSerdes;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import lombok.Getter;

import java.nio.file.Path;

@Getter
public class ConfigManager {

    private final Config config;

    public ConfigManager() {
        Path configPath = EventMain.getInstance().getDataPath().resolve("config.yml");

        this.config = eu.okaeri.configs.ConfigManager.create(Config.class, (it) -> {
            it.withConfigurer(new YamlBukkitConfigurer(), new StandardSerdes());
            it.withRemoveOrphans(false);
            it.withBindFile(configPath);

            it.withSerdesPack(registry -> {
                registry.register(new LocationTransformer());
            });
            it.saveDefaults();
            it.load(true);
        });
    }
}
