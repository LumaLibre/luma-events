package dev.jsinco.luma.lumaevents.commands.modules.team;

import dev.jsinco.luma.lumacore.manager.commands.CommandInfo;
import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumaevents.EventMain;
import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.commands.CommandManager;
import dev.jsinco.luma.lumaevents.commands.CommandModule;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.enums.EventTeamType;
import dev.jsinco.luma.lumaevents.utility.Util;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@AutoRegister(RegisterType.SUBCOMMAND)
@CommandInfo(
        parent = CommandManager.class,
        name = "jointeam",
        description = "Join a team",
        usage = "/<command> jointeam",
        permission = "lumaevents.default",
        playerOnly = true
)
public class JoinTeamCommand implements CommandModule {
    @Override
    public boolean execute(EventMain eventMain, CommandSender commandSender, String s, String[] strings) {
        Player player = (Player) commandSender;
        EventPlayer eplayer = EventPlayerManager.getByUUID(player.getUniqueId());
        if (eplayer.getTeamType() != null) {
            eplayer.sendMessage("You are already on the " + eplayer.getTeamType().getFormatted() + " team.");
            return true;
        }

        EventTeamType lastChosenTeam = EventMain.getOkaeriConfig().getLastChosenTeam();
        // Cycle through the teams
        EventTeamType nextTeam = getNextTeam(lastChosenTeam);
        eplayer.setTeamType(nextTeam);
        EventMain.getOkaeriConfig().setLastChosenTeam(nextTeam);

        Util.broadcast(player.getName() + " has joined the " + nextTeam.getTeamWithGradient() + " <reset>team!");
        Util.broadcastSound(Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.3f, 1.0f);
        return true;
    }

    @Override
    public List<String> tabComplete(EventMain eventMain, CommandSender commandSender, String[] strings) {
        return List.of();
    }

    private EventTeamType getNextTeam(EventTeamType lastChosenTeam) {
        EventTeamType[] values = EventTeamType.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i] == lastChosenTeam) {
                return values[(i + 1) % values.length];
            }
        }
        return values[0];
    }
}
