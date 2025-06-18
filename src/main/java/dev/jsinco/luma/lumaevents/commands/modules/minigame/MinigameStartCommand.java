package dev.jsinco.luma.lumaevents.commands.modules.minigame;

import dev.jsinco.luma.lumacore.manager.commands.CommandInfo;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.commands.CommandManager;
import dev.jsinco.luma.lumaevents.commands.CommandModule;
import dev.jsinco.luma.lumaevents.games.logic.Minigame;
import dev.jsinco.luma.lumaevents.games.MinigameManager;
import dev.jsinco.luma.lumaevents.games.logic.Paintball2_1;
import dev.jsinco.luma.lumaevents.games.logic.TNTTag;
import dev.jsinco.luma.lumaevents.utility.Util;
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
            case "paintball2.1" -> Paintball2_1.class;
            case "tnttag" -> TNTTag.class;
            default -> null;
        };

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

        if (MinigameManager.getInstance().tryNewMinigameSafely(minigame, true, seconds)){
            Util.sendMsg(commandSender, "Minigame started");
        } else {
            Util.sendMsg(commandSender, "Failed to start minigame. Is there another minigame active?");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return List.of("paintball2.1", "tnttag");
    }
}
