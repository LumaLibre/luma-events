package dev.lumas.events.commands.modules.gui;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import dev.lumas.events.manager.EventPlayerManager;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.shop.ShopManagerService;
import dev.lumas.events.utility.Util;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Register(Autowire.SUBCOMMAND)
@CommandMeta(
        name = "shop",
        permission = "lumaevents.admin",
        parent = CommandManager.class,
        usage = "/<command> shop"
)
public class ShopCommand implements CommandModule {

    @Override
    public boolean execute(@NotNull EventMain eventMain, @NotNull CommandSender commandSender, String s, String[] args) {
        if (args.length == 0 && !(commandSender instanceof Player)) {
            Util.sendMsg(commandSender, "You must specify a player");
            return false;
        }

        Player player;

        if (args.length > 0) {
            player = Bukkit.getPlayerExact(args[0]);
        } else {
            player = (Player) commandSender;
        }

        if (player == null) {
            Util.sendMsg(commandSender, "Player not found");
            return false;
        }

        EventPlayer eventPlayer = EventPlayerManager.getByUUIDOrNull(player.getUniqueId());
        if (eventPlayer == null || !eventPlayer.isSuspended()) {
            ShopManagerService.getInstance().openShop(player);
        } else {
            eventPlayer.sendMessage("You cannot access the shop while suspended.");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull EventMain eventMain, @NotNull CommandSender commandSender, String[] strings) {
        return null;
    }
}
