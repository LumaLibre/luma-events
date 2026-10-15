package dev.lumas.events.games.tokenformula;

public class SkyWarsTokenFormula {

    private static final int MIN_SCORE = 0;
    private static final int MAX_SCORE = 60;

    private SkyWarsTokenFormula() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static ScoreRangeTokenFormula formula() {
        return new ScoreRangeTokenFormula(MIN_SCORE, MAX_SCORE, "kills and time survived");
    }
}
