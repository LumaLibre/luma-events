package dev.lumas.events.commands.modules.team;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import dev.lumas.events.manager.EventPlayerManager;
import dev.lumas.events.manager.EventTeamManager;
import dev.lumas.events.model.EventPlayer;
import dev.lumas.events.model.team.EventTeam;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Register(Autowire.SUBCOMMAND)
@CommandMeta(
        parent = CommandManager.class,
        name = "jointeam",
        description = "Join a team",
        usage = "/<command> jointeam",
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

        EventTeamManager.Provider nextTeam = getNextTeam();
        EventTeam actualTeam = EventTeamManager.getByClass(nextTeam.getTeamClass());
        actualTeam.addMember(eventPlayer);
        actualTeam.sendTeamMessage(player.getName() + " has joined the team!");
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return List.of();
    }

    private EventTeamManager.Provider getNextTeam() {
        var providers = List.of(EventTeamManager.Provider.values());
        int min = providers.stream()
                .mapToInt(p -> EventTeamManager.getByClass(p.getTeamClass()).getTotalMembers())
                .min().orElse(0);

        var smallest = providers.stream()
                .filter(p -> EventTeamManager.getByClass(p.getTeamClass()).getTotalMembers() == min)
                .toList();

        return smallest.get(ThreadLocalRandom.current().nextInt(smallest.size()));
    }
}
