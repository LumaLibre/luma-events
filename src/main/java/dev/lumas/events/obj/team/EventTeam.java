package dev.lumas.events.obj.team;

import dev.lumas.events.games.interfaces.Scorer;
import dev.lumas.events.manager.EventPlayerManager;
import dev.lumas.events.manager.EventTeamManager;
import dev.lumas.events.obj.EventPlayer;
import dev.lumas.events.utility.Util;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@NullMarked
@Getter
public abstract class EventTeam implements Scorer {

    private transient final String identifier;
    private transient final Component displayName;
    private final Set<UUID> members;
    private int points;

    private transient final String plainTextDisplayName;

    public EventTeam(String identifier, Component displayName, Set<UUID> members, int points) {
        this.identifier = identifier;
        this.displayName = displayName;
        this.members = members;
        this.points = points;

        this.plainTextDisplayName = PlainTextComponentSerializer.plainText().serialize(displayName);
    }


    @Nullable
    public EventPlayer getPlayer(UUID uuid) {
        if (members.contains(uuid)) {
            return EventPlayerManager.getByUUID(uuid);
        }
        return null;
    }

    public boolean isMember(EventPlayer eventPlayer) {
        return members.contains(eventPlayer.getUuid());
    }

    public boolean addMember(EventPlayer eventPlayer) {
        EventTeam existing = EventTeamManager.getByMember(eventPlayer);
        if (existing != null) {
            throw new IllegalArgumentException("Player " + eventPlayer.getName() + " is already a member of " + existing.getName());
        }
        return members.add(eventPlayer.getUuid());
    }

    public boolean removeMember(EventPlayer eventPlayer) {
        if (!members.contains(eventPlayer.getUuid())) {
            throw new IllegalArgumentException("Player " + eventPlayer.getName() + " is not a member of " + getName());
        }
        return members.remove(eventPlayer.getUuid());
    }


    public void addPoints(int points) {
        this.points += points;
    }

    @Override
    public String getName() {
        return plainTextDisplayName;
    }

    public void sendTeamMessage(String msg) {
        Component msgComponent = formatMessage(msg);
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (members.contains(onlinePlayer.getUniqueId())) {
                onlinePlayer.sendMessage(msgComponent);
            }
        }
    }

    public Component formatMessage(String msg) {
        return displayName.append(Util.color("<!b> <dark_gray>»</dark_gray> " + msg).colorIfAbsent(TextColor.fromHexString(Util.TEXT_COLOR)));
    }

    public static <T extends EventTeam> T instance(Class<T> teamClass) {
        try {
            return teamClass.getConstructor(Set.class, int.class).newInstance( new HashSet<>(), 0);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance of " + teamClass.getName(), e);
        }
    }
}
