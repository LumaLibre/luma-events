package dev.lumas.events.commands.modules;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import dev.lumas.events.utility.Util;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

@NullMarked
@Register(Autowire.SUBCOMMAND)
@CommandMeta(
        name = "reload",
        permission = "lumaevents.admin",
        description = "Reload config, minigame definitions and minigame state",
        parent = CommandManager.class,
        usage = "/<command> reload"
)
public class ReloadCommand implements CommandModule {
    @Override
    public boolean execute(EventMain eventMain, CommandSender commandSender, String s, String[] strings) {
        try {
            EventMain.getConfigManager().reload();
        } catch (IOException e) {
            EventMain.getInstance().getLogger().log(Level.SEVERE, "Failed to reload config", e);
            Util.sendMsg(commandSender, "<red>Failed to reload config: " + e.getMessage());
            return true;
        }
        Util.sendMsg(commandSender, "Reloaded config");
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return List.of();
    }
}
