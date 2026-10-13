package dev.lumas.events.games.interfaces.models;

import dev.lumas.events.model.EventPlayer;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public record InactivitySettings(
    List<Class<? extends MinigameRole>> afkRemovableRoles,
    Function<EventPlayer, @Nullable MinigameRole> roleLookup,
    long secondsAfk, // warn the player of their afk status
    long secondsBeforeKick // kick the player from the game
) {

    public static InactivitySettings defaults() {
        return new InactivitySettings(List.of(), player -> null, 30, 45);
    }

    @SafeVarargs
    public static InactivitySettings defaults(Function<EventPlayer, @Nullable MinigameRole> roleLookup, Class<? extends MinigameRole>... roles) {
        return new InactivitySettings(List.of(roles), roleLookup, 30, 45);
    }
}
