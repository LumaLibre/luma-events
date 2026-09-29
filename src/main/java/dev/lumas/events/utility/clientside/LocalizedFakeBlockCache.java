package dev.lumas.events.utility.clientside;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LocalizedFakeBlockCache {

    private long time = 0;
    private final int renderDistance;
    private final Map<Location, CachedFakeBlock> cache = new ConcurrentHashMap<>();
    private final Map<Location, Set<UUID>> shownToPlayersCache = new ConcurrentHashMap<>();

    public LocalizedFakeBlockCache(int renderDistance) {
        this.renderDistance = renderDistance;
    }

    public void setBlock(Location position, BlockData blockData, int lifetime) {
        Location blockLocation = position.toBlockLocation();
        CachedFakeBlock previous = cache.put(blockLocation, new CachedFakeBlock(
                blockData, time + lifetime
        ));
        if (previous == null || !blockData.equals(previous.blockData)) {
            shownToPlayersCache.remove(blockLocation);
        }
    }

    public void showSync() {
        List<Map.Entry<Location, CachedFakeBlock>> toRemove = new ArrayList<>();
        BlockChangeSession blockChangeSession = new BlockChangeSession();
        for (Map.Entry<Location, CachedFakeBlock> entry : cache.entrySet()) {
            if (entry.getValue().expired < time) {
                toRemove.add(entry);
                continue;
            }
            cache.put(entry.getKey(), entry.getValue());
            for (Player player : entry.getKey().getNearbyPlayers(renderDistance)) {
                if (shownToPlayersCache.containsKey(entry.getKey()) &&
                        shownToPlayersCache.get(entry.getKey()).contains(player.getUniqueId())) {
                    continue;
                }
                shownToPlayersCache.computeIfAbsent(
                        entry.getKey(),
                        ignored -> Collections.newSetFromMap(new ConcurrentHashMap<>())
                ).add(player.getUniqueId());
                blockChangeSession.addBlockChange(player, entry.getKey(), entry.getValue().blockData());
            }
        }
        for (Map.Entry<Location, CachedFakeBlock> entry : toRemove) {
            Set<UUID> stored = shownToPlayersCache.remove(entry.getKey());
            cache.remove(entry.getKey(), entry.getValue());
            if (stored != null) {
                stored.stream()
                        .map(Bukkit::getPlayer)
                        .filter(Objects::nonNull)
                        .forEach(player -> {
                            int blockViewDistance = player.getSimulationDistance() * 16;
                            if (blockViewDistance * blockViewDistance < player.getLocation().distanceSquared(entry.getKey())) {
                                return;
                            }
                            blockChangeSession.addBlockChange(player, entry.getKey(), entry.getKey().getBlock().getBlockData());
                        });
            }
        }
        blockChangeSession.send();
    }

    private static class BlockChangeSession {
        Map<Player, Map<Location, BlockData>> toRender = new HashMap<>();

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


    private record CachedFakeBlock(BlockData blockData, long expired) {
    }
}
