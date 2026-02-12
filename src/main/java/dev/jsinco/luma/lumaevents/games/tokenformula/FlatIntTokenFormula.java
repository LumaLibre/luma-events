package dev.jsinco.luma.lumaevents.games.tokenformula;

import dev.jsinco.luma.lumaevents.games.interfaces.TokenFormula;

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
}
