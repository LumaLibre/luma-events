package dev.lumas.events.commands.modules.minigame;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.EventPlayerManager;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import dev.lumas.events.games.MinigameManager;
import dev.lumas.events.games.interfaces.Minigame;
import dev.lumas.events.obj.EventPlayer;
import dev.lumas.events.utility.Util;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@Register(Autowire.SUBCOMMAND)
@CommandMeta(
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
            return true;
        } else if (!minigame.isOpen()) {
            Util.sendMsg(commandSender, "The active minigame is locked!");
            return true;
        }

        EventPlayer eventPlayer = EventPlayerManager.getByUUID(player.getUniqueId());

        if (eventPlayer.isSuspended()) {
            Util.sendMsg(commandSender, "You are suspended!");
            return true;
        }

        if (minigame.addParticipant(eventPlayer)) {
            Util.sendMsg(commandSender, "Joined!");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return List.of();
    }
}
