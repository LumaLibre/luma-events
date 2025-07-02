package dev.jsinco.luma.lumaevents.placeholders;

import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumacore.manager.placeholder.PlaceholderInfo;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.games.constants.MinigameConstant;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@AutoRegister(RegisterType.PLACEHOLDER)
@PlaceholderInfo(
        identifier = "paintball",
        parent = PlaceholderManager.class
)
public class Paintball2_1PositionPlaceholder implements PlaceholderModule {
    @Nullable
    @Override
    public String onRequest(EventMain eventMain, @Nullable OfflinePlayer offlinePlayer, List<String> args) {
        int position = !args.isEmpty() ? Integer.parseInt(args.getFirst()) : 1;
        return infoForMinigamePosition(MinigameConstant.PAINTBALL2_1, position);
    }
}
