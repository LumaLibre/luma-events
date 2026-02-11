package dev.jsinco.luma.lumaevents.games.tokenformula;

import dev.jsinco.luma.lumaevents.games.interfaces.TokenFormula;

public class TNTRunTokenFormula extends TokenFormula<Integer> {
    @Override
    protected int tokens(Integer context) {
        // 1 point for each tick alive
        return 1; // TODO: How many tokens should we give here?
    }
}
