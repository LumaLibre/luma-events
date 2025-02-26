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
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@AutoRegister(RegisterType.PLACEHOLDER)
@PlaceholderInfo(
        identifier = "nameposition",
        parent = PlaceholderManager.class
)
public class PlayerNamePositionPlaceholder implements PlaceholderModule {
    @Override
    public @Nullable String onRequest(EventMain eventMain, @Nullable OfflinePlayer offlinePlayer, List<String> args) {
        if (args.isEmpty()) {
            return null;
        }

        List<EventPlayer> players = new ArrayList<>(List.copyOf(EventPlayerManager.EVENT_PLAYERS));
        players.sort((p1, p2) -> Integer.compare(p2.getPoints(), p1.getPoints()));
        if (players.size() < Util.getInt(args.getFirst(), 1)) {
            return "Unfilled position";
        }

        EventPlayer player = players.get(Util.getInt(args.getFirst(), 1) - 1);
        if (player == null) {
            return "Unfilled position";
        }
        return Bukkit.getOfflinePlayer(player.getUuid()).getName();
    }
}
