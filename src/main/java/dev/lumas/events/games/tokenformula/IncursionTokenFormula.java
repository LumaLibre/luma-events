package dev.lumas.events.games.tokenformula;

import dev.lumas.events.games.interfaces.TokenFormula;

public class IncursionTokenFormula extends TokenFormula<Integer> {

    private final int minimumTokens;
    private final int pointsPerToken;

    public IncursionTokenFormula(int minimumTokens, int pointsPerToken) {
        this.minimumTokens = minimumTokens;
        this.pointsPerToken = Math.max(1, pointsPerToken);
    }

    @Override
    protected int tokens(Integer points) {
        return minimumTokens + (points / pointsPerToken);
    }

    @Override
    public String description() {
        return minimumTokens + " tokens + 1 token per " + pointsPerToken + " point(s) scored.";
    }
}
