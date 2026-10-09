package dev.lumas.events.games.logic.manor2.roles;

import dev.lumas.events.games.interfaces.models.MinigameRoleMap;
import dev.lumas.events.utility.latency.hitbox.HitboxProvider;

public final class RoleHitbox {

    private static final double ARTIFACT_HEIGHT = 0.5;
    private static final double ARTIFACT_WIDTH = 0.5;

    private RoleHitbox() {
        throw new UnsupportedOperationException("Utility class");
    }


    public static HitboxProvider hitboxProvider(MinigameRoleMap<Manor2Role> roles) {
        return livingEntity -> {
            Manor2Role role = roles.get(livingEntity.getUniqueId());
            return switch (role) {
                case Artifact ignored -> HitboxProvider.box(livingEntity, ARTIFACT_WIDTH, ARTIFACT_HEIGHT);
                case Spectator ignored -> HitboxProvider.box(livingEntity, 0d, 0d);
                case null, default -> HitboxProvider.humanoid(livingEntity);
            };
        };
    }
}
