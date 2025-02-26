package dev.jsinco.luma.lumaevents.games;

import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.enums.EventTeamType;
import dev.jsinco.luma.lumaevents.utility.Util;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.title.Title;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MinigameScoreboard {

    private final Map<EventPlayer, Integer> individualScores = new HashMap<>();

    @Getter @Setter
    private int pointMultiplier;

    public MinigameScoreboard(int pointMultiplier) {
        this.pointMultiplier = pointMultiplier;
    }

    public boolean hasScore(EventPlayer player) {
        return individualScores.containsKey(player);
    }

    public void addScore(EventPlayer player, int points) {
        individualScores.putIfAbsent(player, 0);
        individualScores.put(player, individualScores.get(player) + points);
    }

    public void removeScore(EventPlayer player) {
        individualScores.remove(player);
    }


    public int getPoints(EventTeamType team) {
        return this.getScore(team) * pointMultiplier;
    }

    public int getPoints(EventPlayer player) {
        if (!individualScores.containsKey(player)) {
            return 0;
        }
        return individualScores.get(player) * pointMultiplier;
    }

    public int getScore(EventTeamType team) {
        int points = 0;
        for (Map.Entry<EventPlayer, Integer> entry : individualScores.entrySet()) {
            EventTeamType playerTeam = entry.getKey().getTeamType();
            if (playerTeam != null && playerTeam.equals(team)) {
                points += entry.getValue();
            }
        }
        return points;
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
        List<EventPlayer> playersByScore = new ArrayList<>(individualScores.keySet());
        playersByScore.sort((player1, player2) -> individualScores.get(player2) - individualScores.get(player1));
        return playersByScore.indexOf(player) + 1;
    }

    public int getFinalPositionAdditionalPoints(EventTeamType team) {
        // scaling a bit too volatile for me to handle atm, just going to flat until p3
        return switch (this.getPosition(team)) {
            case 1 -> 500;
            case 2 -> 750;
            default -> 1300;
        };
    }

    public EventTeamType getLeadingTeam() {
        Map<EventTeamType, Integer> teamScores = new HashMap<>();
        for (EventTeamType teamType : EventTeamType.values()) {
            teamScores.put(teamType, this.getScore(teamType));
        }
        return teamScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(EventTeamType.ROSETHORN); // Default to Rosethorn
    }

    public List<EventTeamType> getTeamsByScore() {
        // order with the team with the highest score first
        Map<EventTeamType, Integer> teamScores = new HashMap<>();
        for (EventTeamType teamType : EventTeamType.values()) {
            teamScores.put(teamType, this.getScore(teamType));
        }
        return teamScores.entrySet().stream()
                .sorted((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue())) // Sort in descending order
                .map(Map.Entry::getKey)
                .toList();
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
