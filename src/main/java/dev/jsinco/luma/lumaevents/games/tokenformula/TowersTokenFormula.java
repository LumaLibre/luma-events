package dev.jsinco.luma.lumaevents.games.tokenformula;

import dev.jsinco.luma.lumaevents.games.interfaces.TokenFormula;

public class TowersTokenFormula extends TokenFormula<Integer> {
    @Override
    public int tokens(Integer context) {
        return Math.min(context, 20); // Max 20 tokens
    }
}
