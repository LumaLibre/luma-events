package dev.jsinco.luma.lumaevents.games.tokenformula;

import dev.jsinco.luma.lumaevents.games.interfaces.TokenFormula;

public class ManorTokenFormula implements TokenFormula<Integer> {
    @Override
    public int tokens(Integer context) {
        return Math.min(context + 2, 15);
    }
}
