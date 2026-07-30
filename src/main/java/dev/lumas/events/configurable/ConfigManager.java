package dev.lumas.events.configurable;

import dev.lumas.events.EventMain;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.serdes.standard.StandardSerdes;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import lombok.Getter;

import java.nio.file.Path;

@Getter
public class ConfigManager {

    private final Config config;
    private final PersistentStates persistentStates;

    public ConfigManager() {
        Path dataPath = EventMain.getInstance().getDataPath();

        this.config = loadConfig(Config.class, dataPath.resolve("config.yml"));
        this.persistentStates = loadConfig(PersistentStates.class, dataPath.resolve("minigame-state.yml"));
    }

    private <T extends OkaeriConfig> T loadConfig(Class<T> configClass, Path path) {
        return eu.okaeri.configs.ConfigManager.create(configClass, (it) -> {
            it.withConfigurer(new YamlBukkitConfigurer(), new StandardSerdes());
            it.withRemoveOrphans(false);
            it.withBindFile(path);

            it.withSerdesPack(registry -> {
                registry.register(new LocationTransformer());
                registry.register(new BoundingBoxTransformer());
                registry.register(new MaterialCount.Transformer());
            });
            it.saveDefaults();
            it.load(true);
        });
    }
}
