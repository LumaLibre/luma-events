package dev.lumas.events.games.logic.copycat;

import dev.lumas.events.configurable.sectors.game.CopycatDefinition;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

public record BuildSettings(boolean offGround, @Nullable Integer minTime, @Nullable Integer maxTime, Set<Material> ignoredMaterials) {

    public static final BuildSettings DEFAULT = new BuildSettings(false, null, null, Set.of());

    public static BuildSettings from(CopycatDefinition.BuildSettings config) {
        Set<Material> ignored = config.getIgnoredMaterials().isEmpty()
            ? Set.of()
            : Set.copyOf(EnumSet.copyOf(config.getIgnoredMaterials()));
        return new BuildSettings(config.isOffGround(), config.getMinTime(), config.getMaxTime(), ignored);
    }

    public int clampSeconds(int seconds) {
        int clamped = seconds;
        if (minTime != null) clamped = Math.max(clamped, minTime);
        if (maxTime != null) clamped = Math.min(clamped, maxTime);
        return clamped;
    }

    public boolean removable(Material material) {
        return !ignoredMaterials.contains(material);
    }
}
