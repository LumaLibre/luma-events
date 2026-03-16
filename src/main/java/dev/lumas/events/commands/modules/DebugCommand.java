package dev.lumas.events.commands.modules;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import dev.lumas.events.utility.Util;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

@Register(Autowire.SUBCOMMAND)
@CommandMeta(
        name = "debug",
        permission = "lumaevents.admin",
        parent = CommandManager.class,
        usage = "/<command> debug"
)
public class DebugCommand implements CommandModule {
    @Override
    public boolean execute(EventMain eventMain, CommandSender commandSender, String s, String[] strings) {
        Player player = (Player) commandSender;
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        Util.sendMsg(commandSender, "Done");
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return List.of("<int>");
    }
}

