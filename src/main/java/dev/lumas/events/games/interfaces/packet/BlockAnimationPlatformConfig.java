package dev.lumas.events.games.interfaces.packet;

import lombok.Builder;
import org.bukkit.Material;

@Builder
public record BlockAnimationPlatformConfig(
        Material material,
        // 1 means 3-wide (dx -1..1):
        int radiusX,
        int radiusZ,
        int lifetimeTicks,
        int warnTicks,
        boolean sendBreakAnimation
) {
    public static BlockAnimationPlatformConfig defaultBedrock3x3(int lifetimeTicks, int warnTicks) {
        return BlockAnimationPlatformConfig.builder()
                .material(Material.BEDROCK)
                .radiusX(1)
                .radiusZ(1)
                .lifetimeTicks(lifetimeTicks)
                .warnTicks(warnTicks)
                .sendBreakAnimation(true)
                .build();
    }

    public static BlockAnimationPlatformConfig defaultConfig() {
        return BlockAnimationPlatformConfig.builder()
                .material(Material.BEDROCK)
                .radiusX(1)
                .radiusZ(1)
                .lifetimeTicks(140)
                .warnTicks(40)
                .sendBreakAnimation(true)
                .build();
    }
}
