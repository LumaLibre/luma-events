package dev.lumas.events.configurable;

import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.serializers.BlockDataSerializer;
import dev.lumas.events.configurable.serializers.BoundingBoxSerializer;
import dev.lumas.events.configurable.serializers.ColorSerializer;
import dev.lumas.events.configurable.serializers.DustOptionsSerializer;
import dev.lumas.events.configurable.serializers.KeyedSerializer;
import dev.lumas.events.configurable.serializers.NamespacedKeySerializer;
import dev.lumas.events.configurable.serializers.ParticleDefinitionSerializer;
import dev.lumas.events.configurable.serializers.SoundDefinitionSerializer;
import dev.lumas.events.configurable.serializers.VectorSerializer;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.serdes.standard.StandardSerdes;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import lombok.Getter;
import org.bukkit.Particle;
import org.bukkit.Registry;

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
                registry.register(new WorldTiedBoundingBoxTransformer());
                registry.register(new MaterialCount.Transformer());

                registry.register(new BlockDataSerializer());
                registry.register(new BoundingBoxSerializer());
                registry.register(new ColorSerializer());
                registry.register(new DustOptionsSerializer());
                registry.register(new KeyedSerializer<>(Particle.class, Registry.PARTICLE_TYPE));
                registry.register(new NamespacedKeySerializer());
                registry.register(new ParticleDefinitionSerializer());
                registry.register(new SoundDefinitionSerializer());
                registry.register(new VectorSerializer());
            });
            it.saveDefaults();
            it.load(true);
        });
    }
}
