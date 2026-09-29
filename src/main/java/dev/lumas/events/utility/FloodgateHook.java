package dev.lumas.events.utility;

import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

public final class FloodgateHook {

    private static final boolean FLOODGATE_AVAILABLE = Externals.pluginExists("floodgate");

    public static boolean isBedrockPlayer(UUID uuid) {
        return FLOODGATE_AVAILABLE && FloodgateApi.getInstance().isFloodgatePlayer(uuid);
    }
}
