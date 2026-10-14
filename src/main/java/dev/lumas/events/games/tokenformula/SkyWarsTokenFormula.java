package dev.lumas.events.games.tokenformula;

public class SkyWarsTokenFormula extends ScoreRangeTokenFormula {

    private static final int MIN_SCORE = 0;
    private static final int MAX_SCORE = 60;

    public SkyWarsTokenFormula() {
        super(MIN_SCORE, MAX_SCORE);
    }

    @Override
    protected String scoreDescription() {
        return "kills and time survived";
    }
}
