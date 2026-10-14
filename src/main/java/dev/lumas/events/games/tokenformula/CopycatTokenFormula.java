package dev.lumas.events.games.tokenformula;

public class CopycatTokenFormula extends ScoreRangeTokenFormula {

    private static final int MIN_ROUNDS = 0;
    private static final int MAX_ROUNDS = 8;

    public CopycatTokenFormula() {
        super(MIN_ROUNDS, MAX_ROUNDS);
    }

    @Override
    protected String scoreDescription() {
        return "rounds survived";
    }
}
