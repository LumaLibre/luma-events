package dev.lumas.events.commands.modules.team;


import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import dev.lumas.events.manager.EventPlayerManager;
import dev.lumas.events.manager.EventTeamManager;
import dev.lumas.events.obj.EventPlayer;
import dev.lumas.events.obj.team.EventTeam;
import dev.lumas.events.utility.Util;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;

@NullMarked
@Register(Autowire.SUBCOMMAND)
@CommandMeta(
        name = "setteam",
        permission = "lumaevents.admin",
        description = "Set a player's team",
        parent = CommandManager.class,
        usage = "/<command> setteam <player> <team>"
)
public class SetTeamCommand implements CommandModule {
    @Override
    public boolean execute(EventMain eventMain, CommandSender commandSender, String s, String[] strings) {
        if (strings.length != 2) {
            return false;
        }
        Player player = Bukkit.getPlayer(strings[0]);
        EventTeamManager.Provider teamType = Util.getEnumFromString(EventTeamManager.Provider.class, strings[1]);

        if (player == null) {
            Util.sendMsg(commandSender, "Player not found");
            return false;
        }

        EventPlayer eventPlayer = EventPlayerManager.getByUUID(player.getUniqueId());

        EventTeam existing = EventTeamManager.getByMember(eventPlayer);
        if (existing != null) {
            existing.removeMember(eventPlayer);
            existing.sendTeamMessage(player.getName() + " has left the team.");
        }

        if (teamType == null) {
            Util.sendMsg(commandSender, "Removed " + player.getName() + "'s team");
            return true;
        }

        EventTeam newTeam = EventTeamManager.getByProvider(teamType);

        newTeam.addMember(eventPlayer);
        newTeam.sendTeamMessage(player.getName() + " has joined the team!");
        Util.sendMsg(commandSender, "Set " + player.getName() + "'s team to " + teamType.name());
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        if (strings.length == 2) {
            List<String> completions = new ArrayList<>(List.of("none"));
            for (EventTeamManager.Provider provider : EventTeamManager.Provider.values()) {
                completions.add(provider.toString());
            }
            return completions;
        }
        return null;
    }
}