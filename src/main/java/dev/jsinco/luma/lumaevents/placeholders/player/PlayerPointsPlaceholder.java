package dev.jsinco.luma.lumaevents.placeholders.player;

import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumacore.manager.placeholder.PlaceholderInfo;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.placeholders.PlaceholderManager;
import dev.jsinco.luma.lumaevents.placeholders.PlaceholderModule;
import dev.jsinco.luma.lumaevents.utility.Util;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@AutoRegister(RegisterType.PLACEHOLDER)
@PlaceholderInfo(
        identifier = "points",
        parent = PlaceholderManager.class
)
public class PlayerPointsPlaceholder implements PlaceholderModule {
    @Override
    public @Nullable String onRequest(EventMain eventMain, @Nullable OfflinePlayer offlinePlayer, List<String> list) {
        if (offlinePlayer == null) {
            return null;
        }
        EventPlayer eplayer = EventPlayerManager.getByUUID(offlinePlayer.getUniqueId());
        return Util.formatInt(eplayer.getPoints());
    }
}
