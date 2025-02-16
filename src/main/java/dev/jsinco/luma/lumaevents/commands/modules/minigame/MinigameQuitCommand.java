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
        name = "quit",
        permission = "lumaevents.default",
        description = "quit an active minigame",
        parent = CommandManager.class,
        usage = "/<command> quit"
)
public class MinigameQuitCommand implements CommandModule {
    @Override
    public boolean execute(EventMain eventMain, CommandSender sender, String s, String[] strings) {
        Player player = (Player) sender;

        Minigame current = MinigameManager.getInstance().getCurrent();
        EventPlayer eventPlayer = EventPlayerManager.getByUUID(player.getUniqueId());
        if (!current.removeParticipant(eventPlayer)) {
            Util.sendMsg(player, "No active minigame found.");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return List.of();
    }
}
