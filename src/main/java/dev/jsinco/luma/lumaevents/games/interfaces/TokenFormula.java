package dev.jsinco.luma.lumaevents.games.interfaces;

import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.items.TokenExchanging;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public abstract class TokenFormula<C> {

    private final Set<UUID> dirtyPlayers = new HashSet<>();

    protected abstract int tokens(C context);

    public void giveTokens(EventPlayer player, C context) {
        int amount = tokens(context);
        Player bukkitPlayer = player.getPlayer();
        if (amount < 1 || bukkitPlayer == null || dirtyPlayers.contains(player.getUuid())) {
            return;
        }

        dirtyPlayers.add(player.getUuid());
        TokenExchanging.give(bukkitPlayer, TokenExchanging.TokenType.POWDER, amount);
    }
}
