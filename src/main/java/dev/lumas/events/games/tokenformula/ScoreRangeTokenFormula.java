package dev.lumas.events.games.tokenformula;

import dev.lumas.events.games.interfaces.TokenFormula;
import dev.lumas.events.utility.Couple;

import java.util.function.Supplier;

public class ScoreRangeTokenFormula extends TokenFormula<Couple<Integer, Boolean>> {

    private static final int MIN_TOKENS = 2;
    private static final int MAX_TOKENS = 10;
    private static final int WIN_BONUS = 2;

    private final Supplier<ScoreRange> scoreRanges;
    private final String scoreDescription;

    public ScoreRangeTokenFormula(int minScore, int maxScore, String scoreDescription) {
        this(() -> new ScoreRange(minScore, maxScore), scoreDescription);
    }

    public ScoreRangeTokenFormula(Supplier<ScoreRange> scoreRanges, String scoreDescription) {
        this.scoreRanges = scoreRanges;
        this.scoreDescription = scoreDescription;
    }

    @Override
    protected int tokens(Couple<Integer, Boolean> result) {
        ScoreRange range = scoreRanges.get();
        int minScore = range.minScore();
        int maxScore = Math.max(minScore + 1, range.maxScore());
        double progress = Math.clamp((result.a() - minScore) / (double) (maxScore - minScore), 0.0, 1.0);
        int earned = MIN_TOKENS + (int) Math.round((MAX_TOKENS - MIN_TOKENS) * progress);
        return result.b() ? earned + WIN_BONUS : earned;
    }

    @Override
    public String description() {
        return MIN_TOKENS + "-" + MAX_TOKENS + " tokens based on " + scoreDescription + ", +" + WIN_BONUS + " for winning.";
    }

    public record ScoreRange(int minScore, int maxScore) {
    }
}
