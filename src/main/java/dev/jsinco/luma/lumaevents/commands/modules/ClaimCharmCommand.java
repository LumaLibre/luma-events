package dev.jsinco.luma.lumaevents.commands.modules;

import dev.jsinco.luma.lumacore.manager.commands.CommandInfo;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.commands.CommandManager;
import dev.jsinco.luma.lumaevents.commands.CommandModule;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.utility.Util;
import dev.jsinco.luma.lumaitems.api.LumaItemsAPI;
import dev.jsinco.luma.lumaitems.manager.CustomItem;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@AutoRegister(RegisterType.SUBCOMMAND)
@CommandInfo(
        name = "claim",
        permission = "lumaevents.default",
        parent = CommandManager.class,
        usage = "/<command> claim",
        playerOnly = true
)
public class ClaimCharmCommand implements CommandModule {
    @Override
    public boolean execute(EventMain eventMain, CommandSender commandSender, String s, String[] strings) {
        Player player = (Player) commandSender;
        EventPlayer eventPlayer = EventPlayerManager.getByUUID(player.getUniqueId());
        if (eventPlayer.isClaimedCharm()) {
            Util.sendMsg(player, "You have already claimed your event charm!");
            return true;
        }

        if (eventPlayer.getScores().isEmpty()) {
            Util.sendMsg(player, "You need to participate in at least one minigame to claim your event charm.");
            return true;
        }

        CustomItem customItem = LumaItemsAPI.getInstance().getCustomItem("lumalympics-charm");
        if (customItem == null) {
            Util.sendMsg(player, "Something went wrong trying to execute this command.");
            return true;
        }
        eventPlayer.setClaimedCharm(true);
        Util.giveItem(player, customItem.createItem().getSecond());
        Util.sendMsg(player, "You have claimed your event charm!");
        Bukkit.getAsyncScheduler().runNow(EventMain.getInstance(), task -> {
            EventPlayerManager.save(eventPlayer);
        });
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return List.of();
    }
}
