package dev.lumas.events.obj.team;

import dev.lumas.core.util.Text;
import dev.lumas.events.games.interfaces.Scorer;
import dev.lumas.events.hooks.ChatHeadsService;
import dev.lumas.events.manager.EventPlayerManager;
import dev.lumas.events.manager.EventTeamManager;
import dev.lumas.events.obj.EventPlayer;
import dev.lumas.events.utility.Util;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

    private static final Component SPACE_TEXT_COMPONENT = Component.text(" ");
    private static final Set<UUID> TEAM_CHAT_SPIES = new HashSet<>();


    private transient final Set<UUID> inTeamChat = new HashSet<>();
    private transient final String identifier;

    private transient final String displayName;
    private transient final Component formattedDisplayName;
    private transient final String plainTextDisplayName;

    // data
    private final Set<UUID> members; // TODO Use separate object
    private final Set<UUID> disabledTeamChat; // TODO Use separate object
    private int points;



    public EventTeam(String identifier, String name, Set<UUID> members, Set<UUID> disabledTeamChat, int points) {
        this.identifier = identifier;
        this.displayName = name;
        this.members = members;
        this.disabledTeamChat = disabledTeamChat;
        this.points = points;

        this.formattedDisplayName = Util.color(name);
        this.plainTextDisplayName = PlainTextComponentSerializer.plainText().serialize(this.formattedDisplayName);
    }


    @Nullable
    public EventPlayer getPlayer(UUID uuid) {
        if (members.contains(uuid)) {
            EventPlayer eventPlayer = EventPlayerManager.getByUUID(uuid);
            eventPlayer.updateLazyTeam(this);
            return eventPlayer;
        }
        return null;
    }

    public boolean isMember(EventPlayer eventPlayer) {
        eventPlayer.updateLazyTeam(this);
        return members.contains(eventPlayer.getUuid());
    }

    public boolean addMember(EventPlayer eventPlayer) {
        EventTeam existing = EventTeamManager.getByMember(eventPlayer);
        if (existing != null) {
            throw new IllegalArgumentException("Player " + eventPlayer.getName() + " is already a member of " + existing.getName());
        }
        eventPlayer.updateLazyTeam(this);
        return members.add(eventPlayer.getUuid());
    }

    public boolean removeMember(EventPlayer eventPlayer) {
        if (!members.contains(eventPlayer.getUuid())) {
            throw new IllegalArgumentException("Player " + eventPlayer.getName() + " is not a member of " + getName());
        }
        eventPlayer.invalidateLazyTeam();
        inTeamChat.remove(eventPlayer.getUuid());
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
        return formattedDisplayName.append(Util.color("<!b> <dark_gray>»</dark_gray> " + msg).colorIfAbsent(TextColor.fromHexString(Util.TEXT_COLOR)));
    }


    public void sendTeamChat(EventPlayer sender, Component msg) {
        Player player = sender.getPlayer();
        if (player == null) {
            throw new IllegalStateException("Player is not online!");
        }
        Component finalMsg = getFormattedSender(player.getName())
                .append(msg.color(NamedTextColor.WHITE))
                .colorIfAbsent(TextColor.fromHexString(Util.TEXT_COLOR));
        ChatHeadsService chatHeadsService = ChatHeadsService.getInstance().orElse(null);

        for (Player bukkitPlayer : Bukkit.getOnlinePlayers()) {
            if (!members.contains(bukkitPlayer.getUniqueId()) && !TEAM_CHAT_SPIES.contains(bukkitPlayer.getUniqueId())) {
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
        return Text.mm(displayName + " <reset><gray>|</gray> " + sender + "<gray>:</gray> ");
    }


    public boolean togglePersistentTeamChat(EventPlayer eventPlayer) {
        if (inTeamChat.contains(eventPlayer.getUuid())) {
            inTeamChat.remove(eventPlayer.getUuid());
            return false;
        } else {
            inTeamChat.add(eventPlayer.getUuid());
            return true;
        }
    }

    public void removePersistentTeamChat(EventPlayer eventPlayer) {
        inTeamChat.remove(eventPlayer.getUuid());
    }

    public boolean isPersistentTeamChat(EventPlayer eventPlayer) {
        return inTeamChat.contains(eventPlayer.getUuid());
    }


    public int getTotalMembers() {
        return members.size();
    }

    public int getOnlineMembers() {
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (members.contains(player.getUniqueId())) {
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
