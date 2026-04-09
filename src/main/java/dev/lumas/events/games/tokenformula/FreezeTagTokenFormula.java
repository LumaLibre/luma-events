package dev.lumas.events.games.tokenformula;

import dev.lumas.events.games.interfaces.TokenFormula;

public class FreezeTagTokenFormula extends TokenFormula<Integer> {

    private final int minimumTokens;
    private final int tokensPerPoint;

    public FreezeTagTokenFormula(int minimumTokens, int tokensPerPoint) {
        this.minimumTokens = minimumTokens;
        this.tokensPerPoint = tokensPerPoint;
    }

    @Override
    public int tokens(Integer points) {
        return minimumTokens + (points * tokensPerPoint);
    }
}
