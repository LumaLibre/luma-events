package dev.lumas.events.games.logic.manor2.score;

import dev.lumas.events.games.logic.manor2.Manor2;
import dev.lumas.events.utility.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreManager {
    private final Map<UUID, Double> scores = new ConcurrentHashMap<>();

    private final Manor2 manor2;

    public ScoreManager(Manor2 manor2) {
        this.manor2 = manor2;
    }

    public void registerScore(ScoreRecord scoreRecord) {
        Optional<Component> message = scoreRecord.scoreRecordDisplay()
                .display(scoreRecord.attributionPercentage(), manor2.getDefinition().getMessages());
        for (Map.Entry<UUID, Double> entry : scoreRecord.attributionPercentage().entrySet()) {
            double scoreDelta = entry.getValue() / 100D * scoreRecord.sharedScore();
            scores.merge(entry.getKey(), scoreDelta, Double::sum);
        }
        if (message.isEmpty()) {
            return;
        }
        manor2.getAudience().sendMessage(message.get());
    }

    public void showScores() {
        String scorerLine = manor2.getDefinition().getMessages().getScoreRecord();
        List<Map.Entry<UUID, Double>> topScores = scores.entrySet().stream()
                .sorted(Comparator.comparingDouble(Map.Entry::getValue))
                .limit(5)
                .toList();
        int position = 1;
        manor2.getAudience().sendMessage(Util.color(manor2.getDefinition().getMessages().getScoreBoardBorder()));
        for (Map.Entry<UUID, Double> topScore : topScores) {
            manor2.getAudience().sendMessage(
                    MiniMessage.miniMessage().deserialize(scorerLine,
                            Formatter.number("position", position++),
                            Placeholder.component("scorer", Util.displayName(topScore.getKey())),
                            Formatter.number("points", topScore.getValue())
                    )
            );
        }
        manor2.getAudience().sendMessage(Util.color(manor2.getDefinition().getMessages().getScoreBoardBorder()));
    }
}
