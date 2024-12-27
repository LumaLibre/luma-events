package dev.jsinco.luma.commands.modules;

import dev.jsinco.luma.EventMain;
import dev.jsinco.luma.commands.CommandManager;
import dev.jsinco.luma.commands.CommandModule;
import dev.jsinco.luma.guis.ChallengesGui;
import dev.jsinco.luma.manager.commands.CommandInfo;
import dev.jsinco.luma.manager.modules.AutoRegister;
import dev.jsinco.luma.manager.modules.RegisterType;
import dev.jsinco.luma.obj.EventPlayer;
import dev.jsinco.luma.obj.EventPlayerManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@AutoRegister(RegisterType.SUBCOMMAND)
@CommandInfo(
        parent = CommandManager.class,
        name = "challenges",
        permission = "lumaevent.default",
        usage = "/<command> challenges",
        playerOnly = true
)
public class OpenChallengesGui implements CommandModule {
    @Override
    public boolean execute(EventMain eventMain, CommandSender commandSender, String s, String[] strings) {
        Player onlinePlayer = (Player) commandSender;
        EventPlayer eventPlayer = EventPlayerManager.getByUUID(onlinePlayer.getUniqueId());
        ChallengesGui challengesGui = new ChallengesGui(eventPlayer);
        challengesGui.open(onlinePlayer);
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return List.of();
    }
}
