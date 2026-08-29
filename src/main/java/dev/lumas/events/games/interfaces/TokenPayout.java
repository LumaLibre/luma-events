package dev.lumas.events.games.interfaces;

import org.jetbrains.annotations.NotNull;

public record TokenPayout(double amount, boolean flat) {

    public static final TokenPayout NORMAL = new TokenPayout(1.0, false);
    public static final TokenPayout NONE = new TokenPayout(0.0, false);

    public static TokenPayout multiplier(double multiplier) {
        return new TokenPayout(multiplier, false);
    }

    public static TokenPayout flat(double tokens) {
        return new TokenPayout(tokens, true);
    }

    public boolean isNormal() {
        return !this.flat && this.amount == 1.0;
    }

    public boolean paysNothing() {
        return this.amount <= 0.0 || round(this.amount) < 1;
    }

    public int flatTokens() {
        return round(this.amount);
    }

    public int apply(int earned) {
        if (this.amount <= 0.0) return 0;
        if (this.flat) return round(this.amount);
        if (earned < 1) return 0;
        if (this.amount == 1.0) return earned;
        return round(earned * this.amount);
    }

    private static int round(double value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.round(value));
    }

    public String percentDifference() {
        double percent = Math.abs(this.amount - 1.0) * 100.0;
        return format(Math.round(percent * 10.0) / 10.0) + "% " + (this.amount >= 1.0 ? "more" : "fewer");
    }

    public static String format(double amount) {
        return amount == Math.rint(amount)
                ? String.valueOf((long) amount)
                : String.valueOf(amount);
    }

    @NotNull
    @Override
    public String toString() {
        return this.flat ? this.flatTokens() + " flat tokens" : format(this.amount) + "x tokens";
    }
}
