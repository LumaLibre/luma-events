package dev.lumas.events.games.logic.manor2.score;

import java.util.Map;
import java.util.UUID;

public record ScoreRecord(Map<UUID, Double> attributionPercentage, int sharedScore,
                          ScoreRecordDisplay scoreRecordDisplay) {
}
