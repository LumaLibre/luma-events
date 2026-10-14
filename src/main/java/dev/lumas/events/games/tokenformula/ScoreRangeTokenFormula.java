package dev.lumas.events.games.tokenformula;

import dev.lumas.events.games.interfaces.TokenFormula;
import dev.lumas.events.utility.Couple;

public abstract class ScoreRangeTokenFormula extends TokenFormula<Couple<Integer, Boolean>> {

    private static final int MIN_TOKENS = 2;
    private static final int MAX_TOKENS = 10;
    private static final int WIN_BONUS = 2;

    private final int minScore;
    private final int maxScore;

    protected ScoreRangeTokenFormula(int minScore, int maxScore) {
        this.minScore = minScore;
        this.maxScore = Math.max(minScore + 1, maxScore);
    }

    protected abstract String scoreDescription();

    @Override
    protected int tokens(Couple<Integer, Boolean> result) {
        double progress = Math.clamp((result.a() - minScore) / (double) (maxScore - minScore), 0.0, 1.0);
        int earned = MIN_TOKENS + (int) Math.round((MAX_TOKENS - MIN_TOKENS) * progress);
        return result.b() ? earned + WIN_BONUS : earned;
    }

    @Override
    public String description() {
        return MIN_TOKENS + "-" + MAX_TOKENS + " tokens based on " + scoreDescription() + ", +" + WIN_BONUS + " for winning.";
    }
}
