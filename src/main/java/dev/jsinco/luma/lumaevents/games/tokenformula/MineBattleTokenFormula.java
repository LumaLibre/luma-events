package dev.jsinco.luma.lumaevents.games.tokenformula;

import dev.jsinco.luma.lumaevents.games.interfaces.TokenFormula;

public class MineBattleTokenFormula extends TokenFormula<Integer> {
    @Override
    protected int tokens(Integer context) {
        // Kill = 100 points, Survive full match = 150 points (dynamic)
        return 1; // TODO: How many tokens should we give here?
    }
}
