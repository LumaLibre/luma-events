package dev.lumas.events.commands.modules;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import dev.lumas.events.shop.ShopManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Register(Autowire.SUBCOMMAND)
@CommandMeta(
        name = "shop",
        permission = "lumaevents.default",
        parent = CommandManager.class,
        usage = "/<command> shop",
        playerOnly = true
)
public class ShopCommand implements CommandModule {

    @Override
    public boolean execute(@NotNull EventMain eventMain, @NotNull CommandSender commandSender, String s, String[] strings) {
        Player player = (Player) commandSender;
        ShopManager.getInstance().openShop(player);
        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull EventMain eventMain, @NotNull CommandSender commandSender, String[] strings) {
        return List.of();
    }
}
