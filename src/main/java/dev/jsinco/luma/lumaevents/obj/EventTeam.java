package dev.jsinco.luma.lumaevents.obj;

import dev.jsinco.chatheads.Handler;
import dev.jsinco.chatheads.integration.ChatHeadsAPI;
import dev.jsinco.chatheads.obj.CachedPlayer;
import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.enums.EventReward;
import dev.jsinco.luma.lumaevents.enums.EventTeamType;
import dev.jsinco.luma.lumaevents.utility.Util;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Utility class which represents a team and all of it's points players, etc.
 */
@Setter
@Getter
@AllArgsConstructor
public class EventTeam {

    private static final Component SPACE_TEXT_COMPONENT = Component.text(" ");

    private final EventTeamType type;
    private int teamPoints;
    private final Set<EventPlayer> teamPlayers;

    // Constructing
    public void addPoints(int points) {
        teamPoints += points;
    }

    public void addPlayer(EventPlayer player) {
        teamPlayers.add(player);
    }

    // Operations on all players
    public void addTeamReward(EventReward reward) {
        for (EventPlayer player : teamPlayers) {
            player.addReward(reward);
        }
    }

    public void title(String title, String subtitle) {
        for (EventPlayer player : teamPlayers) {
            player.getPlayer().showTitle(Title.title(
                    Util.color(title),
                    Util.color(subtitle)
            ));
        }
    }

    public void msg(CommandSender player, String msg) {
        player.sendMessage(Util.color(type.getColor() + msg));
    }


    public void teamMsg(Player sender, Component msg) {
        Component chathead = ChatHeadsAPI.getChatHead(sender);
        for (EventPlayer player : teamPlayers) {
            CachedPlayer cachedPlayer = Handler.getCachedPlayer(sender);

            if (cachedPlayer.isDisabledChatHead()) {
                player.sendNoPrefixedMessage(Util.color(getFormattedSender(sender.getName())).append(msg));
                continue;
            }

            Component finalComponent;
            if (cachedPlayer.doNotReverseOrientation()) {
                finalComponent = chathead
                        .append(SPACE_TEXT_COMPONENT)
                        .append(Util.color(getFormattedSender(sender.getName()))
                        .append(msg));
            } else {
                finalComponent = Util.color(getFormattedSender(sender.getName()))
                        .append(msg)
                        .append(SPACE_TEXT_COMPONENT)
                        .append(chathead);
            }

            player.sendNoPrefixedMessage(finalComponent);
        }
        Bukkit.getConsoleSender().sendMessage(
                Util.color(getFormattedSender(sender.getName()))
                        .append(msg)
        );
    }

    public String getFormattedSender(String sender) {
        return "<b>"+type.getColor()+"Team <reset><gray>| "+type.getColor()+sender+"<gray>: "+type.getColor();
    }

    public static CompletableFuture<Set<EventTeam>> ofAsync() {
        return CompletableFuture.supplyAsync(() -> of(EventPlayerManager.EVENT_PLAYERS, false));
    }

    public static CompletableFuture<Set<EventTeam>> ofAsync(boolean sorted) {
        return CompletableFuture.supplyAsync(() -> of(EventPlayerManager.EVENT_PLAYERS, sorted));
    }

    public static CompletableFuture<Set<EventTeam>> ofAsync(List<EventPlayer> playerList) {
        return CompletableFuture.supplyAsync(() -> of(playerList, false));
    }

    public static CompletableFuture<Set<EventTeam>> ofAsync(List<EventPlayer> playerList, boolean sorted) {
        return CompletableFuture.supplyAsync(() -> of(playerList, sorted));
    }

    public static Set<EventTeam> of() {
        return of(EventPlayerManager.EVENT_PLAYERS, false);
    }

    public static Set<EventTeam> of(boolean sorted) {
        return of(EventPlayerManager.EVENT_PLAYERS, sorted);
    }

    public static Set<EventTeam> of(List<EventPlayer> playerList, boolean sorted) {
        Set<EventTeam> createdTeamObjects = new HashSet<>();
        for (EventPlayer eventPlayer : playerList) {
            EventTeamType teamType = eventPlayer.getTeamType();
            if (teamType == null) {
                continue;
            }
            EventTeam team = createdTeamObjects.stream()
                    .filter(eventTeam -> eventTeam.getType().equals(teamType))
                    .findFirst()
                    .orElseGet(() -> {
                        EventTeam newTeam = new EventTeam(teamType, 0, new HashSet<>());
                        createdTeamObjects.add(newTeam);
                        return newTeam;
                    });
            team.addPoints(eventPlayer.getPoints());
            team.addPlayer(eventPlayer);
        }

        for (EventTeamType type : EventTeamType.values()) {
            if (createdTeamObjects.stream().noneMatch(team -> team.getType().equals(type))) {
                createdTeamObjects.add(ofEmptyTeam(type));
            }
        }

        // Sorted by points
        // If all points are empty: rosethorn, sweethearts, heartbreakers
        if (sorted) {
            return createdTeamObjects.stream()
                    .sorted((team1, team2) -> {
                        int pointComparison = Integer.compare(team2.getTeamPoints(), team1.getTeamPoints());
                        if (pointComparison == 0) {
                            return team1.getType().compareTo(team2.getType());
                        }
                        return pointComparison;
                    })
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return createdTeamObjects;
    }

    public static EventTeam ofEmptyTeam(EventTeamType type) {
        return new EventTeam(type, 0, new HashSet<>());
    }

    public static EventTeam ofOnlinePlayers(EventTeamType type) {
        EventTeam team = new EventTeam(type, 0, new HashSet<>());
        for (Player player : Bukkit.getOnlinePlayers()) {
            EventPlayer eventPlayer = EventPlayerManager.getByUUID(player.getUniqueId());
            if (eventPlayer.getTeamType() != null && eventPlayer.getTeamType() == type) {
                team.addPlayer(eventPlayer);
                team.addPoints(eventPlayer.getPoints());
            }
        }
        return team;
    }
}
