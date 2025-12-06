package dev.jsinco.luma.lumaevents.games.tokenformula;

import dev.jsinco.luma.lumaevents.games.interfaces.TokenFormula;

public class BoatRace2TokenFormula extends TokenFormula<Integer> {

    public BoatRace2TokenFormula(boolean makeDirty) {
        super(makeDirty);
    }

    @Override
    public int tokens(Integer context) {
        int amt = 2;

        if (context <= 3) {
            amt = 4;
        } else if (context <= 5) {
            amt = 3;
        }
        return amt;
    }
}
