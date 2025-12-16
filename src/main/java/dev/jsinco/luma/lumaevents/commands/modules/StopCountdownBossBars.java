package dev.jsinco.luma.lumaevents.commands.modules;

import dev.jsinco.luma.lumacore.manager.commands.CommandInfo;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.commands.CommandManager;
import dev.jsinco.luma.lumaevents.games.obj.CountdownBossBar;
import dev.jsinco.luma.lumaitems.LumaItems;
import dev.jsinco.luma.lumaitems.commands.SubCommand;
import org.bukkit.command.CommandSender;

import java.util.List;

@AutoRegister(RegisterType.SUBCOMMAND)
@CommandInfo(
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
