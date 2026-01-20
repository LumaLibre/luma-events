package dev.jsinco.luma.lumaevents.games.interfaces.tempplatforms;

import org.bukkit.Material;

public record TempPlatformConfig(
        Material platformMaterial,
        // 1 means 3-wide (dx -1..1):
        int radiusX,
        int radiusZ,
        int lifetimeTicks,
        int warnTicks,
        boolean sendBreakAnimation
) {
    public static TempPlatformConfig defaultBedrock3x3(int lifetimeTicks, int warnTicks) {
        return new TempPlatformConfig(Material.BEDROCK, 1, 1, lifetimeTicks, warnTicks, true);
    }
}
