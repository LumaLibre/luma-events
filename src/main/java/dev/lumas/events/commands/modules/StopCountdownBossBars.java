package dev.lumas.events.commands.modules;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.games.models.CountdownBossBar;
import dev.lumas.lumaitems.LumaItems;
import dev.lumas.lumaitems.commands.SubCommand;
import org.bukkit.command.CommandSender;

import java.util.List;

@Register(Autowire.SUBCOMMAND)
@CommandMeta(
        name = "stopcountdownbossbars",
        permission = "lumaevents.admin",
        parent = CommandManager.class,
        usage = "/<command> stopcountdownbossbars"
)
public class StopCountdownBossBars implements SubCommand {
    @Override
    public boolean execute(LumaItems lumaItems, CommandSender commandSender, String s, String[] strings) {
        boolean callback = strings.length > 0 && strings[0].equalsIgnoreCase("callback");
        CountdownBossBar.stopAll(callback);
        return false;
    }

    @Override
    public List<String> tabComplete(LumaItems lumaItems, CommandSender commandSender, String[] strings) {
        return List.of("callback");
    }
}
