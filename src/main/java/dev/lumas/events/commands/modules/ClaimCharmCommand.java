package dev.lumas.events.commands.modules;

import dev.lumas.lumacore.manager.commands.CommandInfo;
import dev.lumas.events.EventMain;
import dev.lumas.events.EventPlayerManager;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import dev.lumas.events.obj.EventPlayer;
import dev.lumas.events.utility.Util;
import dev.lumas.lumacore.manager.modules.AutoRegister;
import dev.lumas.lumacore.manager.modules.RegisterType;
import dev.lumas.lumaitems.api.LumaItemsAPI;
import dev.lumas.lumaitems.model.CustomItem;
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

        CustomItem customItem = LumaItemsAPI.getInstance().getCustomItem("valentide-2026-charm");
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
