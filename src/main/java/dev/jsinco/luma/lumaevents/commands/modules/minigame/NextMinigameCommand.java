package dev.jsinco.luma.lumaevents.commands.modules.minigame;

import dev.jsinco.luma.lumacore.manager.commands.CommandInfo;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.commands.CommandManager;
import dev.jsinco.luma.lumaevents.commands.CommandModule;
import dev.jsinco.luma.lumaevents.configurable.Config;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.concurrent.TimeUnit;

@AutoRegister(RegisterType.SUBCOMMAND)
@CommandInfo(
        name = "next",
        permission = "lumaevents.default",
        description = "Get the time until the next minigame",
        parent = CommandManager.class,
        usage = "/<command> next",
        playerOnly = true
)
public class NextMinigameCommand implements CommandModule {
    @Override
    public boolean execute(EventMain eventMain, CommandSender commandSender, String s, String[] strings) {
        Config config = EventMain.getOkaeriConfig();

        long timeCombined = config.getLastGameLaunchTime() - config.getAutomaticMinigameCooldown();
        // print how long until the next minigame
        commandSender.sendMessage("The next minigame will be in <gold>" + convertMillisToReadable(timeCombined - System.currentTimeMillis()));
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return List.of();
    }

    private String convertMillisToReadable(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        // Format the result in HH:mm:ss
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
