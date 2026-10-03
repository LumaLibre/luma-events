package dev.lumas.events.utility.latency.tracing;

import org.bukkit.entity.LivingEntity;

public record BeamTraceResult(LivingEntity hitTarget, String hitboxName) {
}
