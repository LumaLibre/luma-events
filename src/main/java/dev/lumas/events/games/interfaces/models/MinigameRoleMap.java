package dev.lumas.events.games.interfaces.models;

import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Executors;
import dev.lumas.events.utility.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class MinigameRoleMap<T extends MinigameRole> extends HashMap<UUID, T> implements Iterable<T> {

    private final Consumer<T> cleanupConsumer;

    public MinigameRoleMap(Consumer<T> cleanupConsumer) {
        this.cleanupConsumer = cleanupConsumer;
    }

    public MinigameRoleMap() {
        this.cleanupConsumer = role -> {
            // Default no-op cleanup
        };
    }

    public T put(T value) {
        return super.put(value.getUuid(), value);
    }

    public <S extends MinigameRole> List<S> getMatching(Class<S> type) {
        return this.values().stream()
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }

//    public List<T> getMatching(Class<T>... types) {
//        return this.values().stream()
//                .filter(player -> Util.isAssignableFromAny(player.getClass(), types))
//                .toList();
//    }

    @Nullable
    public <K extends T> K as(UUID uuid, Class<K> type) {
        T player = this.get(uuid);
        if (type.isInstance(player)) {
            return type.cast(player);
        }
        return null;
    }

    public <K extends T> List<K> predicate(Class<K> type, Predicate<K> predicate) {
        return this.values().stream()
                .filter(type::isInstance)
                .map(type::cast)
                .filter(predicate)
                .toList();
    }

    public <S extends MinigameRole> T swapRole(S rolePlayer, Supplier<T> newRoleSupplier) {
        return this.swapRole(rolePlayer.getEventPlayer(), newRoleSupplier);
    }

    public T swapRole(EventPlayer eventPlayer, Supplier<T> newRoleSupplier) {
        T currentRole = this.get(eventPlayer.getUuid());
        if (currentRole != null) {
            Executors.runSync(eventPlayer, () -> {
                cleanupConsumer.accept(currentRole);
            });
        }
        T newRole = newRoleSupplier.get();
        this.put(newRole.getEventPlayer().getUuid(), newRole);
        return newRole;
    }

    /**
     * Ensures that there are at least {@code count} players of the specified {@code type}.
     * If there are not enough players of that type, it will promote players from the provided {@code fromTypes}.
     *
     * @param type The type of player to ensure.
     * @param newRoleConsumer A consumer that creates a new instance of the specified type.
     * @param count The minimum number of players of the specified type.
     * @param fromTypes The types of players to promote from.
     * @return true if the requirement was met or successfully promoted, false otherwise.
     */
    @SafeVarargs
    private <S extends MinigameRole> boolean ensureAtLeast(Class<T> type, NewRoleConsumer<T> newRoleConsumer, int count, Class<S>... fromTypes) {
        long currentCount = this.values().stream()
                .filter(type::isInstance)
                .count();
        if (currentCount >= count) {
            return true;
        }
        int difference = count - (int) currentCount;
        List<T> candidates = this.values().stream()
                .filter(player -> Util.isAssignableFromAny(player.getClass(), fromTypes))
                .toList();

        if (candidates.size() < difference) {
            return false; // Not enough candidates to promote
        }

        for (int i = 0; i < difference; i++) {
            T candidate = Util.getRandom(candidates);
            this.swapRole(candidate, () -> newRoleConsumer.accept(candidate.getEventPlayer()));
        }
        return true;
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return this.values().iterator();
    }

    @FunctionalInterface
    private interface NewRoleConsumer<K extends MinigameRole> {
        K accept(EventPlayer eventPlayer);
    }
}