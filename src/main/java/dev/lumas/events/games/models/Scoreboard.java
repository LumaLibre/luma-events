package dev.lumas.events.games.models;

import dev.lumas.events.games.interfaces.Scorer;
import dev.lumas.events.utility.Util;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Scoreboard<T extends Scorer> {

    private static final String BORDER = "<#eee1d5><st>                     <reset><#eee1d5>⋆⁺₊⋆ ★ ⋆⁺₊⋆<st>                     ";
    private static final String MESSAGE_FORMAT = "<green><gold>#%s</gold> is <red>%s</red> with <gold>%s points</gold>!";

    private final Map<T, Integer> scores = new HashMap<>();

    public void addScore(T scorer, int points) {
        scores.putIfAbsent(scorer, 0);
        scores.put(scorer, scores.get(scorer) + points);
    }

    public void removeScore(T scorer) {
        scores.remove(scorer);
    }

    public void addScorers(List<T> scorers) {
        for (T scorer : scorers) {
            scores.putIfAbsent(scorer, 0);
        }
    }

    public int getScore(T scorer) {
        return scores.getOrDefault(scorer, 0);
    }

    public Map<T, Integer> getScores() {
        return Map.copyOf(scores);
    }

    @Nullable
    public T getTopScorer() {
        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public LinkedHashMap<T, Integer> sortedScores() {
        return scores.entrySet().stream()
                .sorted(Map.Entry.<T, Integer>comparingByValue().reversed())
                .collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), LinkedHashMap::putAll);
    }

    public List<Component> scoreboardMessage() {
        LinkedHashMap<T, Integer> sorted = sortedScores();
        if (sorted.isEmpty()) {
            return List.of(Util.color("<gray>No scores available."));
        }

        List<Component> messages = new ArrayList<>();
        // message for the top 5 scorers
        int count = 0;
        for (Map.Entry<T, Integer> entry : sorted.entrySet()) {
            if (count >= 5) break; // Limit to top 5
            T scorer = entry.getKey();
            int score = entry.getValue();
            messages.add(Util.prefixed(String.format(MESSAGE_FORMAT, count + 1, scorer.getName(), score)));
            count++;
        }

        return messages;
    }

    public void handleGameEnd(Audience audience, Runnable callback) {
        Map<T, Integer> top = this.sortedScores();
        if (top == null) {
            audience.sendMessage(Util.prefixed("No scores available to determine a winner."));
            return;
        }
        T topScorer = top.keySet().stream().findFirst().orElse(null);
        List<Component> messages = this.scoreboardMessage();

        audience.showTitle(Util.title(
                "<yellow>Game Over",
                "<gold>" + (topScorer != null ? topScorer.getName() : "Unknown") + " has won!"
        ));

        audience.sendMessage(Util.color(BORDER));
        for (Component message : messages) {
            audience.sendMessage(message);
        }
        audience.sendMessage(Util.color(BORDER));

        if (callback != null) {
            try {
                callback.run();
            } catch (Throwable e) {
                audience.sendMessage(Util.color("<red>Error while handling game end: " + e.getMessage()));
                e.printStackTrace();
            }
        }
    }
}
