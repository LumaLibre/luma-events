package dev.lumas.events.games.tokenformula;

import dev.lumas.events.games.interfaces.TokenFormula;

public class TNTTagTokenFormula extends TokenFormula<Integer> {
    @Override
    public int tokens(Integer context) {
        return context;
    }
}
