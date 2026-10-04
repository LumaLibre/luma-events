package dev.lumas.events.utility.latency.tracing;

import org.bukkit.entity.LivingEntity;

/**
 * Provides cylindrical coordinates for a coordinate system
 * where origin is the player eye height
 * and z axis is in the same direction as player facing
 *
 * @param target the targeted entity
 * @param z      the distance between target and player in z axis
 * @param r      the distance between target and player in radius axis
 */
public record ConeTraceResult(LivingEntity target, double z, double r) {
}
