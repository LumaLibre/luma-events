package dev.lumas.events.games.interfaces;

import dev.lumas.events.games.MinigameManager;
import dev.lumas.events.items.TokenExchanging;
import dev.lumas.events.items.TokenSource;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Executors;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class TokenFormula<C> {

    private final Map<UUID, Integer> dirty = new ConcurrentHashMap<>();
    private final boolean makeDirty;
    private volatile String minigameName;

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

    public final int giveTokens(EventPlayer player, C context) {
        int amount = tokens(context);
        Player bukkitPlayer = player.getPlayer();
        UUID uuid = player.getUuid();
        if (amount < 1 || bukkitPlayer == null) {
            return 0;
        }

        if (makeDirty && dirty.putIfAbsent(uuid, amount) != null) return 0;
        if (this.minigameName == null) {
            this.minigameName = MinigameManager.getInstance().getCurrent().getName();
        }
        TokenSource source = TokenSource.minigame(this.minigameName);
        Executors.runSync(bukkitPlayer, () -> {
            TokenExchanging.give(bukkitPlayer, TokenExchanging.TokenType.SUMMER_DOLLOP, amount, source);
        });
        return amount;
    }


    public final Map<UUID, Integer> earnersSorted() {
        return dirty.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);
    }


    private boolean isDirty(UUID uuid) {
        return makeDirty && dirty.containsKey(uuid);
    }
}
