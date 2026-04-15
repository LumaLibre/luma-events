package dev.lumas.events.commands.modules.minigame;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import dev.lumas.events.games.MinigameManager;
import dev.lumas.events.games.interfaces.Minigame;
import dev.lumas.events.manager.EventPlayerManager;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.utility.Util;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@Register(Autowire.SUBCOMMAND)
@CommandMeta(
        name = "quit",
        permission = "lumaevents.default",
        description = "quit an active minigame",
        parent = CommandManager.class,
        usage = "/<command> quit"
)
@NullMarked
public class MinigameQuitCommand implements CommandModule {
    @Override
    public boolean execute(EventMain eventMain, CommandSender sender, String s, String[] strings) {
        Player player = (Player) sender;

        Minigame current = MinigameManager.getInstance().getCurrent();
        EventPlayer eventPlayer = EventPlayerManager.getByUUID(player.getUniqueId());

        if (eventPlayer.isSuspended()) {
            Util.sendMsg(sender, "You are suspended!");
            return true;
        }

        if (!current.removeParticipant(eventPlayer, true)) {
            Util.sendMsg(player, "No active minigame found.");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return List.of();
    }
}
