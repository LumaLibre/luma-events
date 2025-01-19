package dev.jsinco.luma.lumaevents.commands.modules;

import dev.jsinco.luma.lumacore.manager.commands.CommandInfo;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.commands.CommandManager;
import dev.jsinco.luma.lumaevents.commands.CommandModule;
import dev.jsinco.luma.lumaevents.obj.EventTeam;
import dev.jsinco.luma.lumaevents.obj.EventTeamType;
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
        usage = "/<command> team <team>",
        permission = "lumaevents.default"
)
public class TeamCommand implements CommandModule {
    @Override
    public boolean execute(EventMain eventMain, CommandSender commandSender, String s, String[] strings) {
        EventTeam.ofAsync().thenAcceptAsync(eventTeams -> {
            EventTeamType teamTypeToLookup;
            if (strings.length == 0 && commandSender instanceof Player player) {
                teamTypeToLookup = EventPlayerManager.getByUUID(player.getUniqueId()).getTeamType();
            } else {
                teamTypeToLookup = Util.getEnumFromString(EventTeamType.class, strings[0]);
            }

            if (teamTypeToLookup == null) {
                Util.sendMsg(commandSender, "Invalid team type");
                return;
            }

            EventTeam team = eventTeams.stream()
                    .filter(eventTeam -> eventTeam.getType().equals(teamTypeToLookup))
                    .findFirst()
                    .orElse(null);

            commandSender.sendMessage("Team: " + teamTypeToLookup.name());
            commandSender.sendMessage("Points: " + team.getTeamPoints());
            commandSender.sendMessage("Players: " + team.getTeamPlayers().size());
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
