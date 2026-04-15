package dev.lumas.events.manager;

import com.google.gson.Gson;
import dev.lumas.events.EventMain;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.model.team.EventTeam;
import dev.lumas.events.model.team.IvoryTeam;
import dev.lumas.events.model.team.ScarletTeam;
import dev.lumas.events.utility.gson.GsonHolder;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class EventTeamManager {

    private static final List<EventTeam> EVENT_TEAMS = new ArrayList<>();
    private static final Gson GSON = GsonHolder.GSON;
    private static final Path FOLDER = EventMain.getInstance()
            .getDataPath()
            .resolve("teams");


    static {
        if (!FOLDER.toFile().exists()) {
            FOLDER.toFile().mkdirs();
        }
    }

    public static List<EventTeam> eventTeams() {
        return List.copyOf(EVENT_TEAMS);
    }

    public static <T extends EventTeam> T load(Class<T> teamClass) {
        Provider provider = Provider.fromClass(teamClass);
        EventTeam eventTeam = null;
        try (FileReader fileReader = new FileReader(FOLDER.resolve(provider + ".json").toFile())) {
            eventTeam = GSON.fromJson(fileReader, teamClass);
        } catch (IOException ignored) {
        }
        if (eventTeam == null) {
            eventTeam = provider.newInstance();
        }
        EVENT_TEAMS.add(eventTeam);
        return (T) eventTeam;
    }

    public static void loadAll() {
        for (Provider provider : Provider.values()) {
            load(provider.getTeamClass());
        }
        EventMain.getInstance().getLogger().info("Loaded " + EVENT_TEAMS.size() + " teams.");
    }

    public static void save(EventTeam eventTeam) {
        Provider provider = Provider.fromClass(eventTeam.getClass());
        try (FileWriter fileWriter = new FileWriter(FOLDER.resolve(provider + ".json").toFile())) {
            GSON.toJson(eventTeam, fileWriter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveAll() {
        for (EventTeam eventTeam : EVENT_TEAMS) {
            save(eventTeam);
        }
        //EventMain.getInstance().getLogger().info("Saved " + EVENT_TEAMS.size() + " teams.");
    }


    @SuppressWarnings("unchecked")
    public static <T extends EventTeam> T getByProvider(Provider provider) {
        return (T) getByClass(provider.getTeamClass());
    }


    @SuppressWarnings("unchecked")
    public static <T extends EventTeam> T getByClass(Class<T> teamClass) {
        return (T) EVENT_TEAMS.stream()
                .filter(team -> team.getClass().equals(teamClass))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No team found for class: " + teamClass.getName()));
    }

    @Nullable
    public static EventTeam getByMember(EventPlayer eventPlayer) {
        for (EventTeam eventTeam : EVENT_TEAMS) {
            if (eventTeam.isMember(eventPlayer)) {
                eventPlayer.updateLazyTeam(eventTeam);
                return eventTeam;
            }
        }
        return null;
    }

    @NonNull
    public static EventTeam getByMemberOrThrow(EventPlayer eventPlayer) {
        EventTeam team = getByMember(eventPlayer);
        if (team == null) {
            throw new IllegalArgumentException("Player " + eventPlayer.getName() + " is not a member of any team.");
        }
        return team;
    }


    public enum Provider {
        IVORY(IvoryTeam.class, IvoryTeam::new),
        SCARLET(ScarletTeam.class, ScarletTeam::new);

        @Getter
        private final Class<? extends EventTeam> teamClass;
        private final Supplier<EventTeam> supplier;

        Provider(Class<? extends EventTeam> teamClass, Supplier<EventTeam> supplier) {
            this.teamClass = teamClass;
            this.supplier = supplier;
        }

        public EventTeam newInstance() {
            return supplier.get();
        }

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }

        public static Provider fromClass(Class<? extends EventTeam> teamClass) {
            for (Provider provider : values()) {
                if (provider.teamClass.equals(teamClass)) {
                    return provider;
                }
            }
            throw new IllegalArgumentException("No provider found for class: " + teamClass.getName());
        }
    }
}
