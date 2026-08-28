package dev.lumas.events.games.tokenformula;

import dev.lumas.events.games.interfaces.TokenFormula;
import dev.lumas.events.utility.Couple;

public class SoccerTokenFormula extends TokenFormula<Couple<Integer, Boolean>> {

    private static final int MINIMUM_TOKENS = 3;
    private static final int MAXIMUM_TOKENS = 15;
    private static final int PLAYS_PER_TOKEN = 2;
    private static final int WINNER_BONUS = 2;

    @Override
    protected int tokens(Couple<Integer, Boolean> context) {
        int plays = Math.max(0, context.getFirst());
        int total = MINIMUM_TOKENS + (plays / PLAYS_PER_TOKEN) + (Boolean.TRUE.equals(context.getSecond()) ? WINNER_BONUS : 0);
        return Math.clamp(total, MINIMUM_TOKENS, MAXIMUM_TOKENS);
    }

    @Override
    public String description() {
        return MINIMUM_TOKENS + " tokens, +1 per " + PLAYS_PER_TOKEN + " plays made, +" + WINNER_BONUS
            + " for winning, capped at " + MAXIMUM_TOKENS + ".";
    }
}
