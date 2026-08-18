package dev.lumas.events.games.tokenformula;

import dev.lumas.events.games.interfaces.TokenFormula;

public class PanelPartyTokenFormula extends TokenFormula<PanelPartyTokenFormula.Context> {

    public record Context(int panelsSurvived, int purplePanelsSurvived, int panelsPerToken) {
    }

    private final int maxTokens;

    public PanelPartyTokenFormula(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    @Override
    protected int tokens(Context context) {
        int tokens = context.panelsSurvived() / context.panelsPerToken();
        tokens += context.purplePanelsSurvived();
        return Math.min(tokens, maxTokens);
    }

    @Override
    public String description() {
        return "A token formula that awards 1 token per 2-3 panels survived, plus 1 token per purple panel survived, up to a maximum of " + maxTokens + " tokens.";
    }
}
