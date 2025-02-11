package dev.jsinco.luma.lumaevents.commands.modules.team;

import dev.jsinco.luma.lumacore.manager.commands.CommandInfo;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.commands.CommandManager;
import dev.jsinco.luma.lumaevents.commands.CommandModule;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@AutoRegister(RegisterType.SUBCOMMAND)
@CommandInfo(
        parent = CommandManager.class,
        name = "disabletc",
        description = "Disable Team Chat",
        usage = "/<command> disabletc",
        permission = "lumaevents.default",
        playerOnly = true
)
public class DisableTeamChatCommand implements CommandModule {

    @Override
    public boolean execute(EventMain eventMain, CommandSender commandSender, String s, String[] strings) {
        Player player = (Player) commandSender;
        EventPlayer eplayer = EventPlayerManager.getByUUID(player.getUniqueId());
        eplayer.setDisabledTeamChat(!eplayer.isDisabledTeamChat());
        eplayer.sendMessage("Team chat is now " + (eplayer.isDisabledTeamChat() ? "disabled" : "enabled"));
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return List.of();
    }
}
