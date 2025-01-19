package dev.jsinco.luma.lumaevents.obj;

import dev.jsinco.luma.lumaevents.EventPlayerManager;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Utility class which represents a team and all of it's points players, etc.
 */
@Setter
@Getter
@AllArgsConstructor
public class EventTeam {

    private final EventTeamType type;
    private int teamPoints;
    private final Set<EventPlayer> teamPlayers;

    public void addPoints(int points) {
        teamPoints += points;
    }

    public void addPlayer(EventPlayer player) {
        teamPlayers.add(player);
    }


    public static CompletableFuture<Set<EventTeam>> ofAsync() {
        return CompletableFuture.supplyAsync(() -> of(EventPlayerManager.EVENT_PLAYERS));
    }

    public static CompletableFuture<Set<EventTeam>> ofAsync(List<EventPlayer> playerList) {
        return CompletableFuture.supplyAsync(() -> of(playerList));
    }

    public static Set<EventTeam> of(List<EventPlayer> playerList) {
        Set<EventTeam> createdTeamObjects = new HashSet<>();
        for (EventPlayer eventPlayer : playerList) {
            EventTeamType teamType = eventPlayer.getTeamType();
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
        return createdTeamObjects;
    }
}
