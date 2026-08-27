package dev.lumas.events.games.tokenformula;

import dev.lumas.events.games.interfaces.TokenFormula;

public class PropHuntTokenFormula extends TokenFormula<Integer> {
    @Override
    public int tokens(Integer context) {
        return Math.clamp(context, 3, 9);
    }
}
