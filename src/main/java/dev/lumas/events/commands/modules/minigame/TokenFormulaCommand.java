package dev.lumas.events.commands.modules.minigame;

import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.events.EventMain;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import org.bukkit.command.CommandSender;

import java.util.List;

// TODO
//@AutoRegister(RegisterType.SUBCOMMAND)
@CommandMeta(
        name = "formula",
        permission = "lumaevents.default",
        description = "Dump token outputs for the previous minigame.",
        parent = CommandManager.class,
        usage = "/<command> formula"
)
public class TokenFormulaCommand implements CommandModule {
    @Override
    public boolean execute(EventMain eventMain, CommandSender sender, String s, String[] args) {


        return false;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return List.of();
    }
}
