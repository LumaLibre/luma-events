package dev.jsinco.luma.lumaevents.games.tokenformula;

import dev.jsinco.luma.lumaevents.games.interfaces.TokenFormula;

public class TNTTagTokenFormula extends TokenFormula<Integer> {
    @Override
    public int tokens(Integer context) {
        return context;
    }
}
