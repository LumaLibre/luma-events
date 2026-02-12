package dev.lumas.events.games.tokenformula;

import dev.lumas.events.games.interfaces.TokenFormula;

/**
 * A token formula that divides the double context by a divisor to determine
 * the number of tokens to award. The result is floored to the nearest whole
 * number, with a maximum cap on the number of tokens that can be awarded.
 */
public class DivisibleTokenFormula extends TokenFormula<Double> {

    private final double divisor;
    private final int maxTokens;

    public DivisibleTokenFormula(double divisor, int maxTokens) {
        this.divisor = divisor;
        this.maxTokens = maxTokens;
    }

    @Override
    protected int tokens(Double context) {
        double quotient = context / divisor;
        int value = (int) Math.floor(quotient);

        if (value < 1) {
            return 0;
        }
        return Math.min(value, maxTokens);
    }
}
