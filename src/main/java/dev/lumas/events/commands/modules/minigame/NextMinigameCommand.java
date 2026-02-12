package dev.lumas.events.commands.modules.minigame;

import dev.lumas.lumacore.manager.commands.CommandInfo;
import dev.lumas.lumacore.manager.modules.AutoRegister;
import dev.lumas.lumacore.manager.modules.RegisterType;
import dev.lumas.events.EventMain;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import dev.lumas.events.configurable.Config;
import dev.lumas.events.configurable.MinigameState;
import dev.lumas.events.utility.Util;
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
        MinigameState minigameState = EventMain.getMinigameState();

        long timeSinceLast = System.currentTimeMillis() - minigameState.getLastGameLaunchTime();
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
