package dev.jsinco.luma.lumaevents.commands.modules.minigame;

import dev.jsinco.luma.lumacore.manager.commands.CommandInfo;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.commands.CommandManager;
import dev.jsinco.luma.lumaevents.commands.CommandModule;
import dev.jsinco.luma.lumaevents.games.constants.MinigameConstant;
import dev.jsinco.luma.lumaevents.games.MinigameManager;
import dev.jsinco.luma.lumaevents.utility.Util;
import eu.okaeri.configs.OkaeriConfig;
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

        MinigameConstant minigame = MinigameConstant.fromAlias(strings[0]);
        OkaeriConfig definition;


        if (minigame == null) {
            Util.sendMsg(commandSender, "Invalid minigame");
            return false;
        }


        int seconds = 90;
        if (strings.length >= 2) {
            try {
                seconds = Integer.parseInt(strings[1]);
            } catch (NumberFormatException e) {
                Util.sendMsg(commandSender, "Invalid number of seconds");
                return false;
            }
        }

        if (strings.length >= 3) {
            definition = minigame.getDefinitions().get(strings[2]);
            if (definition == null) {
                Util.sendMsg(commandSender, "Invalid minigame definition: " + strings[2]);
                return false;
            }
        } else {
            definition = Util.getRandom(minigame.getDefinitions().values());
        }

        if (MinigameManager.getInstance().tryNewMinigameSafely(minigame, definition, true, seconds)){
            Util.sendMsg(commandSender, "Minigame started");
        } else {
            Util.sendMsg(commandSender, "Failed to start minigame. Is there another minigame active?");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return List.of("paintball2.1", "tnttag", "boatrace2");
    }
}
