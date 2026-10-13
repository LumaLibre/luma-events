package dev.lumas.events.utility;

import dev.lumas.sleepy.api.SleepyAPI;
import org.bukkit.entity.Player;

import java.util.UUID;

public class SoftSleepyAPI {

    private static final boolean SLEEPY_AVAILABLE = Externals.classExists("dev.lumas.sleepy.api.SleepyAPI");

    public static boolean hasBeenAfkForSeconds(Player player, long seconds) {
        if (!SLEEPY_AVAILABLE) return false;
        return SleepyAPI.hasBeenAfkFor(player, seconds);
    }

    public static boolean hasBeenAfkForSeconds(UUID uuid, long seconds) {
        if (!SLEEPY_AVAILABLE) return false;
        return SleepyAPI.hasBeenAfkFor(uuid, seconds);
    }
}
