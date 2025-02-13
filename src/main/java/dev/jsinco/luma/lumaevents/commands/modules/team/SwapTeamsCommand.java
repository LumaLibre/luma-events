package dev.jsinco.luma.lumaevents.commands.modules.team;

import dev.jsinco.luma.lumacore.manager.commands.CommandInfo;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.commands.CommandManager;
import dev.jsinco.luma.lumaevents.commands.CommandModule;
import dev.jsinco.luma.lumaevents.customitems.SwapTeamsItem;
import dev.jsinco.luma.lumaevents.enums.EventTeamType;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.utility.Util;
import dev.jsinco.luma.lumaitems.api.LumaItemsAPI;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@AutoRegister(RegisterType.SUBCOMMAND)
@CommandInfo(
        parent = CommandManager.class,
        name = "swapteams",
        description = "Command for the swap teams item",
        usage = "/<command> swapteams <team!>",
        permission = "lumaevents.default",
        playerOnly = true
)
public class SwapTeamsCommand implements CommandModule {
    @Override
    public boolean execute(EventMain eventMain, CommandSender commandSender, String s, String[] strings) {
        Player player = (Player) commandSender;
        if (!SwapTeamsItem.VALID_SWAP_TEAMS.contains(player.getUniqueId())) {
            Util.sendMsg(player, "This command is only available to players who have redeemed the <b><gradient:#954381:#EC60B0:#EE80C6:#954381>Trade Teams</gradient></b> item " +
                    "and are actively switching teams.");
            return true;
        }

        if (strings.length == 0) {
            Util.sendMsg(player, "Please specify a team to swap to. Or cancel by typing <gold>cancel</gold>.");
            return true;
        }

        SwapTeamsItem.VALID_SWAP_TEAMS.remove(player.getUniqueId());

        EventTeamType teamType = Util.getEnumFromString(EventTeamType.class, strings[0].toUpperCase());
        if (teamType == null) {
            Util.sendMsg(player, "Cancelled!");
            Util.giveItem(player, LumaItemsAPI.getInstance()
                    .getCustomItem(SwapTeamsItem.KEY)
                    .createItem()
                    .getSecond());
            return true;
        }

        EventPlayer eventPlayer = EventPlayerManager.getByUUID(player.getUniqueId());
        eventPlayer.setTeamType(teamType);
        EventPlayerManager.save(eventPlayer); // Save now
        Util.broadcast(player.getName() + " has swapped to the " + teamType.getTeamWithGradient() + " <reset>team!");
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return List.of("rosethorn", "sweethearts", "heartbreakers");
    }
}
