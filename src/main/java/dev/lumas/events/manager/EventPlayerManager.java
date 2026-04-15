package dev.lumas.events.manager;

import com.google.gson.Gson;
import dev.lumas.events.EventMain;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Util;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EventPlayerManager {
    private static final long EXPIRY_MS = 10 * 60 * 1000; // 10 minutes
    private static final Gson gson = Util.GSON;
    private static final Path FOLDER = EventMain.getInstance()
            .getDataPath()
            .resolve("players");

    private static final Map<UUID, CachedPlayer> cache = new ConcurrentHashMap<>();

    static {
        if (!FOLDER.toFile().exists()) {
            FOLDER.toFile().mkdirs();
        }
    }

    private record CachedPlayer(EventPlayer player, long lastAccessed) {}

    /**
     * Gets a player by UUID, loading from disk if necessary.
     * Resets the expiry timer on each access.
     */
    @NotNull
    public static EventPlayer getByUUID(UUID uuid) {
        CachedPlayer cached = cache.compute(uuid, (key, existing) -> {
            if (existing != null) {
                return new CachedPlayer(existing.player(), System.currentTimeMillis());
            }
            EventMain.getInstance().getLogger().info("Loading player " + key + " from disk");
            return new CachedPlayer(loadFromDisk(key), System.currentTimeMillis());
        });
        return cached.player();
    }

    @Nullable
    public static EventPlayer getByUUIDOrNull(UUID uuid) {
        CachedPlayer cached = cache.computeIfPresent(uuid, (key, existing) ->
                new CachedPlayer(existing.player(), System.currentTimeMillis()));
        return cached != null ? cached.player() : null;
    }


    public static void evictStale() {
        long now = System.currentTimeMillis();
        cache.forEach((uuid, cached) -> {
            if (now - cached.lastAccessed() >= EXPIRY_MS && !cached.player().isOnline()) {
                cache.remove(uuid);
                save(cached.player());
            }
        });
    }

    private static EventPlayer loadFromDisk(UUID uuid) {
        File file = FOLDER.resolve(uuid + ".json").toFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                EventPlayer player = gson.fromJson(reader, EventPlayer.class);
                if (player != null) return player;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new EventPlayer(uuid);
    }

    public static void save(EventPlayer eventPlayer) {
        try (FileWriter writer = new FileWriter(FOLDER.resolve(eventPlayer.getUuid() + ".json").toFile())) {
            gson.toJson(eventPlayer, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void saveAll() {
        EventMain.getInstance().getLogger().info("Saving " + cache.size() + " players");
        cache.forEach((uuid, cached) -> save(cached.player()));
    }

    public static void saveAllAndClear() {
        saveAll();
        cache.clear();
    }

    public static void loadOnlinePlayers() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            getByUUID(player.getUniqueId());
        });
        EventMain.getInstance().getLogger().info("Loaded " + cache.size() + " players");
    }

    public static List<EventPlayer> loadAllFromDisk() {
        File[] files = FOLDER
                .toFile()
                .listFiles();

        if (files == null) return List.of();

        List<EventPlayer> players = new ArrayList<>();
        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                EventPlayer player = Util.GSON.fromJson(reader, EventPlayer.class);
                if (player != null) players.add(player);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return players;
    }

    public static int cacheSize() {
        return cache.size();
    }

}