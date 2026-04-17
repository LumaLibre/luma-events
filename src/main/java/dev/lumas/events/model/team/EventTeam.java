package dev.lumas.events.model.team;

import dev.lumas.core.util.Text;
import dev.lumas.events.games.interfaces.Scorer;
import dev.lumas.events.hooks.ChatHeadsService;
import dev.lumas.events.manager.EventTeamManager;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Util;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@NullMarked
@Getter
public abstract class EventTeam implements Scorer {

    private static final Component SPACE_TEXT_COMPONENT = Component.text(" ");
    private static final Set<UUID> TEAM_CHAT_SPIES = new HashSet<>();

    private transient final String identifier;
    private transient final String displayName;
    private transient final String color;
    private transient final String chatColor;
    private transient final Component formattedDisplayName;
    private transient final String plainTextDisplayName;

    // data
    private final Map<UUID, EventTeamPlayerHandle> members;



    public EventTeam(String identifier, String name, String color, String chatColor, Map<UUID, EventTeamPlayerHandle> members) {
        this.identifier = identifier;
        this.displayName = name;
        this.color = color;
        this.chatColor = chatColor;
        this.members = members;
        this.formattedDisplayName = Util.color(name);
        this.plainTextDisplayName = PlainTextComponentSerializer.plainText().serialize(this.formattedDisplayName);
    }

    private EventTeamPlayerHandle getHandle(EventPlayer eventPlayer) {
        EventTeamPlayerHandle handle = members.get(eventPlayer.getUuid());
        if (handle != null) {
            if (!handle.isCheckedLastKnownName()) {
                handle.setCheckedLastKnownName(true);
                String currentName = eventPlayer.getName();
                if (currentName != null) {
                    handle.setLastKnownName(currentName);
                }
            }
            return handle;
        }
        throw new IllegalArgumentException("Player " + eventPlayer.getName() + " is not a member of " + getName());
    }

    @Override
    public String getName() {
        return plainTextDisplayName;
    }

    public void addPoints(EventPlayer who, int points) {
        EventTeamPlayerHandle handle = getHandle(who);
        handle.setPoints(handle.getPoints() + points);
    }

    public int getPoints(EventPlayer who) {
        return getHandle(who).getPoints();
    }

    public int getPoints() {
        return members.values().stream().mapToInt(EventTeamPlayerHandle::getPoints).sum();
    }

    public Collection<EventTeamPlayerHandle> getMembers() {
        return Set.copyOf(members.values());
    }

    public void addMember(EventPlayer eventPlayer) {
        EventTeam existing = EventTeamManager.getByMember(eventPlayer);
        if (existing != null) {
            throw new IllegalArgumentException("Player " + eventPlayer.getName() + " is already a member of " + existing.getName());
        }

        EventTeamPlayerHandle handle = new EventTeamPlayerHandle(eventPlayer);
        members.put(eventPlayer.getUuid(), handle);
        eventPlayer.updateLazyTeam(this);
    }

    public void removeMember(EventPlayer eventPlayer) {
        EventTeamPlayerHandle handle = members.remove(eventPlayer.getUuid());
        if (handle != null) {
            handle.getEventPlayer().invalidateLazyTeam();
            return;
        }
        throw new IllegalArgumentException("Player " + eventPlayer.getName() + " is not a member of " + getName());
    }

    public boolean isMember(EventPlayer eventPlayer) {
        return members.containsKey(eventPlayer.getUuid());
    }


    public EventTeamPlayerHandle getMember(EventPlayer eventPlayer) {
        EventTeamPlayerHandle handle = getHandle(eventPlayer);
        eventPlayer.updateLazyTeam(this);
        return handle;
    }


    public void sendTeamMessage(String msg) {
        Component msgComponent = formatMessage(msg);
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (members.containsKey(onlinePlayer.getUniqueId())) {
                onlinePlayer.sendMessage(msgComponent);
            }
        }
    }

    public Component formatMessage(String msg) {
        return formattedDisplayName.append(Util.color("<!b> <dark_gray>»</dark_gray> " + msg).colorIfAbsent(TextColor.fromHexString(Util.TEXT_COLOR)));
    }


    public void sendTeamChat(EventPlayer sender, Component msg) {
        Player player = sender.getPlayer();
        if (player == null) {
            throw new IllegalStateException("Player is not online!");
        }
        Component finalMsg = getFormattedSender(player.getName())
                .append(msg.color(TextColor.fromHexString(chatColor)))
                .colorIfAbsent(TextColor.fromHexString(Util.TEXT_COLOR));
        ChatHeadsService chatHeadsService = ChatHeadsService.getInstance().orElse(null);

        for (Player bukkitPlayer : Bukkit.getOnlinePlayers()) {
            if (!members.containsKey(bukkitPlayer.getUniqueId()) && !TEAM_CHAT_SPIES.contains(bukkitPlayer.getUniqueId())) {
                continue;
            }

            if (chatHeadsService != null && !chatHeadsService.isDisabled(bukkitPlayer)) {
                Component withChatHead = chatHeadsService.getChatHead(player)
                        .append(SPACE_TEXT_COMPONENT)
                        .append(finalMsg);
                bukkitPlayer.sendMessage(withChatHead);
            } else {
                bukkitPlayer.sendMessage(finalMsg);
            }

            Bukkit.getConsoleSender().sendMessage(finalMsg);
        }
    }

    public Component getFormattedSender(String sender) {
        return Text.mm(displayName + " <reset><gray>|</gray> <" + color + ">" + sender + "<gray>:</gray> ");
    }


    public int getTotalMembers() {
        return members.size();
    }

    public int getOnlineMembers() {
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (members.containsKey(player.getUniqueId())) {
                count++;
            }
        }
        return count;
    }


    public static boolean toggleTeamChatSpy(UUID uuid) {
        if (TEAM_CHAT_SPIES.contains(uuid)) {
            TEAM_CHAT_SPIES.remove(uuid);
            return false;
        }
        TEAM_CHAT_SPIES.add(uuid);
        return true;
    }
}
