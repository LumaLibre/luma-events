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
import dev.lumas.events.model.team.EventTeamPlayerHandle;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Register(Autowire.SUBCOMMAND)
@CommandMeta(
        name = "teamchat",
        aliases = {"tc"},
        permission = "lumaevents.default",
        parent = CommandManager.class,
        usage = "/<command> teamchat <message?>",
        playerOnly = true
)
public class TeamChatCommand implements CommandModule {
    @Override
    public boolean execute(@NonNull EventMain plugin, @NonNull CommandSender sender, @NonNull String label, @NonNull String @NonNull [] args) {
        Player player = (Player) sender;
        EventPlayer eventPlayer = EventPlayerManager.getByUUID(player.getUniqueId());
        EventTeam eventTeam = EventTeamManager.getByMember(eventPlayer);

        if (eventTeam == null) {
            eventPlayer.sendMessage("You have no team.");
            return true;
        }

        if (args.length == 0) {
            EventTeamPlayerHandle handle = eventTeam.getMember(eventPlayer);
            boolean result = handle.togglePersistentTeamChat();
            eventPlayer.sendMessage("Team chat is now " + (result ? "enabled" : "disabled") + ".");
            return true;
        }

        String message = String.join(" ", args);
        Component component = Component.text(message);
        eventTeam.sendTeamChat(eventPlayer, component);
        return true;
    }

    @Override
    public @Nullable List<String> tabComplete(@NonNull EventMain plugin, @NonNull CommandSender sender, @NonNull String @NonNull [] args) {
        return List.of();
    }
}
