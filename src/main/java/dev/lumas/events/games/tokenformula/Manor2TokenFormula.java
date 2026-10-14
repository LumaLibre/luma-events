package dev.lumas.events.games.tokenformula;

public class Manor2TokenFormula extends ScoreRangeTokenFormula {

    private static final int MIN_SCORE = 400;
    private static final int MAX_SCORE = 900;

    public Manor2TokenFormula() {
        super(MIN_SCORE, MAX_SCORE);
    }

    @Override
    protected String scoreDescription() {
        return "score";
    }
}
