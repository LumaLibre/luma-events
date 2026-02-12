package dev.lumas.events.games.tokenformula;

import dev.lumas.events.games.interfaces.TokenFormula;

public class TowersTokenFormula extends TokenFormula<Integer> {
    @Override
    public int tokens(Integer context) {
        return Math.min(context, 15);
    }
}
