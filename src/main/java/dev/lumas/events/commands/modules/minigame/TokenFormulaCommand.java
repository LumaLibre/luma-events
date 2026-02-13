package dev.lumas.events.commands.modules.minigame;

import dev.lumas.events.EventMain;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import dev.lumas.lumacore.manager.commands.CommandInfo;
import dev.lumas.lumacore.manager.modules.AutoRegister;
import dev.lumas.lumacore.manager.modules.RegisterType;
import org.bukkit.command.CommandSender;

import java.util.List;

// TODO
//@AutoRegister(RegisterType.SUBCOMMAND)
@CommandInfo(
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
