package dev.lumas.events.commands.modules.team;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.commands.CommandManager;
import dev.lumas.events.commands.CommandModule;
import dev.lumas.events.obj.team.EventTeam;
import dev.lumas.events.utility.Util;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Register(Autowire.SUBCOMMAND)
@CommandMeta(
        name = "teamspy",
        permission = "lumaevents.admin",
        parent = CommandManager.class,
        usage = "/<command> teamspy",
        playerOnly = true
)
public class TeamChatSpyCommand implements CommandModule {
    @Override
    public boolean execute(@NonNull EventMain plugin, @NonNull CommandSender sender, @NonNull String label, @NonNull String @NonNull [] args) {
        Player player = (Player) sender;
        boolean result = EventTeam.toggleTeamChatSpy(player.getUniqueId());
        Util.sendMsg(player, "Team chat spy is now " + (result ? "enabled" : "disabled"));
        return true;
    }

    @Override
    public @Nullable List<String> tabComplete(@NonNull EventMain plugin, @NonNull CommandSender sender, @NonNull String @NonNull [] args) {
        return List.of();
    }
}
