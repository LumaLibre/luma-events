package dev.lumas.events.placeholders.games;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.PlaceholderMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.games.constants.MinigameConstant;
import dev.lumas.events.placeholders.PlaceholderManager;
import dev.lumas.events.placeholders.PlaceholderModule;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Register(Autowire.PLACEHOLDER)
@PlaceholderMeta(
        identifier = "panelparty",
        parent = PlaceholderManager.class
)
public class PanelPartyPositionPlaceholder implements PlaceholderModule {
    @Nullable
    @Override
    public String onRequest(EventMain eventMain, @Nullable OfflinePlayer offlinePlayer, List<String> args) {
        int position = !args.isEmpty() ? Integer.parseInt(args.getFirst()) : 1;
        return infoForMinigamePosition(MinigameConstant.PANEL_PARTY, position);
    }
}
