package dev.lumas.events.utility.latency.tracing;

import dev.lumas.events.utility.latency.hitbox.TrailSnapshot;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BoundingBox;

import java.util.Map;

public record Target(LivingEntity entity, BoundingBox hitbox, Map<String, BoundingBox> parts) {
    private static final double HEAD_DROP_BELOW_EYE = 0.32;
    private static final double MAX_HEAD_FRACTION = 0.5;

    public static final String HEAD_ID = "head";
    public static final String BODY_ID = "body";

    public static Target ofHumanoid(LivingEntity entity, double expandBy, TrailSnapshot snapshot) {
        return ofHumanoid(
                entity,
                expandBy,
                snapshot.hitbox(),
                snapshot.eyeHeight()
        );
    }

    public static Target ofHumanoid(LivingEntity entity, double expandBy) {
        return ofHumanoid(
                entity,
                expandBy,
                entity.getBoundingBox(),
                entity.getEyeHeight()
        );
    }

    private static Target ofHumanoid(LivingEntity entity, double expandBy, BoundingBox body, double eyeHeight) {
        BoundingBox expanded = body.clone().expand(expandBy);
        return new Target(
                entity,
                expanded,
                Map.of(BODY_ID, expanded,
                        HEAD_ID, headHitbox(body, expandBy, eyeHeight)
                )
        );
    }

    private static BoundingBox headHitbox(BoundingBox body, double expandBy, double eyeHeight) {
        double chin = body.getMinY() + eyeHeight - HEAD_DROP_BELOW_EYE;

        double lowest = body.getMaxY() - (body.getHeight() * MAX_HEAD_FRACTION);

        return body.clone()
                .resize(body.getMinX(), Math.max(chin, lowest), body.getMinZ(),
                        body.getMaxX(), body.getMaxY(), body.getMaxZ())
                .expand(expandBy);
    }

}
