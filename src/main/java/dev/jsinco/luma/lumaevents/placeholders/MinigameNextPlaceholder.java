package dev.jsinco.luma.lumaevents.placeholders;

import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumacore.manager.placeholder.PlaceholderInfo;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.configurable.Config;
import dev.jsinco.luma.lumaevents.configurable.MinigameState;
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
