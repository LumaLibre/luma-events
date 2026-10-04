package dev.lumas.events.utility.latency.tracing;

import org.bukkit.entity.LivingEntity;

public record ProximityTraceResult(LivingEntity target, double distance) {
}
