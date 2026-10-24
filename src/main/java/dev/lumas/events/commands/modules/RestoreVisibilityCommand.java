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
import org.jspecify.annotations.Nullable;

import java.util.List;

@NullMarked
@Register(Autowire.SUBCOMMAND)
@CommandMeta(
    name = "restorevisibility",
    permission = "lumaevents.admin",
    parent = CommandManager.class,
    usage = "/<command> restorevisibility"
)
public class RestoreVisibilityCommand implements CommandModule {
    @Override
    public boolean execute(EventMain plugin, CommandSender sender, String label, String[] args) {
        int count = Util.restoreVisibility(plugin);
        sender.sendMessage("Restoring visibility for " + count + " players, vanished players stay hidden.");
        return true;
    }

    @Override
    public @Nullable List<String> tabComplete(EventMain plugin, CommandSender sender, String[] args) {
        return List.of();
    }
}
