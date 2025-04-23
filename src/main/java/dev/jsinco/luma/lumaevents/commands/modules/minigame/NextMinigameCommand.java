package dev.jsinco.luma.lumaevents.commands.modules.minigame;

import dev.jsinco.luma.lumacore.manager.commands.CommandInfo;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.commands.CommandManager;
import dev.jsinco.luma.lumaevents.commands.CommandModule;
import dev.jsinco.luma.lumaevents.configurable.Config;
import dev.jsinco.luma.lumaevents.utility.Util;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.concurrent.TimeUnit;

@AutoRegister(RegisterType.SUBCOMMAND)
@CommandInfo(
        name = "next",
        permission = "lumaevents.default",
        description = "Get the time until the next minigame",
        parent = CommandManager.class,
        usage = "/<command> next"
)
public class NextMinigameCommand implements CommandModule {
    @Override
    public boolean execute(EventMain eventMain, CommandSender commandSender, String s, String[] strings) {
        Config cfg = EventMain.getOkaeriConfig();

        long timeSinceLast = System.currentTimeMillis() - cfg.getLastGameLaunchTime();
        long timeCombined = cfg.getAutomaticMinigameCooldown() - timeSinceLast;
        // print how long until the next minigame
        Util.sendMsg(commandSender, "The next minigame will be in <gold>" + millisToMins(timeCombined) + "</gold>.");
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return List.of();
    }

    private String millisToMins(long millis) {
        return String.format("%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }
}
