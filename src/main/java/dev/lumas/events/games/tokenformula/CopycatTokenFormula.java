package dev.lumas.events.games.tokenformula;

import dev.lumas.events.games.interfaces.TokenFormula;
import dev.lumas.events.utility.Couple;

public class CopycatTokenFormula extends TokenFormula<Couple<Integer, Boolean>> {

    private static final int BASE_TOKENS = 2;
    private static final int MAX_TOKENS = 12;
    private static final int WIN_BONUS = 3;

    @Override
    protected int tokens(Couple<Integer, Boolean> result) {
        int earned = Math.min(BASE_TOKENS + result.a(), MAX_TOKENS);
        if (result.b()) {
            earned += WIN_BONUS;
        }
        return earned;
    }

    @Override
    public String description() {
        return BASE_TOKENS + " tokens plus 1 per round survived (up to " + MAX_TOKENS + "), +" + WIN_BONUS + " for the top score.";
    }
}
