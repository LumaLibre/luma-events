package dev.lumas.events.commands.modules.minigame;

import dev.lumas.lumacore.manager.commands.CommandInfo;
import dev.lumas.lumacore.manager.modules.AutoRegister;
import dev.lumas.lumacore.manager.modules.RegisterType;
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
