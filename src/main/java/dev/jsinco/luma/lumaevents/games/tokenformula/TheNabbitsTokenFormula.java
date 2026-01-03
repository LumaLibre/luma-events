package dev.jsinco.luma.lumaevents.games.tokenformula;

import dev.jsinco.luma.lumaevents.games.interfaces.TokenFormula;

public class TheNabbitsTokenFormula extends TokenFormula<Integer> {
    @Override
    protected int tokens(Integer context) {
        return Math.min(context, 10);
    }
}
