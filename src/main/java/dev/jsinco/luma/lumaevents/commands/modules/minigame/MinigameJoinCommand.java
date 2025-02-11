package dev.jsinco.luma.lumaevents.commands.modules.minigame;

import dev.jsinco.luma.lumacore.manager.commands.CommandInfo;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.commands.CommandManager;
import dev.jsinco.luma.lumaevents.commands.CommandModule;
import dev.jsinco.luma.lumaevents.games.MinigameManager;
import dev.jsinco.luma.lumaevents.games.logic.Minigame;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.utility.Util;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@AutoRegister(RegisterType.SUBCOMMAND)
@CommandInfo(
        name = "join",
        permission = "lumaevents.default",
        description = "Join a minigame",
        parent = CommandManager.class,
        usage = "/<command> join",
        playerOnly = true
)
public class MinigameJoinCommand implements CommandModule {
    @Override
    public boolean execute(EventMain eventMain, CommandSender commandSender, String s, String[] strings) {
        Minigame minigame = MinigameManager.getInstance().getCurrent();
        Player player = (Player) commandSender;
        if (!minigame.isActive()) {
            Util.sendMsg(commandSender, "No active minigame");
        } else if (!minigame.isOpen()) {
            Util.sendMsg(commandSender, "The active minigame is locked!");
        }

        EventPlayer eventPlayer = EventPlayerManager.getByUUID(player.getUniqueId());
        if (eventPlayer.getTeamType() == null) {
            Util.sendMsg(commandSender, "You must be on a team to join a minigame!");
            return false;
        }

        minigame.addParticipant(eventPlayer);
        Util.sendMsg(commandSender, "Joined!");
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return List.of();
    }
}
