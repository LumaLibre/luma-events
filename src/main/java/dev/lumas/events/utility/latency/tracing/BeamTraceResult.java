package dev.lumas.events.utility.latency.tracing;

import dev.lumas.events.utility.latency.hitbox.HitboxPart;
import org.bukkit.entity.LivingEntity;

public record BeamTraceResult(LivingEntity hitTarget, HitboxPart hitboxName) {
}
