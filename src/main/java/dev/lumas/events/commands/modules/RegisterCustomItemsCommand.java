package dev.lumas.events.commands.modules;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import dev.lumas.events.items.LocalCustomItemManager;
import dev.lumas.events.utility.Util;
import org.bukkit.command.CommandSender;

import java.util.List;

@Register(Autowire.SUBCOMMAND)
@CommandMeta(
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
