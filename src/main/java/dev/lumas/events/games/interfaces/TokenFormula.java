package dev.lumas.events.games.interfaces;

import dev.lumas.events.obj.EventPlayer;
import dev.lumas.events.items.TokenExchanging;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class TokenFormula<C> {

    private final Map<UUID, Integer> dirty = new HashMap<>();
    private final boolean makeDirty;

    public TokenFormula() {
        this(true);
    }

    public TokenFormula(boolean makeDirty) {
        this.makeDirty = makeDirty;
    }

    protected abstract int tokens(C context);

    public String description() {
        return "Did not provide a description for this token formula.";
    }

    public final void giveTokens(EventPlayer player, C context) {
        int amount = tokens(context);
        Player bukkitPlayer = player.getPlayer();
        UUID uuid = player.getUuid();
        if (amount < 1 || bukkitPlayer == null || isDirty(uuid)) {
            return;
        }

        makeDirty(uuid, amount);
        TokenExchanging.give(bukkitPlayer, TokenExchanging.TokenType.CARAMEL_APPLE, amount, "Minigame");
    }


    public final Map<UUID, Integer> earnersSorted() {
        return dirty.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);
    }


    private boolean isDirty(UUID uuid) {
        return makeDirty && dirty.containsKey(uuid);
    }

    private void makeDirty(UUID uuid, int tokens) {
        if (makeDirty) {
            dirty.put(uuid, tokens);
        }
    }
}
