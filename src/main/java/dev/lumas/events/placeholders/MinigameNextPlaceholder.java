package dev.lumas.events.placeholders;

import dev.lumas.lumacore.manager.modules.AutoRegister;
import dev.lumas.lumacore.manager.modules.RegisterType;
import dev.lumas.lumacore.manager.placeholder.PlaceholderInfo;
import dev.lumas.events.EventMain;
import dev.lumas.events.configurable.Config;
import dev.lumas.events.configurable.MinigameState;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.TimeUnit;

@AutoRegister(RegisterType.PLACEHOLDER)
@PlaceholderInfo(
        identifier = "mgnext",
        parent = PlaceholderManager.class
)
public class MinigameNextPlaceholder implements PlaceholderModule{
    @Nullable
    @Override
    public String onRequest(EventMain eventMain, @Nullable OfflinePlayer offlinePlayer, List<String> list) {
        Config cfg = EventMain.getOkaeriConfig();

        if (!cfg.isAutomaticMinigames()) {
            return "∞m";
        }

        MinigameState minigameState = EventMain.getMinigameState();

        long timeSinceLast = System.currentTimeMillis() - minigameState.getLastGameLaunchTime();
        long timeCombined = cfg.getAutomaticMinigameCooldown() - timeSinceLast;
        return String.format("%dm", TimeUnit.MILLISECONDS.toMinutes(timeCombined));
    }
}
