package dev.lumas.events.utility.clientside;

import dev.lumas.events.utility.vector.Location3i;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class LocalizedFakeBlockCache {

    private static final Object SHARED_LAYER = new Object();

    private long time = 0;
    private final int renderDistance;
    private final Map<Location3i, Map<Object, CachedFakeBlock>> cache = new ConcurrentHashMap<>(); // layer per owner, so owners don't overwrite each other
    private final Map<Location3i, Map<UUID, BlockData>> shownToPlayersCache = new ConcurrentHashMap<>(); // what each player was sent

    public LocalizedFakeBlockCache(int renderDistance) {
        this.renderDistance = renderDistance;
    }

    public void setBlock(Location position, BlockData blockData, int lifetime) {
        setBlock(position, blockData, lifetime, SHARED_LAYER, ignored -> true);
    }

    public void setBlock(Location position, BlockData blockData, int lifetime, Object layer, Predicate<Player> visibleTo) {
        Location3i blockLocation = Location3i.from(position.toBlockLocation());
        CachedFakeBlock fakeBlock = new CachedFakeBlock(blockData, time + lifetime, visibleTo);
        cache.compute(blockLocation, (ignored, layers) -> {
            Map<Object, CachedFakeBlock> result = layers == null ? new ConcurrentHashMap<>() : layers;
            result.put(layer, fakeBlock);
            return result;
        });
    }

    public void showSync() {
        BlockChangeSession blockChangeSession = new BlockChangeSession();
        for (Map.Entry<Location3i, Map<Object, CachedFakeBlock>> entry : cache.entrySet()) {
            Location3i blockLocation = entry.getKey();
            Location location = blockLocation.toLocation();
            entry.getValue().values().removeIf(fakeBlock -> fakeBlock.expired() < time);
            if (cache.computeIfPresent(blockLocation, (ignored, layers) -> layers.isEmpty() ? null : layers) == null) {
                Map<UUID, BlockData> stored = shownToPlayersCache.remove(blockLocation);
                if (stored != null) {
                    revert(blockChangeSession, stored, location);
                }
                continue;
            }

            Map<UUID, BlockData> shownToPlayers = shownToPlayersCache.computeIfAbsent(blockLocation, ignored -> new ConcurrentHashMap<>());
            for (Player player : location.getNearbyPlayers(renderDistance)) {
                BlockData blockData = visibleBlockData(entry.getValue(), player);
                if (blockData == null || blockData.equals(shownToPlayers.put(player.getUniqueId(), blockData))) {
                    continue;
                }
                blockChangeSession.addBlockChange(player, location, blockData);
            }
            // Revert for players that no longer may see any layer here
            Iterator<UUID> shownIterator = shownToPlayers.keySet().iterator();
            while (shownIterator.hasNext()) {
                Player player = Bukkit.getPlayer(shownIterator.next());
                if (player == null) {
                    shownIterator.remove();
                    continue;
                }
                if (visibleBlockData(entry.getValue(), player) != null) {
                    continue;
                }
                shownIterator.remove();
                blockChangeSession.addBlockChange(player, location, location.getBlock().getBlockData());
            }
        }
        blockChangeSession.send();
        time++;
    }

    public void shutdown() {
        BlockChangeSession blockChangeSession = new BlockChangeSession();
        for (Map.Entry<Location3i, Map<UUID, BlockData>> entry : shownToPlayersCache.entrySet()) {
            revert(blockChangeSession, entry.getValue(), entry.getKey().toLocation());
        }
        blockChangeSession.send();
    }

    private static @Nullable BlockData visibleBlockData(Map<Object, CachedFakeBlock> layers, Player player) {
        for (CachedFakeBlock fakeBlock : layers.values()) {
            if (fakeBlock.visibleTo().test(player)) {
                return fakeBlock.blockData();
            }
        }
        return null;
    }

    private static void revert(BlockChangeSession blockChangeSession, Map<UUID, BlockData> shownToPlayers, Location location) {
        BlockData blockData = location.getBlock().getBlockData();
        shownToPlayers.keySet().stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .forEach(player -> {
                    int blockViewDistance = player.getSimulationDistance() * 16;
                    if (blockViewDistance * blockViewDistance < player.getLocation().distanceSquared(location)) {
                        return;
                    }
                    blockChangeSession.addBlockChange(player, location, blockData);
                });
    }

    private static class BlockChangeSession {
        private final Map<Player, Map<Location, BlockData>> toRender = new HashMap<>();

        private void addBlockChange(Player player, Location location, BlockData blockData) {
            toRender.computeIfAbsent(player, ignored -> new HashMap<>())
                    .put(location, blockData);
        }

        public void send() {
            for (Map.Entry<Player, Map<Location, BlockData>> entry : toRender.entrySet()) {
                entry.getKey().sendMultiBlockChange(entry.getValue());
            }
        }
    }


    private record CachedFakeBlock(BlockData blockData, long expired, Predicate<Player> visibleTo) {
    }

}
