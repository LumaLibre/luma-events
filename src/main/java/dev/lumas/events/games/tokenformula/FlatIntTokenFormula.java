package dev.lumas.events.games.tokenformula;

import dev.lumas.events.games.interfaces.TokenFormula;

/**
 * A token formula that always returns the int context as the token amount,
 * effectively treating every point earned as a single token.
 */
public class FlatIntTokenFormula extends TokenFormula<Integer> {

    private final int maxTokens;

    public FlatIntTokenFormula(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    @Override
    protected int tokens(Integer context) {
        return Math.min(context, maxTokens);
    }

    @Override
    public String description() {
        return "A token formula that awards 1 token per point earned, up to a maximum of " + maxTokens + " tokens.";
    }
}
