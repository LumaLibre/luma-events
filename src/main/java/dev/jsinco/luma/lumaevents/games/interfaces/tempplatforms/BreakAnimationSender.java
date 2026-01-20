package dev.jsinco.luma.lumaevents.games.interfaces.tempplatforms;

import org.bukkit.entity.Player;

public interface BreakAnimationSender {
    void sendBreakAnimation(Player viewer, int breakerId, int x, int y, int z, int stage);

    default void clearBreakAnimation(Player viewer, int breakerId, int x, int y, int z) {
        sendBreakAnimation(viewer, breakerId, x, y, z, -1);
    }

    // Provide to TempPlatformManager for no animation
    static BreakAnimationSender noop() {
        return (viewer, breakerId, x, y, z, stage) -> {};
    }
}
