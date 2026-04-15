package dev.lumas.events.commands.modules;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import dev.lumas.events.manager.EventPlayerManager;
import dev.lumas.events.utility.Util;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
@Register(Autowire.SUBCOMMAND)
@CommandMeta(
        name = "save",
        permission = "lumaevents.admin",
        description = "Save data",
        parent = CommandManager.class,
        usage = "/<command> save"
)
public class SaveCommand implements CommandModule {

    @Override
    public boolean execute(EventMain eventMain, CommandSender commandSender, String s, String[] strings) {
        EventPlayerManager.saveAll();
        Util.sendMsg(commandSender, "saved");
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return List.of();
    }
}
