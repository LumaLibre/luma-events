package dev.jsinco.luma.lumaevents.commands.modules.team;

import dev.jsinco.luma.lumacore.manager.commands.CommandInfo;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.commands.CommandManager;
import dev.jsinco.luma.lumaevents.commands.CommandModule;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.enums.EventTeamType;
import dev.jsinco.luma.lumaevents.utility.Util;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@AutoRegister(RegisterType.SUBCOMMAND)
@CommandInfo(
        name = "setteam",
        permission = "lumaevents.admin",
        description = "Set a player's team",
        parent = CommandManager.class,
        usage = "/<command> setteam <player!> <team!>"
)
public class SetTeamCommand implements CommandModule {
    @Override
    public boolean execute(EventMain eventMain, CommandSender commandSender, String s, String[] strings) {
        if (strings.length != 2) {
            return false;
        }
        Player player = Bukkit.getPlayer(strings[0]);
        EventTeamType teamType = Util.getEnumFromString(EventTeamType.class, strings[1]);

        if (player == null) {
            Util.sendMsg(commandSender, "Player not found");
            return false;
        }

        EventPlayer eventPlayer = EventPlayerManager.getByUUID(player.getUniqueId());
        eventPlayer.setTeamType(teamType);
        Util.sendMsg(commandSender, "Set " + player.getName() + "'s team to " + teamType.name());
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        if (strings.length == 2) {
            List<String> completions = new ArrayList<>(List.of("none"));
            for (EventTeamType teamType : EventTeamType.values()) {
                completions.add(teamType.name().toLowerCase());
            }
            return completions;
        }
        return null;
    }
}
