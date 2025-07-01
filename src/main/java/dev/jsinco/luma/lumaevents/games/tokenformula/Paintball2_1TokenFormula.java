package dev.jsinco.luma.lumaevents.games.tokenformula;

import dev.jsinco.luma.lumaevents.games.interfaces.TokenFormula;
import dev.jsinco.luma.lumaevents.utility.Couple;

public class Paintball2_1TokenFormula implements TokenFormula<Couple<Integer, Boolean>> {

    @Override
    public int tokens(Couple<Integer, Boolean> context) {
        int position = context.getFirst();
        boolean isWinner = context.getSecond();

        int total = 4;

        if (position <= 2) {
            total += 3;
        } else if (position <= 5) {
            total += 2;
        }

        if (!isWinner) {
            total -= 1;
        }
        return total;
    }
}
