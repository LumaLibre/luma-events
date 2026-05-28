package dev.lumas.events.commands.modules;

import de.myzelyam.api.vanish.VanishAPI;
import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import dev.lumas.events.utility.Util;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.util.Collection;
import java.util.List;

@NullMarked
@Register(Autowire.SUBCOMMAND)
@CommandMeta(
        name = "restoreplayervisibility",
        permission = "lumaevents.admin",
        description = "Show all unvanished players to everyone",
        parent = CommandManager.class,
        usage = "/<command> restoreplayervisibility"
)
public class RestorePlayerVisibilityCommand implements CommandModule {

    @Override
    public boolean execute(EventMain eventMain, CommandSender commandSender, String s, String[] strings) {
        boolean superVanishEnabled = Bukkit.getPluginManager().isPluginEnabled("SuperVanish");
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();

        for (Player viewer : online) {
            for (Player target : online) {
                if (viewer.equals(target)) continue;
                if (superVanishEnabled && VanishAPI.isInvisible(target)) continue;
                viewer.showPlayer(eventMain, target);
            }
        }

        int count = (int) online.stream()
                .filter(p -> !superVanishEnabled || !VanishAPI.isInvisible(p))
                .count();

        Util.sendMsg(commandSender, "Restored visibility for " + count + " unvanished player(s).");
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return List.of();
    }
}
