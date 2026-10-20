package dev.lumas.events.games.interfaces.models;

import dev.lumas.events.model.EventPlayer;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public record InactivitySettings(
    List<Class<? extends MinigameRole>> afkRemovableRoles,
    Function<EventPlayer, @Nullable MinigameRole> roleLookup,
    Timeout roleTimeout, // participants whose role is in afkRemovableRoles
    Timeout fallbackTimeout, // every other participant (spectators, games without roles, etc.)
    boolean ignoreFloodgate
) {

    public static final Timeout DEFAULT_ROLE_TIMEOUT = new Timeout(60, 90);
    public static final Timeout DEFAULT_FALLBACK_TIMEOUT = new Timeout(360, 420);

    public record Timeout(long secondsBeforeWarning, long secondsBeforeKick) {
        public long warningWindowSeconds() {
            return Math.max(1, secondsBeforeKick - secondsBeforeWarning);
        }
    }

    public Timeout timeoutFor(EventPlayer participant) {
        if (afkRemovableRoles.isEmpty()) {
            return fallbackTimeout;
        }
        MinigameRole role = roleLookup.apply(participant);
        if (role != null && afkRemovableRoles.stream().anyMatch(type -> type.isInstance(role))) {
            return roleTimeout;
        }
        return fallbackTimeout;
    }

    public static InactivitySettings defaults() {
        return new InactivitySettings(List.of(), player -> null, DEFAULT_ROLE_TIMEOUT, DEFAULT_FALLBACK_TIMEOUT, true);
    }

    @SafeVarargs
    public static InactivitySettings defaults(Function<EventPlayer, @Nullable MinigameRole> roleLookup, Class<? extends MinigameRole>... roles) {
        return new InactivitySettings(List.of(roles), roleLookup, DEFAULT_ROLE_TIMEOUT, DEFAULT_FALLBACK_TIMEOUT, true);
    }

    @SafeVarargs
    public static InactivitySettings withRoleTimeout(Timeout roleTimeout, Function<EventPlayer, @Nullable MinigameRole> roleLookup, Class<? extends MinigameRole>... roles) {
        return new InactivitySettings(List.of(roles), roleLookup, roleTimeout, DEFAULT_FALLBACK_TIMEOUT, true);
    }
}
