package dev.jsinco.luma.lumaevents.games.tokenformula;

import dev.jsinco.luma.lumaevents.games.interfaces.TokenFormula;

public class PropHuntTokenFormula extends TokenFormula<Integer> {
    @Override
    public int tokens(Integer context) {
        return Math.max(2, Math.min(context, 15));
    }
}
