package dev.jsinco.luma.lumaevents.explorer.custom;

import dev.jsinco.luma.lumaevents.tokens.TokenExchanging;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EarnTokenExplorerEvent {
    private final TokenExchanging.TokenType type;
    private final int amount;
}
