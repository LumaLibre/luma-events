package dev.jsinco.luma.lumaevents.games.tokenformula;

import dev.jsinco.luma.lumaevents.games.interfaces.TokenFormula;

public class BoatRace2TokenFormula implements TokenFormula<Integer> {
    @Override
    public int tokens(Integer context) {
        // top 3 places get 3 tokens, next 2 get 2 tokens, and the rest get 1 token.
        if (context <= 3) {
            return 3;
        } else if (context <= 5) {
            return 2;
        }
        return 1;
    }
}
