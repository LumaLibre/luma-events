package dev.jsinco.luma.lumaevents.commands.modules;

import dev.jsinco.luma.lumacore.manager.commands.CommandInfo;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumacore.utility.Text;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.commands.CommandManager;
import dev.jsinco.luma.lumaevents.commands.CommandModule;
import dev.jsinco.luma.lumaevents.games.Envoys;
import dev.jsinco.luma.lumaevents.games.Minigame;
import dev.jsinco.luma.lumaevents.games.Paintball;
import org.bukkit.command.CommandSender;

import java.util.List;

@AutoRegister(RegisterType.SUBCOMMAND)
@CommandInfo(
        name = "mgstart",
        permission = "lumaevents.admin",
        description = "Start a minigame",
        parent = CommandManager.class,
        usage = "/<command> mgstart <minigame>"
)
public class MinigameStartCommand implements CommandModule {
    @Override
    public boolean execute(EventMain eventMain, CommandSender commandSender, String s, String[] strings) {
        if (strings.length == 0) {
            return false;
        }

        Class<? extends Minigame> minigame =
        switch (strings[0]) {
            case "envoys" -> Envoys.class;
            case "paintball" -> Paintball.class;
            default -> null;
        };

        if (minigame == null) {
            //Text.
            return false;
        }

        return false;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return List.of("envoys", "paintball");
    }
}
