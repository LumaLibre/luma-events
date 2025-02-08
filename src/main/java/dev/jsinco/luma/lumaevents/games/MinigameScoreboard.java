package dev.jsinco.luma.lumaevents.games;

import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.obj.EventTeamType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MinigameScoreboard {

    private final Map<EventTeamType, Integer> teamScores = new HashMap<>();
    private final Map<EventPlayer, Integer> individualScores = new HashMap<>();

    public MinigameScoreboard() {
        for (EventTeamType team : EventTeamType.values()) {
            teamScores.put(team, 0);
        }
    }

    public void addPoints(EventPlayer player, int points) {
        individualScores.put(player, individualScores.get(player) + points);
        teamScores.put(player.getTeamType(), teamScores.get(player.getTeamType()) + points);
    }

    public void removePoints(EventPlayer player, int points) {
        individualScores.put(player, individualScores.get(player) - points);
        teamScores.put(player.getTeamType(), teamScores.get(player.getTeamType()) - points);
    }

    public void addPoints(EventTeamType team, int points) {
        teamScores.put(team, teamScores.get(team) + points);
    }

    public void removePoints(EventTeamType team, int points) {
        teamScores.put(team, teamScores.get(team) - points);
    }

    public int getPoints(EventTeamType team) {
        return teamScores.get(team);
    }

    public int getPoints(EventPlayer player) {
        return individualScores.get(player);
    }

    public int getPosition(EventTeamType team) {
        List<EventTeamType> teamsByScore = getTeamsByScore();
        return teamsByScore.indexOf(team) + 1;
    }

    public int getFinalPositionAdditionalPoints(EventTeamType team) {
        return switch (getPosition(team)) {
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
    public void distributePoints(List<EventPlayer> teamParticipants, EventTeamType team) {
        if (teamParticipants.isEmpty()) {
            return;
        }
        int points = getFinalPositionAdditionalPoints(team);
        int pointsPerPlayer = points / teamParticipants.size();
        int remainder = points % teamParticipants.size();
        for (int i = 0; i < teamParticipants.size(); i++) {
            EventPlayer player = teamParticipants.get(i);
            player.addPoints(pointsPerPlayer);
            if (i == teamParticipants.size() - 1) {
                player.addPoints(remainder);
            }
            player.sendTeamStyleMessage("You have received <gold>" + pointsPerPlayer + " additional <gray>points");
        }
    }
}
