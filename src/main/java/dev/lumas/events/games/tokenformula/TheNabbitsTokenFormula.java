package dev.lumas.events.games.tokenformula;

import dev.lumas.events.games.interfaces.TokenFormula;

public class TheNabbitsTokenFormula extends TokenFormula<Integer> {
    @Override
    protected int tokens(Integer context) {
        return Math.min(context, 10);
    }
}
