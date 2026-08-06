package dev.lumas.events.games.tokenformula;

import dev.lumas.events.games.interfaces.TokenFormula;
import dev.lumas.events.utility.Couple;

public class BombermanTokenFormula extends TokenFormula<Couple<Integer, Boolean>> {

    private static final int MIN_TOKENS = 3;
    private static final int MAX_TOKENS = 9;
    private static final int WIN_BONUS = 2;

    private final int scoreForMaximum;

    public BombermanTokenFormula(int scoreForMaximum) {
        this.scoreForMaximum = Math.max(1, scoreForMaximum);
    }

    @Override
    protected int tokens(Couple<Integer, Boolean> result) {
        double progress = Math.clamp(result.a() / (double) scoreForMaximum, 0.0, 1.0);
        int earned = MIN_TOKENS + (int) Math.round((MAX_TOKENS - MIN_TOKENS) * progress);

        if (result.b()) {
            earned += WIN_BONUS;
        }

        return Math.min(earned, MAX_TOKENS);
    }

    @Override
    public String description() {
        return MIN_TOKENS + "-" + MAX_TOKENS + " tokens for blocks destroyed and kills, +" + WIN_BONUS + " for winning.";
    }
}
