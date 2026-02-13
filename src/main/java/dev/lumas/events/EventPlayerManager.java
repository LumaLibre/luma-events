package dev.lumas.events;

import com.google.gson.Gson;
import dev.lumas.events.obj.EventPlayer;
import dev.lumas.events.utility.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// FIXME: lazy temporary solution, please lazy load players
public final class EventPlayerManager {

    // FIXME: This needs to lazy load players
    public static final List<EventPlayer> EVENT_PLAYERS = new ArrayList<>();

    private static final Gson gson = Util.GSON;
    private static final Path FOLDER = EventMain.getInstance()
            .getDataPath()
            .resolve("players");
    static {
        if (!FOLDER.toFile().exists()) {
            FOLDER.toFile().mkdirs();
        }
    }

    public static List<EventPlayer> eventPlayers() {
        return List.copyOf(EVENT_PLAYERS);
    }

    public static EventPlayer load(UUID uuid) {
        EventPlayer eventPlayer = null;
        try (FileReader fileReader = new FileReader(FOLDER.resolve(uuid.toString() + ".json").toFile())) {
            eventPlayer = gson.fromJson(fileReader, EventPlayer.class);
        } catch (IOException ignored) {
        }
        if (eventPlayer == null) {
            eventPlayer = new EventPlayer(uuid);
        }
        EVENT_PLAYERS.add(eventPlayer);
        return eventPlayer;
    }

    public static void loadAll() {
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

    public static void saveAll() {
        for (EventPlayer prisonMinePlayer : EVENT_PLAYERS) {
            save(prisonMinePlayer);
        }
        EventMain.getInstance().getLogger().info("Saved " + EVENT_PLAYERS.size() + " Players");
    }

    @NotNull
    public static EventPlayer getByUUID(UUID uuid) {
        for (EventPlayer eventPlayer : EVENT_PLAYERS) {
            if (eventPlayer.getUuid().equals(uuid)) {
                return eventPlayer;
            }
        }
        return load(uuid);
    }


    @Nullable
    public static EventPlayer getByUUIDOrNull(UUID uuid) {
        for (EventPlayer eventPlayer : EVENT_PLAYERS) {
            if (eventPlayer.getUuid().equals(uuid)) {
                return eventPlayer;
            }
        }
        return null;
    }

}