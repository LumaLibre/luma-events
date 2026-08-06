package dev.lumas.events.games.tokenformula;

import dev.lumas.events.games.interfaces.TokenFormula;

public class IncursionTokenFormula extends TokenFormula<Integer> {

    private static final int MIN_TOKENS = 3;
    private static final int MAX_TOKEN = 9;

    private final int pointsPerToken;

    public IncursionTokenFormula(int pointsPerToken) {
        this.pointsPerToken = Math.max(1, pointsPerToken);
    }

    @Override
    protected int tokens(Integer points) {
        return Math.clamp(MIN_TOKENS + (Math.max(0, points) / pointsPerToken), MIN_TOKENS, MAX_TOKEN);
    }

    @Override
    public String description() {
        return MIN_TOKENS + " tokens + 1 token per " + pointsPerToken + " point(s) scored, capped at " + MAX_TOKEN + ".";
    }
}
