package dev.jsinco.luma.lumaevents.commands.modules;

import dev.jsinco.luma.lumacore.manager.commands.CommandInfo;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.commands.CommandManager;
import dev.jsinco.luma.lumaevents.commands.CommandModule;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.utility.Util;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.List;

@AutoRegister(RegisterType.SUBCOMMAND)
@CommandInfo(
        name = "playerpoints",
        permission = "lumaevents.admin",
        description = "Set a player's points",
        parent = CommandManager.class,
        usage = "/<command> playerpoints <player!> <points!>"
)
public class SetPlayerPointsCommand implements CommandModule {
    @Override
    public boolean execute(EventMain eventMain, CommandSender commandSender, String s, String[] strings) {
        if (strings.length < 2) {
            return false;
        }

        OfflinePlayer player = Bukkit.getOfflinePlayerIfCached(strings[0]);
        if (player == null) {
            Util.sendMsg(commandSender, "Player not found");
            return true;
        }
        EventPlayer eventPlayer = EventPlayerManager.getByUUID(player.getUniqueId());

        int points;
        try {
            points = Integer.parseInt(strings[1]);
        } catch (NumberFormatException e) {
            Util.sendMsg(commandSender, "Invalid number of points");
            return false;
        }

        eventPlayer.setPoints(points);
        Util.sendMsg(commandSender, "Set " + player.getName() + "'s points to " + points);
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        if (strings.length == 1) {
            return null;
        } else if (strings.length == 2) {
            return List.of("<points>");
        }
        return List.of();
    }
}
