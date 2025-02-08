package dev.jsinco.luma.lumaevents.commands.modules;

import dev.jsinco.luma.lumacore.manager.commands.CommandInfo;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumacore.utility.Text;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.commands.CommandManager;
import dev.jsinco.luma.lumaevents.commands.CommandModule;
import dev.jsinco.luma.lumaevents.games.logic.Envoys;
import dev.jsinco.luma.lumaevents.games.logic.Minigame;
import dev.jsinco.luma.lumaevents.games.MinigameManager;
import dev.jsinco.luma.lumaevents.games.logic.Paintball;
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
            Text.msg(commandSender, "Invalid minigame");
            return false;
        }

        if (MinigameManager.getInstance().tryNewMinigameSafely(minigame, true)){
            Text.msg(commandSender, "Minigame started");
        } else {
            Text.msg(commandSender, "Failed to start minigame. Is there another minigame active?");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return List.of("envoys", "paintball");
    }
}
