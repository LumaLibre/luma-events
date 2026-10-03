package dev.lumas.events.utility.latency.hitbox;

import org.bukkit.util.BoundingBox;

public record TrailSnapshot(long takenAt, BoundingBox hitbox, double eyeHeight) {
}
