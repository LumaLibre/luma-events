package dev.lumas.events.games.tokenformula;

public class CopycatTokenFormula {

    private static final int MIN_ROUNDS = 0;
    private static final int MAX_ROUNDS = 8;

    private CopycatTokenFormula() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static ScoreRangeTokenFormula formula() {
        return new ScoreRangeTokenFormula(MIN_ROUNDS, MAX_ROUNDS, "rounds survived");
    }
}
