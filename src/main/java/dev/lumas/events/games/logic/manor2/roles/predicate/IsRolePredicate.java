package dev.lumas.events.games.logic.manor2.roles.predicate;

import dev.lumas.events.games.logic.manor2.roles.Manor2Role;

import java.util.function.Predicate;

public record IsRolePredicate(Class<? extends Manor2Role> roleClass) implements Predicate<Manor2Role> {
    @Override
    public boolean test(Manor2Role role) {
        return roleClass.isInstance(role);
    }
}
