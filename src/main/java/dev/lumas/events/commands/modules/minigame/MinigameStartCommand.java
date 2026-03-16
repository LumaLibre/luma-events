package dev.lumas.events.commands.modules.minigame;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import dev.lumas.events.games.MinigameManager;
import dev.lumas.events.games.constants.MinigameConstant;
import dev.lumas.events.utility.Util;
import eu.okaeri.configs.OkaeriConfig;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

@Register(Autowire.SUBCOMMAND)
@CommandMeta(
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
        return Arrays.stream(MinigameConstant.values())
                .flatMap(c -> Arrays.stream(c.getAliases()))
                .toList();
    }
}
