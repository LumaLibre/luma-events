package dev.jsinco.luma.lumaevents;

import com.google.gson.Gson;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.utility.Util;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@AutoRegister(RegisterType.LISTENER)
public final class EventPlayerManager implements Listener {

    private static final ConcurrentHashMap<UUID, Object> LOCKS = new ConcurrentHashMap<>();
    public static final Queue<EventPlayer> EVENT_PLAYERS = new ConcurrentLinkedQueue<>();

    private static final Gson gson = Util.GSON;
    private static final Path FOLDER = EventMain.getInstance()
            .getDataPath()
            .resolve("players");
    static {
        if (!FOLDER.toFile().exists()) {
            FOLDER.toFile().mkdirs();
        }
    }

    public static boolean isLoaded(UUID uuid) {
        return EVENT_PLAYERS.stream()
                .anyMatch(eventPlayer -> eventPlayer.getUuid().equals(uuid));
    }

    public static EventPlayer load(UUID uuid) {
        Object lock = LOCKS.computeIfAbsent(uuid, k -> new Object());
        synchronized (lock) {
            if (isLoaded(uuid)) {
                for (EventPlayer eventPlayer : EVENT_PLAYERS) {
                    if (eventPlayer.getUuid().equals(uuid)) {
                        return eventPlayer;
                    }
                }
            }
            try (FileReader fileReader = new FileReader(FOLDER.resolve(uuid.toString() + ".json").toFile())) {
                EventPlayer eventPlayer = gson.fromJson(fileReader, EventPlayer.class);
                if (eventPlayer == null) {
                    eventPlayer = new EventPlayer(uuid);
                }
                EVENT_PLAYERS.add(eventPlayer);
                Util.log("Loaded EventPlayer: " + uuid);
                return eventPlayer;
            } catch (IOException ignored) {
                return new EventPlayer(uuid);
            } finally {
                LOCKS.remove(uuid);
            }
        }
    }

    public synchronized static void loadAll() {
        File[] listedFiles = FOLDER.toFile().listFiles();
        if (listedFiles == null) {
            return;
        }
        for (File file : listedFiles) {
            try (FileReader fileReader = new FileReader(file)) {
                EVENT_PLAYERS.add(gson.fromJson(fileReader, EventPlayer.class));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        EventMain.getInstance().getLogger().info("Loaded " + EVENT_PLAYERS.size() + " Players");
    }

    public static void save(EventPlayer eventPlayer) {
        try (FileWriter fileWriter = new FileWriter(FOLDER.resolve(eventPlayer.getUuid().toString() + ".json").toFile())) {
            gson.toJson(eventPlayer, fileWriter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized static void saveAll() {
        for (EventPlayer eventPlayer : EVENT_PLAYERS) {
            save(eventPlayer);
        }
        EventMain.getInstance().getLogger().info("Saved " + EVENT_PLAYERS.size() + " Players");
    }

    public static void unloadOffline(boolean save) {
        for (EventPlayer eventPlayer : EVENT_PLAYERS) {
            if (eventPlayer.getPlayer() == null) {
                if (save) save(eventPlayer);
                EVENT_PLAYERS.remove(eventPlayer);
            }
        }
    }

    public static void unload(EventPlayer eventPlayer) {
        save(eventPlayer);
        EVENT_PLAYERS.remove(eventPlayer);
        Util.log("Unloaded EventPlayer: " + eventPlayer.getUuid());
    }

    public static void unloadAll() {
        for (EventPlayer eventPlayer : EVENT_PLAYERS) {
            save(eventPlayer);
        }
        EVENT_PLAYERS.clear();
    }

    @NotNull
    public static EventPlayer getByUUID(UUID uuid) {
        Object lock = LOCKS.computeIfAbsent(uuid, k -> new Object());
        synchronized (lock) {
            for (EventPlayer eventPlayer : EVENT_PLAYERS) {
                if (eventPlayer.getUuid().equals(uuid)) {
                    return eventPlayer;
                }
            }
            return load(uuid);
        }
    }

    // Don't really need this.
    // We can just lazy load.
//    @EventHandler
//    public void onPlayerJoin(PlayerJoinEvent event) {
//        load(event.getPlayer().getUniqueId());
//    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        unload(getByUUID(event.getPlayer().getUniqueId()));
    }
}
