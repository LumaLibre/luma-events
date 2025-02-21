package dev.jsinco.luma.lumaevents.commands.modules;

import dev.jsinco.luma.lumacore.manager.commands.CommandInfo;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.commands.CommandManager;
import dev.jsinco.luma.lumaevents.commands.CommandModule;
import dev.jsinco.luma.lumaevents.enums.EventTeamType;
import dev.jsinco.luma.lumaevents.obj.EventTeam;
import dev.jsinco.luma.lumaevents.utility.Util;
import org.bukkit.command.CommandSender;

import java.util.List;

@AutoRegister(RegisterType.SUBCOMMAND)
@CommandInfo(
        name = "teampoints",
        permission = "lumaevents.admin",
        description = "Set a team's points",
        parent = CommandManager.class,
        usage = "/<command> playerpoints <team!> <points!>"
)
public class SetTeamPointsCommand implements CommandModule {
    @Override
    public boolean execute(EventMain eventMain, CommandSender commandSender, String s, String[] strings) {
        if (strings.length < 2) {
            return false;
        }

        EventTeamType type = Util.getEnumFromString(EventTeamType.class, strings[0]);
        if (type == null) {
            Util.sendMsg(commandSender, "Invalid team type");
            return false;
        }

        EventTeam.ofTypeAsync(type).thenAcceptAsync(team -> {
            int points = Integer.parseInt(strings[1]);
            // Evenly distribute the points to the team members
            team.getTeamPlayers().forEach(player -> {
                player.setPoints(points / team.getTeamPlayers().size());
            });
            Util.sendMsg(commandSender, "Set " + type.getTeamWithGradient() + "<reset>'s points to " + points);
        });
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return List.of("rosethorn", "sweethearts", "heartbreakers");
    }
}
