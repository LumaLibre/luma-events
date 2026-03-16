package dev.lumas.events.commands.modules.suspend;

import dev.lumas.events.EventMain;
import dev.lumas.events.EventPlayerManager;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import dev.lumas.events.obj.EventPlayer;
import dev.lumas.events.utility.Util;
import dev.lumas.lumacore.manager.commands.CommandInfo;
import dev.lumas.lumacore.manager.modules.AutoRegister;
import dev.lumas.lumacore.manager.modules.RegisterType;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@AutoRegister(RegisterType.SUBCOMMAND)
@CommandInfo(
        name = "suspend",
        permission = "lumaevents.default",
        description = "Suspend a player/yourself",
        parent = CommandManager.class,
        usage = "/<command> suspend"
)
public class SuspendCommand implements CommandModule {

    @Override
    public boolean execute(@NotNull EventMain eventMain, @NotNull CommandSender commandSender, @NotNull String s, @NotNull String[] strings) {
        Player target = strings.length > 0 ? Bukkit.getPlayerExact(strings[0]) : (Player) commandSender; // TODO: cast exception if console sender
        if (target == null) {
            return false;
        }

        EventPlayer eventPlayer = EventPlayerManager.getByUUID(target.getUniqueId());
        if (eventPlayer.isSuspended()) {
            Util.sendMsg(commandSender, target.getName() + " is already suspended.");
        } else {
            eventPlayer.suspend();
            Util.sendMsg(commandSender, target.getName() + " has been suspended.");
        }

        return true;
    }

    @Nullable
    @Override
    public List<String> tabComplete(@NotNull EventMain eventMain, @NotNull CommandSender commandSender, @NotNull String[] strings) {
        return null;
    }
}
