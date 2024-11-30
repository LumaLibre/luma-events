package dev.jsinco.luma;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public final class PrisonMinePlayerManager {

    public static final Set<PrisonMinePlayer> PRISON_MINE_PLAYERS = new HashSet<>();

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FOLDER = ThanksgivingEvent.getInstance().getDataPath().resolve("prison_mine_players");
    static {
        if (!FOLDER.toFile().exists()) {
            FOLDER.toFile().mkdirs();
        }
    }


    public static PrisonMinePlayer load(UUID uuid) {
        PrisonMinePlayer prisonMinePlayer = null;
        try (FileReader fileReader = new FileReader(FOLDER.resolve(uuid.toString() + ".json").toFile())) {
            prisonMinePlayer = gson.fromJson(fileReader, PrisonMinePlayer.class);
        } catch (IOException ignored) {
        }
        if (prisonMinePlayer == null) {
            prisonMinePlayer = new PrisonMinePlayer(uuid);
        }
        PRISON_MINE_PLAYERS.add(prisonMinePlayer);
        return prisonMinePlayer;
    }

    public static void loadAll() {
        File[] listedFiles = FOLDER.toFile().listFiles();
        if (listedFiles == null) {
            return;
        }
        for (File file : listedFiles) {
            try (FileReader fileReader = new FileReader(file)) {
                PRISON_MINE_PLAYERS.add(gson.fromJson(fileReader, PrisonMinePlayer.class));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ThanksgivingEvent.getInstance().getLogger().info("Loaded " + PRISON_MINE_PLAYERS.size() + " PrisonMinePlayers");
    }

    public static void save(PrisonMinePlayer prisonMinePlayer) {
        try (FileWriter fileWriter = new FileWriter(FOLDER.resolve(prisonMinePlayer.getUuid().toString() + ".json").toFile())) {
            gson.toJson(prisonMinePlayer, fileWriter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveAll() {
        for (PrisonMinePlayer prisonMinePlayer : PRISON_MINE_PLAYERS) {
            save(prisonMinePlayer);
        }
        ThanksgivingEvent.getInstance().getLogger().info("Saved " + PRISON_MINE_PLAYERS.size() + " PrisonMinePlayers");
    }

    @NotNull
    public static PrisonMinePlayer getByUUID(UUID uuid) {
        for (PrisonMinePlayer prisonMinePlayer : PRISON_MINE_PLAYERS) {
            if (prisonMinePlayer.getUuid().equals(uuid)) {
                return prisonMinePlayer;
            }
        }
        return load(uuid);
    }
}
