package dev.jsinco.luma.lumaevents.games;

import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.enums.EventTeamType;
import dev.jsinco.luma.lumaevents.utility.Util;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.title.Title;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MinigameScoreboard {

    private final Map<EventTeamType, Integer> teamScores = new HashMap<>();
    private final Map<EventPlayer, Integer> individualScores = new HashMap<>();

    private int pointMultiplier;

    public MinigameScoreboard(int pointMultiplier) {
        for (EventTeamType team : EventTeamType.values()) {
            teamScores.put(team, 0);
        }
        this.pointMultiplier = pointMultiplier;
    }

    public void addScore(EventPlayer player, int points) {
        individualScores.putIfAbsent(player, 0);
        teamScores.putIfAbsent(player.getTeamType(), 0);
        individualScores.put(player, individualScores.get(player) + points);
        teamScores.put(player.getTeamType(), teamScores.get(player.getTeamType()) + points);
    }

    public void removeScore(EventPlayer player, int points) {
        individualScores.put(player, individualScores.get(player) - points);
        teamScores.put(player.getTeamType(), teamScores.get(player.getTeamType()) - points);
    }

    public void addScore(EventTeamType team, int points) {
        teamScores.put(team, teamScores.get(team) + points);
    }

    public void removeScore(EventTeamType team, int points) {
        teamScores.put(team, teamScores.get(team) - points);
    }

    public int getPoints(EventTeamType team) {
        if (!teamScores.containsKey(team)) {
            return 0;
        }
        return teamScores.get(team) * pointMultiplier;
    }

    public int getPoints(EventPlayer player) {
        if (!individualScores.containsKey(player)) {
            return 0;
        }
        return individualScores.get(player) * pointMultiplier;
    }

    public int getScore(EventTeamType team) {
        if (!teamScores.containsKey(team)) {
            return 0;
        }
        return teamScores.get(team);
    }

    public int getScore(EventPlayer player) {
        if (!individualScores.containsKey(player)) {
            return 0;
        }
        return individualScores.get(player);
    }

    public int getPosition(EventTeamType team) {
        List<EventTeamType> teamsByScore = getTeamsByScore();
        return teamsByScore.indexOf(team) + 1;
    }

    public int getPosition(EventPlayer player) {
        List<EventTeamType> teamsByScore = getTeamsByScore();
        return teamsByScore.indexOf(player.getTeamType()) + 1;
    }

    public int getFinalPositionAdditionalPoints(EventTeamType team) {
        return switch (this.getPosition(team)) {
            case 1 -> 1000;
            case 2 -> 750;
            default -> 500;
        };
    }

    public EventTeamType getLeadingTeam() {
        EventTeamType leadingTeam = EventTeamType.ROSETHORN; // Default to first team
        int leadingScore = 0;
        for (Map.Entry<EventTeamType, Integer> entry : teamScores.entrySet()) {
            if (entry.getValue() > leadingScore) {
                leadingTeam = entry.getKey();
                leadingScore = entry.getValue();
            }
        }
        return leadingTeam;
    }

    public List<EventTeamType> getTeamsByScore() {
        List<EventTeamType> teamsByScore = new ArrayList<>(teamScores.keySet());
        teamsByScore.sort((team1, team2) -> teamScores.get(team2) - teamScores.get(team1));
        return teamsByScore;
    }

    // Distribute the number of points EVENLY to the team participants
    // If there is a remainder, the last player will receive the remainder
    public void distributeAdditionalPoints(List<EventPlayer> teamParticipants, EventTeamType team) {
        if (teamParticipants.isEmpty()) {
            return;
        }
        int points = this.getFinalPositionAdditionalPoints(team);
        int pointsPerPlayer = points / teamParticipants.size();
        int remainder = points % teamParticipants.size();
        for (int i = 0; i < teamParticipants.size(); i++) {
            EventPlayer player = teamParticipants.get(i);
            player.addPoints(pointsPerPlayer);
            if (i == teamParticipants.size() - 1) {
                player.addPoints(remainder);
            }
            player.sendMessage("You have received <gold>" + pointsPerPlayer + " additional <gray>points");
        }
    }

    public void handleGameEnd(List<EventPlayer> participants, Audience audience, Runnable callback) {
        EventTeamType winner = getLeadingTeam();
        audience.showTitle(Title.title(
                Util.color("<yellow>Game over"),
                Util.color(winner.getColor() + winner.getFormatted() + " <red>team has won!")
        ));
        for (EventPlayer player : participants) {
            // Add points to player
            player.addPoints(getPoints(player));

            player.sendNoPrefixedMessage("<#eee1d5><st>                     <reset><#eee1d5>⋆⁺₊⋆ ★ ⋆⁺₊⋆<st>                     ");
            player.sendMessage("The " + winner.getTeamWithGradient() + " team <reset>has won!");
            player.sendMessage("Total scores<gray>:");
            for (EventTeamType team : getTeamsByScore()) {
                player.sendMessage(
                        team.getTeamWithGradient() + "<gray>: <gold>" + getPoints(team) + " +"
                                + getFinalPositionAdditionalPoints(team) + " additional <gray>points"
                );
            }
        }

        for (EventTeamType team : getTeamsByScore()) {
            List<EventPlayer> teamParticipants = participants.stream()
                    .filter(player -> player.getTeamType().equals(team))
                    .toList();
            distributeAdditionalPoints(teamParticipants, team);
        }
        callback.run();
    }
}
