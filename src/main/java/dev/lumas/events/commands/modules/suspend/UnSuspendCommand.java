package dev.lumas.events.commands.modules.suspend;

import com.gmail.nossr50.mcmmo.acf.annotation.Subcommand;
import dev.lumas.events.EventMain;
import dev.lumas.events.EventPlayerManager;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import dev.lumas.events.obj.EventPlayer;
import dev.lumas.events.utility.Util;
import dev.lumas.lumacore.manager.commands.CommandInfo;
import dev.lumas.lumacore.manager.modules.AutoRegister;
import dev.lumas.lumacore.manager.modules.RegisterType;
import dev.lumas.lumaitems.LumaItems;
import dev.lumas.lumaitems.commands.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@AutoRegister(RegisterType.SUBCOMMAND)
@CommandInfo(
        name = "unsuspend",
        permission = "lumaevents.default",
        description = "Unsuspend a player/yourself",
        parent = CommandManager.class,
        usage = "/<command> unsuspend"
)
public class UnSuspendCommand implements CommandModule {
    @Override
    public boolean execute(@NotNull EventMain eventMain, @NotNull CommandSender commandSender, @NotNull String s, @NotNull String[] strings) {
        Player target = strings.length > 0 ? Bukkit.getPlayerExact(strings[0]) : (Player) commandSender; // TODO: cast exception if console sender
        if (target == null) {
            return false;
        }

        EventPlayer eventPlayer = EventPlayerManager.getByUUIDOrNull(target.getUniqueId());
        if (eventPlayer == null || !eventPlayer.isSuspended()) {
            Util.sendMsg(commandSender, target.getName() + " is not suspended.");
        } else {
            eventPlayer.unsuspend();
            Util.sendMsg(commandSender, target.getName() + " has been unsuspended.");
        }

        return true;
    }

    @Nullable
    @Override
    public List<String> tabComplete(@NotNull EventMain eventMain, @NotNull CommandSender commandSender, @NotNull String[] strings) {
        return null;
    }
}
