package dev.jsinco.luma.lumaevents.archives.modules;

import dev.lumas.lumacore.manager.commands.CommandInfo;
import dev.lumas.lumacore.manager.modules.AutoRegister;
import dev.lumas.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.commands.CommandManager;
import dev.jsinco.luma.lumaevents.commands.CommandModule;
import dev.jsinco.luma.lumaevents.archives.guis.ArchiveReRollGui;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@AutoRegister(RegisterType.SUBCOMMAND)
@CommandInfo(
        parent = CommandManager.class,
        name = "reroll",
        permission = "lumaevent.admin",
        usage = "/<command> reroll <player>",
        playerOnly = false
)
public class OpenArchiveRerollGui implements CommandModule {
    @Override
    public boolean execute(EventMain eventMain, CommandSender commandSender, String s, String[] strings) {
        Player playerFromArgs = Bukkit.getPlayerExact(strings[0]);
        if (playerFromArgs == null) {
            return false;
        }

        ArchiveReRollGui gui = new ArchiveReRollGui();
        gui.open(playerFromArgs);
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return null;
    }
}