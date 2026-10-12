package dev.lumas.events.games.tokenformula;

import dev.lumas.events.games.interfaces.TokenFormula;

public class Manor2TokenFormula extends TokenFormula<Integer> {
    private static int MIN_ATTRIBUTED_SCORE = 400;
    private static int MAX_ATTRIBUTED_SCORE = 900;
    private static int MIN_TOKEN_PAYOT = 3;
    private static int MAX_TOKEN_PAYOT = 9;

    @Override
    protected int tokens(Integer score) {
        int clampedScore = Math.clamp(score, MIN_ATTRIBUTED_SCORE, MAX_ATTRIBUTED_SCORE) - MIN_ATTRIBUTED_SCORE;
        return MIN_TOKEN_PAYOT + ((MAX_TOKEN_PAYOT - MIN_TOKEN_PAYOT) * (clampedScore)) / (MAX_ATTRIBUTED_SCORE - MIN_ATTRIBUTED_SCORE);
    }

    @Override
    public String description() {
        return "%d tokens up to %d tokens with increasing score"
            .formatted(MIN_TOKEN_PAYOT, MAX_TOKEN_PAYOT);
    }
}
