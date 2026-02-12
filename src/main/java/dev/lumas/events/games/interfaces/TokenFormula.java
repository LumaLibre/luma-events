package dev.lumas.events.games.interfaces;

import dev.lumas.events.obj.EventPlayer;
import dev.lumas.events.items.TokenExchanging;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public abstract class TokenFormula<C> {

    private final Set<UUID> dirty = new HashSet<>();
    private final boolean makeDirty;

    public TokenFormula() {
        this(true);
    }

    public TokenFormula(boolean makeDirty) {
        this.makeDirty = makeDirty;
    }

    protected abstract int tokens(C context);

    public void giveTokens(EventPlayer player, C context) {
        int amount = tokens(context);
        Player bukkitPlayer = player.getPlayer();
        UUID uuid = player.getUuid();
        if (amount < 1 || bukkitPlayer == null || isDirty(uuid)) {
            return;
        }

        makeDirty(uuid);
        TokenExchanging.give(bukkitPlayer, TokenExchanging.TokenType.CANDIED_APPLE, amount);
    }

    private boolean isDirty(UUID uuid) {
        return makeDirty && dirty.contains(uuid);
    }

    private void makeDirty(UUID uuid) {
        if (makeDirty) {
            dirty.add(uuid);
        }
    }
}
