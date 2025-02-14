package dev.jsinco.luma.lumaevents.placeholders.team;

import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumacore.manager.placeholder.PlaceholderInfo;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.obj.EventTeam;
import dev.jsinco.luma.lumaevents.placeholders.PlaceholderManager;
import dev.jsinco.luma.lumaevents.placeholders.PlaceholderModule;
import dev.jsinco.luma.lumaevents.utility.Util;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@AutoRegister(RegisterType.PLACEHOLDER)
@PlaceholderInfo(
        identifier = "teampoints",
        parent = PlaceholderManager.class
)
public class TeamPointsPositionPlaceholder implements PlaceholderModule {
    @Override
    public @Nullable String onRequest(EventMain eventMain, @Nullable OfflinePlayer offlinePlayer, List<String> args) {
        if (args.isEmpty()) {
            return null;
        }
        EventTeam team = this.getFromPosition(Integer.parseInt(args.getFirst()) - 1);
        if (team == null) {
            return "Unknown position";
        }
        return Util.formatInt(team.getTeamPoints());
    }
}
