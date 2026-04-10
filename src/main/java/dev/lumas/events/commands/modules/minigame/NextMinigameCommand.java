package dev.lumas.events.commands.modules.minigame;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import dev.lumas.events.configurable.Config;
import dev.lumas.events.configurable.PersistentStates;
import dev.lumas.events.utility.Util;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Register(Autowire.SUBCOMMAND)
@CommandMeta(
        name = "next",
        permission = "lumaevents.default",
        description = "Get the time until the next minigame",
        parent = CommandManager.class,
        usage = "/<command> next"
)
@NullMarked
public class NextMinigameCommand implements CommandModule {
    @Override
    public boolean execute(EventMain eventMain, CommandSender commandSender, String s, String[] strings) {
        Config cfg = EventMain.getOkaeriConfig();
        PersistentStates persistentStates = EventMain.getPersistentStates();

        long timeSinceLast = System.currentTimeMillis() - persistentStates.getLastGameLaunchTime();
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
