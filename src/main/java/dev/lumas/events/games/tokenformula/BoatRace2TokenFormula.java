package dev.lumas.events.games.tokenformula;

import dev.lumas.events.games.interfaces.TokenFormula;

public class BoatRace2TokenFormula extends TokenFormula<Integer> {

    public BoatRace2TokenFormula(boolean makeDirty) {
        super(makeDirty);
    }

    @Override
    public int tokens(Integer context) {
        return context <= 5 ? 3 : 2;
    }
}
