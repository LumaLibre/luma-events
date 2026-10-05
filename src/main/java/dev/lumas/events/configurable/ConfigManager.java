package dev.lumas.events.configurable;

import com.google.gson.JsonParseException;
import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.serializers.BlockDataSerializer;
import dev.lumas.events.configurable.serializers.BoundingBoxSerializer;
import dev.lumas.events.configurable.serializers.ColorSerializer;
import dev.lumas.events.configurable.serializers.DustOptionsSerializer;
import dev.lumas.events.configurable.serializers.KeyedSerializer;
import dev.lumas.events.configurable.serializers.LocationSerializer;
import dev.lumas.events.configurable.serializers.MaterialCountSerializer;
import dev.lumas.events.configurable.serializers.NamespacedKeySerializer;
import dev.lumas.events.configurable.serializers.ParticleDefinitionSerializer;
import dev.lumas.events.configurable.serializers.SoundDefinitionSerializer;
import dev.lumas.events.configurable.serializers.VectorSerializer;
import dev.lumas.events.configurable.serializers.WorldTiedBoundingBoxSerializer;
import dev.lumas.events.games.constants.MinigameConstant;
import dev.lumas.events.utility.Util;
import dev.lumas.events.utility.effects.ParticleDefinition;
import lombok.Getter;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemType;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

@Getter
public class ConfigManager {

    private static final String CONFIG_FILE = "config.yml";
    private static final String MINIGAMES_FOLDER = "minigames";
    private static final String STATE_FILE = "minigame-state.json";
    private static final String SHARED_SECTION = "commons";
    private static final String MAPS_SECTION = "maps";
    private static final String DEFAULT_MAP = "default";

    private final Path dataPath;
    private final TypeSerializerCollection serializers;

    private volatile Config config;
    private volatile Map<MinigameConstant, Map<String, Object>> definitions;
    private volatile GlobalStates persistentStates;

    public ConfigManager() {
        this.dataPath = EventMain.getInstance().getDataPath();
        this.serializers = createSerializers();
        try {
            reload();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load LumaEvents configuration", e);
        }
    }

    public void reload() throws IOException {
        YamlConfigurationLoader configLoader = loader(this.dataPath.resolve(CONFIG_FILE));
        CommentedConfigurationNode configRoot = configLoader.load();
        Config config = loadConfig(configRoot);
        Map<MinigameConstant, Map<String, Object>> definitions = loadDefinitions(configRoot);
        configLoader.save(configRoot);
        GlobalStates persistentStates = loadPersistentStates();

        this.config = config;
        this.definitions = definitions;
        this.persistentStates = persistentStates;
    }

    public Map<String, Object> getDefinitions(MinigameConstant minigame) {
        return this.definitions.getOrDefault(minigame, Map.of());
    }

    public synchronized void savePersistentStates() {
        try {
            writePersistentStates(this.persistentStates);
        } catch (IOException e) {
            EventMain.getInstance().getLogger().log(Level.SEVERE, "Failed to save " + STATE_FILE, e);
        }
    }

    private Config loadConfig(CommentedConfigurationNode root) throws SerializationException {
        ConfigSchema.normalizeKeys(root, Config.class);
        ConfigSchema.merge(root, serialize(root, Config.class, new Config()), Config.class, false);
        return root.get(Config.class);
    }

    private Map<MinigameConstant, Map<String, Object>> loadDefinitions(CommentedConfigurationNode legacyConfig) throws IOException {
        Path folder = this.dataPath.resolve(MINIGAMES_FOLDER);
        Files.createDirectories(folder);

        Map<MinigameConstant, Map<String, Object>> definitions = new EnumMap<>(MinigameConstant.class);
        for (MinigameConstant minigame : MinigameConstant.values()) {
            Path path = folder.resolve(minigame.name().toLowerCase(Locale.ROOT) + ".yml");
            definitions.put(minigame, loadDefinitions(path, minigame, legacyConfig));
        }
        return Collections.unmodifiableMap(definitions);
    }

    private Map<String, Object> loadDefinitions(Path path, MinigameConstant minigame, CommentedConfigurationNode legacyConfig) throws ConfigurateException {
        Class<?> definitionType = minigame.getDefinitionType();
        YamlConfigurationLoader loader = loader(path);
        CommentedConfigurationNode root = loader.load();
        CommentedConfigurationNode shared = root.node(SHARED_SECTION);
        CommentedConfigurationNode maps = root.node(MAPS_SECTION);

        CommentedConfigurationNode defaultShared = CommentedConfigurationNode.root(root.options());
        CommentedConfigurationNode defaultMap = CommentedConfigurationNode.root(root.options());
        ConfigSchema.split(serialize(root, definitionType, newDefinition(definitionType)), definitionType, defaultShared, defaultMap);

        if (root.empty() && OkaeriConfigMigration.migrateMaps(legacyConfig, minigame, defaultShared, shared, maps)) {
            EventMain.getInstance().getLogger().info("Migrated " + minigame.getDisplayName() + " maps from " + CONFIG_FILE + " to " + MINIGAMES_FOLDER + "/" + path.getFileName());
        }

        ConfigSchema.normalizeKeys(shared, definitionType);
        ConfigSchema.merge(shared, defaultShared, definitionType, false);
        if (maps.virtual()) {
            maps.node(DEFAULT_MAP).from(defaultMap);
        }

        Map<String, Object> definitions = new LinkedHashMap<>();
        for (Map.Entry<Object, CommentedConfigurationNode> entry : maps.childrenMap().entrySet()) {
            CommentedConfigurationNode map = entry.getValue();
            ConfigSchema.normalizeKeys(map, definitionType);
            ConfigSchema.merge(map, defaultMap, definitionType, false);

            CommentedConfigurationNode effective = CommentedConfigurationNode.root(root.options()).from(shared);
            ConfigSchema.merge(effective, map, definitionType, true);
            try {
                definitions.put(String.valueOf(entry.getKey()), effective.get(definitionType));
            } catch (SerializationException e) {
                throw new ConfigurateException(map, "Invalid map '" + entry.getKey() + "' in " + path.getFileName(), e);
            }
        }

        loader.save(root);
        return Collections.unmodifiableMap(definitions);
    }

    private static CommentedConfigurationNode serialize(CommentedConfigurationNode context, Type type, Object value) throws SerializationException {
        CommentedConfigurationNode node = CommentedConfigurationNode.root(context.options());
        node.set(type, value);
        return node;
    }

    private static Object newDefinition(Class<?> definitionType) {
        try {
            return definitionType.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Minigame definition " + definitionType.getName() + " needs a no-arg constructor", e);
        }
    }

    private GlobalStates loadPersistentStates() throws IOException {
        Path path = this.dataPath.resolve(STATE_FILE);
        if (Files.notExists(path)) {
            GlobalStates persistentStates = new GlobalStates();
            writePersistentStates(persistentStates);
            return persistentStates;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            GlobalStates persistentStates = Util.GSON.fromJson(reader, GlobalStates.class);
            return persistentStates != null ? persistentStates : new GlobalStates();
        } catch (JsonParseException e) {
            throw new IOException("Malformed " + STATE_FILE, e);
        }
    }

    private void writePersistentStates(GlobalStates persistentStates) throws IOException {
        Path path = this.dataPath.resolve(STATE_FILE);
        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path)) {
            Util.GSON.toJson(persistentStates, writer);
        }
    }

    private YamlConfigurationLoader loader(Path path) {
        return YamlConfigurationLoader.builder()
                .path(path)
                .nodeStyle(NodeStyle.BLOCK)
                .indent(2)
                .defaultOptions(options -> options.serializers(this.serializers))
                .build();
    }

    private static TypeSerializerCollection createSerializers() {
        ObjectMapper.Factory objectMapper = ObjectMapper.factoryBuilder()
                .defaultNamingScheme(ConfigSchema.NAMING_SCHEME)
                .build();

        return TypeSerializerCollection.defaults().childBuilder()
                .register(new LocationSerializer())
                .register(new WorldTiedBoundingBoxSerializer())
                .registerExact(new BoundingBoxSerializer())
                .register(new MaterialCountSerializer())
                .register(new BlockDataSerializer())
                .register(new ColorSerializer())
                .register(Particle.DustOptions.class, new DustOptionsSerializer())
                .register(new KeyedSerializer<>(Particle.class, Registry.PARTICLE_TYPE))
                .register(new KeyedSerializer<>(ItemType.class, Registry.ITEM))
                .register(new NamespacedKeySerializer())
                .register(ParticleDefinition.class, new ParticleDefinitionSerializer())
                .register(new SoundDefinitionSerializer())
                .register(new VectorSerializer())
                .registerAnnotatedObjects(objectMapper)
                .build();
    }
}
