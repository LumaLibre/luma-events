package dev.jsinco.luma.lumaevents.games.tokenformula;

import dev.jsinco.luma.lumaevents.games.interfaces.TokenFormula;

public class TowersTokenFormula implements TokenFormula<Integer> {
    @Override
    public int tokens(Integer context) {
        return Math.min(context, 15); // Max 15 tokens
    }
}
