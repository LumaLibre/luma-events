package dev.lumas.events.games.tokenformula;

import dev.lumas.events.games.logic.manor2.Manor2;

import java.util.function.Supplier;

public class Manor2TokenFormula {

    private static final int MIN_SCORE = 100;
    private static final int MAX_SCORE = 700;
    private static final long EXPECTED_PLAYTIME = 1000 * 60 * 5;

    private Manor2TokenFormula() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static ScoreRangeTokenFormula formula(Manor2 manor2) {
        return new ScoreRangeTokenFormula(rangeSupplier(manor2), "score");
    }

    private static Supplier<ScoreRangeTokenFormula.ScoreRange> rangeSupplier(Manor2 manor2) {
        return () -> {
            long timePlayed = Math.max(EXPECTED_PLAYTIME / 2, System.currentTimeMillis() - manor2.getStartTime());
            double progress = (double) timePlayed / EXPECTED_PLAYTIME;
            return new ScoreRangeTokenFormula.ScoreRange(
                (int) (MIN_SCORE * progress),
                (int) (MAX_SCORE * progress)
            );
        };
    }
}
