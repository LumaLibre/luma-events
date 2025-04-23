package dev.jsinco.luma.lumaevents.commands.modules.minigame;

import dev.jsinco.luma.lumacore.manager.commands.CommandInfo;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.commands.CommandManager;
import dev.jsinco.luma.lumaevents.commands.CommandModule;
import dev.jsinco.luma.lumaevents.games.MinigameManager;
import dev.jsinco.luma.lumaevents.games.logic.Minigame;
import dev.jsinco.luma.lumaevents.utility.Util;
import org.bukkit.command.CommandSender;

import java.util.List;

@AutoRegister(RegisterType.SUBCOMMAND)
@CommandInfo(
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
