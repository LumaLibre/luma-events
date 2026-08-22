package dev.lumas.events.commands.modules;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import dev.lumas.events.utility.JoinTrace;
import dev.lumas.events.utility.Util;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
@Register(Autowire.SUBCOMMAND)
@CommandMeta(
        name = "trace",
        permission = "lumaevents.admin",
        description = "Toggle verbose join/teleport tracing",
        parent = CommandManager.class,
        usage = "/<command> trace [on|off]"
)
public class TraceCommand implements CommandModule {

    @Override
    public boolean execute(EventMain eventMain, CommandSender commandSender, String s, String[] strings) {
        boolean enable;
        if (strings.length == 0) {
            enable = !JoinTrace.isVerbose();
        } else if (strings[0].equalsIgnoreCase("on")) {
            enable = true;
        } else if (strings[0].equalsIgnoreCase("off")) {
            enable = false;
        } else {
            Util.sendMsg(commandSender, "Usage: /event trace [on|off]");
            return false;
        }

        JoinTrace.setVerbose(enable);
        if (enable) JoinTrace.resetReportedSites();
        Util.sendMsg(commandSender, "Join/teleport tracing is now " + (enable ? "<green>on" : "<red>off"));
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return List.of("on", "off");
    }
}
