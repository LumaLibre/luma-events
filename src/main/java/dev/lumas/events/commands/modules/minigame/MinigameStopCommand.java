package dev.lumas.events.commands.modules.minigame;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import dev.lumas.events.games.MinigameManager;
import dev.lumas.events.games.interfaces.Minigame;
import dev.lumas.events.utility.Util;
import org.bukkit.command.CommandSender;

import java.util.List;

@Register(Autowire.SUBCOMMAND)
@CommandMeta(
        name = "mgstop",
        permission = "lumaevents.admin",
        description = "Stop the active minigame",
        parent = CommandManager.class,
        usage = "/<command> mgstop confirm"
)
public class MinigameStopCommand implements CommandModule {

    @Override
    public boolean execute(EventMain eventMain, CommandSender sender, String s, String[] strings) {
        if (strings.length == 0) {
            return false;
        } else if (!strings[0].equalsIgnoreCase("confirm")) {
            return false;
        }

        Minigame current = MinigameManager.getInstance().getCurrent();
        if (!current.isActive() || current.isOpen()) {
            Util.sendMsg(sender, "Minigame is either not active or still has an open queue.");
            return true;
        }

        current.stop();
        Util.sendMsg(sender, "Minigame stopped.");
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return List.of();
    }
}
