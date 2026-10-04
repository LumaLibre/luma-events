package dev.lumas.events.games.logic.manor2.light;

import dev.lumas.events.utility.latency.tracing.ConeTraceResult;
import org.bukkit.entity.LivingEntity;

public record LightHit(double distance, double centeredness, int lightLevel, LivingEntity target) {
    public static LightHit of(ConeTraceResult traceResult, int illuminance, double maxDegrees) {
        double centeredness = centeredness(traceResult, maxDegrees);
        return new LightHit(
                traceResult.z(),
                centeredness,
                (int) (illuminance * centeredness),
                traceResult.target()
        );
    }

    public static double centeredness(ConeTraceResult coneTraceResult, double maxDegrees) {
        return 1 - coneTraceResult.r() / coneTraceResult.z() / Math.tan(maxDegrees * Math.PI / 180);
    }
}
