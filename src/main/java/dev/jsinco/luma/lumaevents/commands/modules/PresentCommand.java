package dev.jsinco.luma.lumaevents.commands.modules;

import dev.jsinco.luma.lumacore.manager.commands.CommandInfo;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.commands.CommandManager;
import dev.jsinco.luma.lumaevents.commands.CommandModule;
import dev.jsinco.luma.lumaevents.items.CustomItemsManager;
import dev.jsinco.luma.lumaevents.items.PresentItem;
import dev.jsinco.luma.lumaevents.utility.Util;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@AutoRegister(RegisterType.SUBCOMMAND)
@CommandInfo(
        parent = CommandManager.class,
        name = "present",
        permission = "lumaevent.admin",
        usage = "/<command> present <player>"
)
public class PresentCommand implements CommandModule {
    @Override
    public boolean execute(EventMain eventMain, CommandSender commandSender, String s, String[] strings) {
        if (strings.length < 1) {
            return false;
        }

        Player receiver = Bukkit.getPlayer(strings[0]);
        if (receiver == null) {
            Util.sendMsg(commandSender, "&cPlayer not found.");
            return false;
        }

        PresentItem customItem = CustomItemsManager.presentItem;
        if (customItem == null) {
            return false;
        }

        String randomPlayerName = Util.getRandom(Bukkit.getOnlinePlayers()).getName();
        ItemStack item = customItem.getItemFormatted(randomPlayerName, receiver.getName());
        Util.giveItem(receiver, item);
        Util.sendMsg(commandSender, "&aSuccessfully gave present to &e" + receiver.getName() + "&a.");
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return null;
    }
}
