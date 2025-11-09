package dev.jsinco.luma.lumaevents.games.tokenformula;

import dev.jsinco.luma.lumaevents.games.interfaces.TokenFormula;
import dev.jsinco.luma.lumaevents.utility.Couple;

public class Paintball2_1TokenFormula implements TokenFormula<Couple<Integer, Boolean>> {

    private static final int PRIORITY_POSITIONS = 5;

    @Override
    public int tokens(Couple<Integer, Boolean> context) {
        int position = context.getFirst();
        boolean isWinner = context.getSecond();

        int total = isWinner ? 9 : 8;
        if (position <= PRIORITY_POSITIONS) {
            total += Math.max(PRIORITY_POSITIONS - position, 2);
        }

        return Math.min(total, 15);
    }
}
