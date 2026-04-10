package dev.lumas.events.commands.modules.team;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import dev.lumas.events.configurable.PersistentStates;
import dev.lumas.events.manager.EventPlayerManager;
import dev.lumas.events.manager.EventTeamManager;
import dev.lumas.events.obj.EventPlayer;
import dev.lumas.events.obj.team.EventTeam;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@Register(Autowire.SUBCOMMAND)
@CommandMeta(
        parent = CommandManager.class,
        name = "team",
        description = "Join a team",
        usage = "/<command> team",
        permission = "lumaevents.default",
        playerOnly = true
)
@NullMarked
public class JoinTeamCommand implements CommandModule {
    @Override
    public boolean execute(EventMain eventMain, CommandSender commandSender, String s, String[] strings) {
        Player player = (Player) commandSender;
        EventPlayer eventPlayer = EventPlayerManager.getByUUID(player.getUniqueId());
        EventTeam currentTeam = EventTeamManager.getByMember(eventPlayer);
        if (currentTeam != null) {
            eventPlayer.sendMessage("You are already on the " + currentTeam.getIdentifier() + " team.");
            return true;
        }

        PersistentStates states = EventMain.getPersistentStates();

        EventTeamManager.Provider lastChosenTeam = states.getLastChosenTeam();
        // Cycle through the teams
        EventTeamManager.Provider nextTeam = getNextTeam(lastChosenTeam);
        EventTeam actualTeam = EventTeamManager.getByClass(nextTeam.getTeamClass());
        actualTeam.addMember(player.getUniqueId());
        states.setLastChosenTeam(nextTeam);
        states.save();

        actualTeam.sendTeamMessage(player.getName() + " has joined the team!");
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return List.of();
    }

    private EventTeamManager.Provider getNextTeam(EventTeamManager.Provider lastChosenTeam) {
        EventTeamManager.Provider[] values = EventTeamManager.Provider.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i] == lastChosenTeam) {
                return values[(i + 1) % values.length];
            }
        }
        return values[0];
    }
}
