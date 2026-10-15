package dev.lumas.events.utility.wyck;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.Register;
import dev.lumas.core.model.Service;
import dev.lumas.core.util.PluginContextLogger;
import dev.lumas.events.EventMain;
import dev.wyck.biome.Biome;
import dev.wyck.biome.CustomBiome;
import dev.wyck.environment.attribute.EnvironmentAttributes;
import dev.wyck.keys.KeyChains;
import dev.wyck.keys.ResourceKey;
import dev.wyck.renderer.packet.PacketHandler;
import dev.wyck.renderer.packet.data.VirtualBiome;
import dev.wyck.renderer.updater.BiomeUpdater;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

@Register(value = Autowire.SERVICE, requires = "Wyck")
public final class WyckBiomesService implements Service {

    public static final WyckBiomesService INSTANCE = new WyckBiomesService();
    private static final PluginContextLogger LOGGER = PluginContextLogger.getPluginLogger();

    private final Map<Manor2Biomes, Set<UUID>> viewers = new EnumMap<>(Manor2Biomes.class);
    private @Nullable PacketHandler packetHandler;
    private @Nullable BiomeUpdater biomeUpdater;

    private WyckBiomesService() {
        for (Manor2Biomes biome : Manor2Biomes.values()) {
            viewers.put(biome, ConcurrentHashMap.newKeySet());
        }
    }

    public void registerBiomes(Plugin plugin) {
        PacketHandler.Injector injector = PacketHandler.Injector.PROTOCOLLIB.isAvailable()
            ? PacketHandler.Injector.PROTOCOLLIB
            : PacketHandler.Injector.NETTY;
        PacketHandler handler = PacketHandler.of(plugin, injector);
        LOGGER.info("Using the " + injector.getName() + " injector for custom biomes");
        for (Manor2Biomes manor2Biome : Manor2Biomes.values()) {
            ResourceKey key = manor2Biome.key();
            CustomBiome customBiome = manor2Biome.biome();
            if (!KeyChains.biomes().isRegistered(key)) {
                customBiome.register();
                LOGGER.info("Registered custom biome: " + key);
            } else if (!Objects.equals(ambientLightColor(customBiome), ambientLightColor(KeyChains.biomes().get(key)))) {
                customBiome.modify();
                LOGGER.info("Modified custom biome: " + key);
            }

            if (!handler.hasBiome(key)) {
                Set<UUID> biomeViewers = viewers.get(manor2Biome);
                handler.appendBiome(manor2Biome.virtualBiome(player -> biomeViewers.contains(player.getUniqueId())));
            }
        }
        this.packetHandler = handler.register();
    }

    @Override
    public void register() {
        this.biomeUpdater = BiomeUpdater.of(EventMain.getInstance());
    }

    @Override
    public void unregister() {
        if (packetHandler != null) {
            packetHandler.unregister();
        }
        biomeUpdater = null;
        viewers.values().forEach(Set::clear);
    }

    public void show(Manor2Biomes biome, Player player) {
        if (true) return; // no op
        if (viewers.get(biome).add(player.getUniqueId())) {
            refresh(player);
        }
    }

    public void hide(Manor2Biomes biome, UUID uuid) {
        if (true) return; // no op
        if (viewers.get(biome).remove(uuid) && Bukkit.getPlayer(uuid) instanceof Player player) {
            refresh(player);
        }
    }

    private void refresh(Player player) {
        if (biomeUpdater != null) {
            biomeUpdater.updateChunksForPlayer(player);
        }
    }

    private static @Nullable Integer ambientLightColor(@Nullable Biome biome) {
        return biome == null ? null : biome.attributes().get(EnvironmentAttributes.AMBIENT_LIGHT_COLOR);
    }

    public enum Manor2Biomes {
        GHOST(
            ResourceKey.of("lumaevents:ghost"),
            key -> CustomBiome.builder()
                .resourceKey(key)
                .attribute(EnvironmentAttributes.AMBIENT_LIGHT_COLOR, "#3F255E")
                .build(),
            (key, viewer) -> VirtualBiome.builder()
                .biome(key)
                .conditional((player, chunk) -> viewer.test(player))
                .wholeChunkCondition((player, chunk) -> true)
                .build()
        );

        private final ResourceKey key;
        private final Function<ResourceKey, CustomBiome> biome;
        private final BiFunction<ResourceKey, Predicate<Player>, VirtualBiome> virtualBiome;

        Manor2Biomes(ResourceKey key, Function<ResourceKey, CustomBiome> biome, BiFunction<ResourceKey, Predicate<Player>, VirtualBiome> virtualBiome) {
            this.key = key;
            this.biome = biome;
            this.virtualBiome = virtualBiome;
        }

        public ResourceKey key() {
            return key;
        }

        public CustomBiome biome() {
            return biome.apply(key);
        }

        public VirtualBiome virtualBiome(Predicate<Player> viewer) {
            return virtualBiome.apply(key, viewer);
        }
    }
}
