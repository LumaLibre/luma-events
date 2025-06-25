package dev.jsinco.luma.lumaevents.games.interfaces;

import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.tokens.TokenExchanging;
import org.bukkit.entity.Player;

public interface TokenFormula<C> {

    int tokens(C context);


    default void giveTokens(EventPlayer player, C context) {
        int amount = tokens(context);
        Player bukkitPlayer = player.getPlayer();
        if (amount < 1 || bukkitPlayer == null) {
            return;
        }

        TokenExchanging.giveWithChances(bukkitPlayer, amount);
    }
}
