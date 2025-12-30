package dev.jsinco.luma.lumaevents.commands.modules;

import dev.lumas.lumacore.manager.commands.CommandInfo;
import dev.lumas.lumacore.manager.modules.AutoRegister;
import dev.lumas.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.commands.CommandManager;
import dev.jsinco.luma.lumaevents.commands.CommandModule;
import dev.jsinco.luma.lumaevents.items.LocalCustomItemManager;
import dev.jsinco.luma.lumaevents.utility.Util;
import org.bukkit.command.CommandSender;

import java.util.List;

@AutoRegister(RegisterType.SUBCOMMAND)
@CommandInfo(
        name = "customitemregister",
        aliases = {"register"},
        permission = "lumaevents.admin",
        description = "re-register lumaitems",
        parent = CommandManager.class,
        usage = "/<command> customitemregister"
)
public class RegisterCustomItemsCommand implements CommandModule {
    @Override
    public boolean execute(EventMain eventMain, CommandSender commandSender, String s, String[] strings) {
        LocalCustomItemManager.registerCustomItems();
        Util.sendMsg(commandSender, "Re-registered all custom items.");
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return List.of();
    }
}
