package dev.jsinco.luma.lumaevents.commands.modules.team;

import dev.jsinco.luma.lumacore.manager.commands.CommandInfo;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.commands.CommandManager;
import dev.jsinco.luma.lumaevents.commands.CommandModule;
import dev.jsinco.luma.lumaevents.obj.EventTeam;
import dev.jsinco.luma.lumaevents.enums.EventTeamType;
import dev.jsinco.luma.lumaevents.utility.Util;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

@AutoRegister(RegisterType.SUBCOMMAND)
@CommandInfo(
        parent = CommandManager.class,
        name = "team",
        description = "Get information about a team",
        usage = "/<command> team <team?>",
        permission = "lumaevents.default"
)
public class TeamCommand implements CommandModule {
    @Override
    public boolean execute(EventMain eventMain, CommandSender sender, String s, String[] strings) {
        EventTeamType type;
        if (strings.length == 0 && sender instanceof Player player) {
            type = EventPlayerManager.getByUUID(player.getUniqueId()).getTeamType();
        } else {
            type = Util.getEnumFromString(EventTeamType.class, strings[0]);
        }

        if (type == null) {
            Util.sendMsg(sender, "Invalid team type");
            return false;
        }

        EventTeam.ofAsync().thenAcceptAsync(eventTeams -> {

            EventTeam team = eventTeams.stream()
                    .filter(eventTeam -> eventTeam.getType().equals(type))
                    .findFirst()
                    .orElse(null);

            if (team == null) {
                Util.sendMsg(sender, "Team not found");
                return;
            }

            team.msg(sender, "<#eee1d5><st>                     <reset><#eee1d5>⋆⁺₊⋆ ★ ⋆⁺₊⋆<st>                     ");
            team.msg(sender, "Team info for<gray>: " +
                    type.getGradient() +
                    type.getFormatted());
            team.msg(sender, "Total accumulated points<gray>: " + team.getTeamPoints());
            team.msg(sender, "Total members<gray>: " + team.getTeamPlayers().size());
            List<String> onlineMembers = team.getTeamPlayerNames(true);
            int total = team.getTeamPlayers().size();
            int online = onlineMembers.size();
            team.msg(sender, "Online Members <gold>(" + online + "/" + total + ")</gold><gray>:</gray> " +
                    Util.formatList(onlineMembers, type.getColor(), "<white>"));
            team.msg(sender, "<#eee1d5><st>                     <reset><#eee1d5>⋆⁺₊⋆ ★ ⋆⁺₊⋆<st>                     ");
        });
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        if (strings.length == 1) {
            return Arrays.stream(EventTeamType.values()).map(it -> it.name().toLowerCase()).toList();
        }
        return null;
    }
}
