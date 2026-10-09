package dev.lumas.events.utility.latency.tracing;

import dev.lumas.events.utility.latency.hitbox.Hitbox;
import org.bukkit.entity.LivingEntity;

public record Target(LivingEntity entity, Hitbox hitbox) {

}
