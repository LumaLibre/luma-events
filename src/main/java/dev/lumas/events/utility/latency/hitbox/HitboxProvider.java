package dev.lumas.events.utility.latency.hitbox;

import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;

@FunctionalInterface
public interface HitboxProvider {

    Double[] HUMANOID_BODY_SAMPLES = {-0.3, 0d, 0.3};
    double HEAD_DROP_BELOW_EYE = 0.32;
    double MAX_HEAD_FRACTION = 0.5;

    Hitbox toHitbox(LivingEntity livingEntity);


    static Hitbox humanoid(LivingEntity livingEntity) {
        BoundingBox body = livingEntity.getBoundingBox();
        return new Hitbox(List.of(
            new Hitbox.Section(HitboxPart.BODY, body),
            new Hitbox.Section(HitboxPart.HEAD, humanoidHeadHitbox(body, livingEntity.getEyeHeight()))
        ),
            body,
            Arrays.stream(HUMANOID_BODY_SAMPLES)
                .map(aDouble -> new Vector(0, livingEntity.getHeight() * aDouble, 0))
                .toArray(Vector[]::new)
        );
    }

    static Hitbox box(LivingEntity livingEntity, double width, double height) {
        BoundingBox body = new BoundingBox(
            livingEntity.getX() - width / 2,
            livingEntity.getY(),
            livingEntity.getZ() - width / 2,
            livingEntity.getX() + width / 2,
            livingEntity.getY() + height,
            livingEntity.getZ() + width / 2
        );
        return new Hitbox(List.of(new Hitbox.Section(HitboxPart.BODY, body)),
            body,
            new Vector()
        );
    }

    private static BoundingBox humanoidHeadHitbox(BoundingBox body, double eyeHeight) {
        double chin = body.getMinY() + eyeHeight - HEAD_DROP_BELOW_EYE;

        double lowest = body.getMaxY() - (body.getHeight() * MAX_HEAD_FRACTION);

        return body.clone()
            .resize(body.getMinX(), Math.max(chin, lowest), body.getMinZ(),
                body.getMaxX(), body.getMaxY(), body.getMaxZ());
    }
}
