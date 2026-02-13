package dev.lumas.events.placeholders.games;

import dev.lumas.events.EventMain;
import dev.lumas.events.games.constants.MinigameConstant;
import dev.lumas.events.placeholders.PlaceholderManager;
import dev.lumas.events.placeholders.PlaceholderModule;
import dev.lumas.lumacore.manager.modules.AutoRegister;
import dev.lumas.lumacore.manager.modules.RegisterType;
import dev.lumas.lumacore.manager.placeholder.PlaceholderInfo;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@AutoRegister(RegisterType.PLACEHOLDER)
@PlaceholderInfo(
        identifier = "minebattle",
        parent = PlaceholderManager.class
)
public class MineBattlePositionPlaceholder implements PlaceholderModule {
    @Nullable
    @Override
    public String onRequest(EventMain eventMain, @Nullable OfflinePlayer offlinePlayer, List<String> args) {
        int position = !args.isEmpty() ? Integer.parseInt(args.getFirst()) : 1;
        return infoForMinigamePosition(MinigameConstant.MINEBATTLE, position);
    }
}
