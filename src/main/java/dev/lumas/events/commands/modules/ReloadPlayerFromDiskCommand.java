package dev.lumas.events.commands.modules;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import dev.lumas.events.manager.EventPlayerManager;
import dev.lumas.events.utility.Util;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Register(Autowire.SUBCOMMAND)
@CommandMeta(
        name = "disk",
        permission = "lumaevents.admin",
        description = "Reload a player's data from disk",
        parent = CommandManager.class,
        usage = "/<command> disk <player>"
)
public class ReloadPlayerFromDiskCommand implements CommandModule {
    @Override
    public boolean execute(@NonNull EventMain plugin, @NonNull CommandSender sender, @NonNull String label, @NonNull String @NonNull [] args) {
        if (args.length != 1) {
            return false;
        }

        Player target = Bukkit.getPlayerExact(args[0]);

        if (target == null) {
            Util.sendMsg(sender, "Player not found");
            return false;
        }

        EventPlayerManager.reloadFromDisk(target.getUniqueId());
        Util.sendMsg(sender, "Reloaded player data for " + target.getName() + " from disk");
        return true;
    }

    @Override
    public @Nullable List<String> tabComplete(@NonNull EventMain plugin, @NonNull CommandSender sender, @NonNull String @NonNull [] args) {
        return null;
    }
}
