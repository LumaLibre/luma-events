package dev.lumas.events.games.tokenformula;

import dev.lumas.events.games.interfaces.TokenFormula;
import dev.lumas.events.utility.Couple;

public class Paintball2_1TokenFormula extends TokenFormula<Couple<Integer, Boolean>> {

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
